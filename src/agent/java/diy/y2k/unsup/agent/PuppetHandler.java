/*
 * This file is part of unsup.
 * Copyright © 2023-2026 Exa Skye
 * https://git.sleeping.town/exa/unsup
 *
 * unsup is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * unsup is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with unsup; if not, see <https://www.gnu.org/licenses/>.
 */

package diy.y2k.unsup.agent;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.brotli.dec.BrotliInputStream;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.unascribed.flexver.FlexVerComparator;

import diy.y2k.stator.concurrent.Latch;
import diy.y2k.unsup.PlatDetect;
import diy.y2k.unsup.PlatDetect.ArchType;
import diy.y2k.unsup.PlatDetect.OSType;
import diy.y2k.unsup.agent.http.HTTP;
import diy.y2k.unsup.agent.pieces.pseudolocale.AccentedEnglish;
import diy.y2k.unsup.agent.pieces.pseudolocale.PigLatin;
import diy.y2k.unsup.bootstrap.Bootstrapper;
import diy.y2k.unsup.bootstrap.Util;
import diy.y2k.unsup.data.AlertMessageType;
import diy.y2k.unsup.data.FlavorGroup;
import diy.y2k.unsup.data.SysPropDefs;
import diy.y2k.unsup.data.SysProps;
import diy.y2k.unsup.data.SysProps.PuppetMode;

public class PuppetHandler {
	
	public static volatile Process puppet;
	public static volatile OutputStream puppetOut;
	
	private static final LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();
	
	private static volatile boolean destroyed = false;
	
	private static String lang;
	
	private static String title;
	private static int lastReportedProgress = 0;
	private static long lastReportedProgressTime = 0;
	
	private static final Map<String, String> alertResults = new HashMap<>();
	private static final Map<String, Latch> alertWaiters = new HashMap<>();
	
	private static final int crashId = ThreadLocalRandom.current().nextInt()&Integer.MAX_VALUE;
	
	private static final Pattern IMK_CLIENT = Pattern.compile(" \\+\\[IMKClient subclass\\]: chose IMKClient_(Modern|Legacy)$");
	
	public enum AlertOptionType { OK, OK_CANCEL, YES_NO, YES_NO_CANCEL, YES_NO_TO_ALL_CANCEL }
	public enum AlertOption { CLOSED, OK, YES, NO, CANCEL, YESTOALL, NOTOALL }

	private static final String bundleVersion = "3.4.1";
	
	private static final String[] copyableProps = {
		"javax.accessibility.assistive_technologies",
		"assistive_technologies",
		"sun.java2d.uiScale",
		"unsup.scale"
	};
	
	public static void destroy() {
		destroyed = true;
		if (puppet != null) puppet.destroy();
	}
	
	public static void create() {
		out: {
			URI uri;
			try {
				uri = Bootstrapper.location.toURI();
			} catch (URISyntaxException e) {
				Log.warn("Cannot summon Puppet: Failed to find our own JAR file or directory.");
				puppet = null;
				puppetCrashed();
				break out;
			}
			File ourPath;
			try {
				ourPath = new File(uri);
			} catch (IllegalArgumentException e) {
				Log.warn("Cannot summon Puppet: Failed to find our own JAR file or directory.");
				puppet = null;
				puppetCrashed();
				break out;
			}
			File javaBin = new File(System.getProperty("java.home")+File.separator+"bin");
			String[] executableNames = {
					"javaw.exe",
					"java.exe",
					"javaw",
					"java"
			};
			String java = null;
			for (String exe : executableNames) {
				File f = new File(javaBin, exe);
				if (f.exists()) {
					java = f.getAbsolutePath();
					break;
				}
			}
			if (java == null) {
				Log.warn("Cannot summon Puppet: Failed to find Java. Looked in "+javaBin+", but can't find a known executable.");
				puppet = null;
				puppetCrashed();
			} else {
				if (SysProps.LANGUAGE.isPresent()) {
					lang = SysProps.LANGUAGE.orBias();
					Log.info("Language: "+lang+" (system property)");
				} else {
					// MultiMC and derivatives force -Duser.language=en because anglophones piss their
					// pants if they see a log file that isn't in English. fork a process that doesn't
					// have tainted system properties to look up the actual fucking system language. i
					// can't believe we have to do this shit what the fuck are we even doing man
					try {
						Process langProcess = new ProcessBuilder(java, "-Xint", "-cp", ourPath.getAbsolutePath(), "diy.y2k.unsup.LangPrinter")
								.start();
						langProcess.getOutputStream().close();
						langProcess.getErrorStream().close();
						try (var r = new BufferedReader(new InputStreamReader(langProcess.getInputStream(), StandardCharsets.UTF_8))) {
							var line = r.readLine();
							if (line != null) {
								Log.info("Language: "+line+" (detected)");
								lang = line;
							} else {
								Log.info("Language: en-US (fallback)");
								lang = "en-US";
							}
						}
					} catch (Throwable t) {
						lang = "en-US";
						Log.error("Failed to detect language, using en-US", t);
					}
				}
				Process p;
				try {
					List<String> args = new ArrayList<>();
					SysProps.PUPPET_WRAPPER_COMMAND.ifPresent(args::add);
					args.add(java);
					args.add("-XX:+UseG1GC");
					args.add("-Xms1M");
					args.add("-Xmx128M");
					args.add("-XX:+IgnoreUnrecognizedVMOptions");
					args.add("-XX:+UnlockDiagnosticVMOptions");
					args.add("-Djbr.catch.SIGABRT=true");
					args.add("-D"+SysPropDefs.LANGUAGE+"="+lang);
					args.add("-D"+SysPropDefs.PUPPET_MODE+"="+Agent.config().puppetMode());
					if (FlexVerComparator.compare(System.getProperty("java.version"), "17") >= 0) {
						args.add("--enable-native-access=ALL-UNNAMED");
					}
					for (String prop : copyableProps) {
						String v = System.getProperty(prop);
						if (v != null) {
							args.add("-D"+prop+"="+v);
						}
					}
					if (SysProps.PUPPET_PASS_ALL_LWJGL_ARGS.orBias()) {
						for (String k : System.getProperties().stringPropertyNames()) {
							if (k.startsWith("org.lwjgl.")) {
								args.add("-D"+k+"="+System.getProperty(k));
							}
						}
					}
					List<String> cp = new ArrayList<>();
					cp.add(ourPath.getAbsolutePath());
					if (Agent.config().puppetMode() != PuppetMode.SWING) {
						if (PlatDetect.OS == OSType.UNSUPPORTED || PlatDetect.ARCH == ArchType.UNSUPPORTED
								|| !PlatDetect.OS.supportedArchitectures.contains(PlatDetect.ARCH)) {
							Log.error("Unrecognized platform, falling back to Swing puppet (use -Dunsup.puppetMode=swing to enforce this behavior)");
							args.add("-D"+SysPropDefs.PUPPET_MODE+"=swing");
						} else {
							if (PlatDetect.OS == OSType.MACOS) {
								args.add("-XstartOnFirstThread");
							}
							if (!Util.DEVELOPMENT_ENVIRONMENT) {
								String os = PlatDetect.OS.lwjglName;
								String arch = PlatDetect.ARCH.apiName;
								Log.debug("Retrieving assets for "+os+"-"+arch+"...");
								File cacheDir = PlatDetect.OS.cacheDirGetter.get();
								ExecutorService svc = Executors.newFixedThreadPool(6);
								List<Future<File>> futures = new ArrayList<>();
								try {
									futures.add(svc.submit(() -> {
										return obtainAsset(cacheDir, "bundles/"+bundleVersion+"/"+os+"-"+arch);
									}));
									String localefonts = switch (lang) {
										case "ja", "ko", "zh-CN", "zh-HK", "zh-TW" -> lang;
										default -> null;
									};
									if (localefonts != null) {
										Log.info("Retrieving "+lang+" support fonts...");
										futures.add(svc.submit(() -> {
											return obtainAsset(cacheDir, "localefonts/"+localefonts);
										}));
									}
									Log.info("Retrieving default fonts...");
									futures.add(svc.submit(() -> {
										return obtainAsset(cacheDir, "localefonts/default");
									}));
									svc.shutdown();
									List<String> addnCp = new ArrayList<>();
									for (Future<File> f : futures) {
										addnCp.add(f.get().getAbsolutePath());
									}
									cp.addAll(addnCp);
								} catch (ExecutionException e) {
									Log.error("Failed to load assets for OpenGL puppet, falling back to Swing puppet (use -D"+SysPropDefs.PUPPET_MODE+"=swing to enforce this behavior)", e);
									args.add("-D"+SysPropDefs.PUPPET_MODE+"=swing");
								}
							}
						}
					}
					if (Util.DEVELOPMENT_ENVIRONMENT) {
						for (String s : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
							cp.add(s);
						}
					}
					StringJoiner cpJ = new StringJoiner(File.pathSeparator);
					cp.forEach(cpJ::add);
					args.add("-cp");
					args.add(cpJ.toString());
					File errorFile = determineErrorFilePath();
					args.add("-XX:ErrorFile="+errorFile.getAbsolutePath());
					args.add("-XX:+ErrorLogSecondaryErrorDetails");
					args.add("diy.y2k.unsup.puppet."+(Util.DEVELOPMENT_ENVIRONMENT ? "Puppet" : "Bootstrap"));
					
					StringJoiner printJ = new StringJoiner("' '", "'", "'");
					for (String s : args) {
						printJ.add(s.replace("'", "\\'"));
					}
					
					Log.debug("unsup location detected as "+ourPath);
					Log.debug("Java location detected as "+java);
					Log.debug("Puppet command: "+printJ);
					
					ProcessBuilder bldr = new ProcessBuilder(args);
					bldr.environment().put("_JAVA_AWT_WM_NONREPARENTING", "1");
					bldr.environment().put("NO_AWT_MITSHM", "1");
					bldr.environment().put("__GL_THREADED_OPTIMIZATIONS", "0");
					p = bldr.start();
				} catch (Throwable t) {
					Log.warn("Failed to summon a puppet.", t);
					puppet = null;
					puppetCrashed();
					break out;
				}
				Log.debug("Dark spell successful. Puppet summoned.");
				puppet = p;
			}
		}
		if (puppet == null) {
			Log.warn("Failed to summon a Puppet. Continuing without a GUI.");
		} else {
			if (checkEarlyDestroy()) return;
			Thread puppetErr = new Thread(() -> {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(puppet.getErrorStream(), StandardCharsets.UTF_8))) {
					while (true) {
						String line = br.readLine();
						if (line == null) return;
						Matcher imk = IMK_CLIENT.matcher(line);
						if (imk.find()) {
							// sigh
							Log.log("DEBUG", "puppet", "macOS chose the "+imk.group(1)+" IMKClient implementation");
						} else if (line.startsWith("WARNING: ") && (line.contains("sun.misc.Unsafe") || line.contains("Please consider reporting this"))) {
							// ignore unsafe deprecation warnings
						} else if (line.contains("|")) {
							int idx = line.indexOf('|');
							Log.log(line.substring(0, idx), "puppet", line.substring(idx+1));
						} else {
							Log.puppetStderr(line);
						}
					}
				} catch (IOException e) {}
			}, "unsup puppet error printer");
			puppetErr.setDaemon(true);
			puppetErr.start();
			Log.debug("Waiting for the Puppet to come to life...");
			BufferedReader br = new BufferedReader(new InputStreamReader(puppet.getInputStream(), StandardCharsets.UTF_8));
			Throwable t = null;
			String firstLine = null;
			try {
				firstLine = br.readLine();
			} catch (IOException e) {
				t = e;
			}
			if (checkEarlyDestroy()) return;
			if (firstLine == null) {
				Log.warn("Puppet failed to come alive. Continuing without a GUI.", t);
				puppet.destroy();
				puppet = null;
				puppetCrashed();
			} else if (!"unsup puppet ready".equals(firstLine)) {
				Log.warn("Puppet sent unexpected hello line \""+firstLine+"\". (Expected \"unsup puppet ready\") Continuing without a GUI.");
				puppet.destroy();
				puppet = null;
				puppetCrashed();
			} else {
				Log.debug("Puppet is alive! Continuing.");
				puppetOut = new BufferedOutputStream(puppet.getOutputStream(), 512);
				while (true) {
					var delayedOrder = commandQueue.poll();
					if (delayedOrder == null) break;
					tellPuppet(delayedOrder);
				}
				Thread puppetThread = new Thread(() -> {
					try (BufferedReader br2 = br) {
						String eatenLine = null;
						int crashState = 0;
						while (!destroyed) {
							String line = br.readLine();
							if (line == null) return;
							if (line.equals("closeRequested")) {
								Log.info("User closed puppet window! Exiting...");
								exit();
							} else if (line.startsWith("alert:")) {
								String[] split = line.split(":", 3);
								String name = split[1];
								String opt = split.length > 2 ? split[2] : "";
								synchronized (alertResults) {
									alertResults.put(name, opt);
									Latch latch = alertWaiters.remove(name);
									if (latch != null) {
										latch.release();
									}
								}
							} else if (crashState == 0 && line.startsWith("#")) {
								crashState = 1;
								eatenLine = line;
							} else if (crashState == 1) {
								if (line.startsWith("# A fatal error has been detected by the Java Runtime Environment:")) {
									crashState = 2;
									puppetCrashed();
								} else {
									Log.warn("Unknown line from puppet: "+eatenLine);
									eatenLine = null;
									crashState = 0;
								}
							} else if (crashState != 2) {
								if (crashState == 1) crashState = 0;
								Log.warn("Unknown line from puppet: "+line);
							}
						}
					} catch (IOException | InterruptedException e) {}
					try {
						if (puppet != null) puppet.destroyForcibly();
					} catch (Throwable t2) {}
				}, "unsup puppet out parser");
				puppetThread.setDaemon(true);
				puppetThread.start();
			}
		}
	}
	
	private static boolean checkEarlyDestroy() {
		if (destroyed) {
			Log.debug("Puppet was destroyed before it finished starting.");
			puppet.destroy();
			return true;
		}
		return false;
	}

	private static String determineErrorFileName() {
		return "unsup-puppet-native-crash-"+crashId+".log";
	}
	
	private static File determineErrorFilePath() {
		File logs = new File("logs");
		String n = determineErrorFileName();
		return logs.isDirectory() ? new File(logs, n) : new File(n);
	}

	private static void puppetCrashed() {
		File nativeCrash = determineErrorFilePath();
		if (nativeCrash.exists()) {
			Log.error("The Puppet crashed in native code. Please report this issue, including the full unsup.log and "+determineErrorFileName());
		}
		
		if (SysProps.ABORT_ON_PUPPET_CRASH.orBias()) {
			Log.error("Puppet crashed! Exiting, as requested by -Dunsup.abortOnPuppetCrash=true!");
			try {
				exit();
			} catch (InterruptedException e) {
			}
		}
	}
	
	private static void exit() throws InterruptedException {
		Agent.awaitingExit = true;
		long start = System.nanoTime();
		synchronized (Agent.dangerMutex) {
			long diff = System.nanoTime()-start;
			if (diff > TimeUnit.MILLISECONDS.toNanos(500)) {
				Log.info("Uninterruptible operations finished, exiting!");
			}
			Process p = puppet;
			if (p != null) {
				p.destroy();
				if (!p.waitFor(1, TimeUnit.SECONDS)) {
					p.destroyForcibly();
				}
			}
			throw ExitCode.USER_REQUEST.exit();
		}
	}

	private static File obtainAsset(File cacheDir, String url) throws IOException {
		try {
			return obtainAsset(cacheDir, url, "unsup.y2k.diy");
		} catch (ConnectException | UnknownHostException e) {
			Log.warn("Connection failed, trying again with non-CDN domain");
			return obtainAsset(cacheDir, url, "exa.y2k.diy/unsup");
		}
	}
	
	private static File obtainAsset(File cacheDir, String url, String host) throws IOException {
		final int M = 1024*1024;
		
		String fname = url.replace("/", "-");
		File cacheFile = new File(cacheDir, fname+".jar");
		File cacheFileTmp = new File(cacheDir, fname+".jar.tmp");
		boolean needsDownload = true;
		if (cacheFile.exists() && cacheFile.length() > 32) {
			needsDownload = false;
			Log.debug("Got "+fname+" from cache");
		}
		if (needsDownload) {
			Log.info("Downloading "+fname+" from "+host+"...");
			String dlBase = "https://"+host+"/assets/v1/"+url;
			URI dl = URI.create(dlBase+".jar.br");
			URI sig = URI.create(dlBase+".sig");
			Files.createDirectories(cacheDir.toPath());
			byte[] data = HTTP.loadAndVerify(dl, 64*M, sig, Agent.unsupSig);
			try (FileOutputStream fos = new FileOutputStream(cacheFileTmp);
					InputStream is = new BrotliInputStream(new ByteArrayInputStream(data))) {
				Util.copy(is, fos);
			}
			Files.deleteIfExists(cacheFile.toPath());
			Files.move(cacheFileTmp.toPath(), cacheFile.toPath());
			Log.debug(fname+" downloaded and saved to cache");
		}
		return cacheFile;
	}

	public static void sendConfig() {
		for (var en : Agent.config().colorChoices().entrySet()) {
			tellPuppet(":color="+en.getKey().name()+":"+en.getValue());
		}
		if (lang != null) {
			var langOut = new HashMap<String, String>();
			langOut.putAll(loadLang("en-US"));
			Function<String, String> pseudolocale = null;
			switch (lang) {
				case "en-US" -> {}
				case "en-XA" -> pseudolocale = AccentedEnglish::toEnXA;
				case "en-PIG" -> pseudolocale = PigLatin::toPigLatin;
				default -> langOut.putAll(loadLang(lang));
			}
			if (pseudolocale != null) {
				for (var en : langOut.entrySet()) {
					String[] spl = en.getValue().split(":");
					var sj = new StringJoiner(":");
					for (var v : spl) {
						sj.add(pseudolocale.apply(v));
					}
					en.setValue(sj.toString());
				}
			}
			for (var en : Agent.config().strings().entrySet()) {
				langOut.put(en.getKey(), ":::::"+en.getValue().replace(":", "\r"));
			}
			for (var en : langOut.entrySet()) {
				tellPuppet(":string="+en.getKey()+":"+en.getValue());
			}
		}
		Agent.config().modpackName().ifPresent(name -> {
			tellPuppet(":modpackName="+name);
		});
		Agent.config().brandingIcon().ifPresent(icon -> {
			tellPuppet(":icon="+icon);
		});
		tellPuppet(":flavorDialogGeom="+Agent.config().flavorDialogGeom());
		tellPuppet(":flavorDialogBias="+Agent.config().flavorDialogBias());
	}
	
	private static Map<String, String> loadLang(String lang) {
		try {
			var res = PuppetHandler.class.getResource("/diy/y2k/unsup/lang/"+lang+".json");
			if (res == null) {
				Log.debug(lang+" translations do not exist");
				return Collections.emptyMap();
			}
			var out = new HashMap<String, String>();
			String[] plurals = {"zero", "one", "two", "few", "many", "other"};
			String[] values = new String[plurals.length];
			JsonObject obj = JsonParser.object().from(res);
			for (var en : obj.entrySet()) {
				if (en.getValue() instanceof JsonObject jo) {
					for (int i = 0; i < plurals.length; i++) {
						values[i] = jo.getString(plurals[i], "");
					}
				} else {
					Arrays.fill(values, "");
					values[5] = String.valueOf(en.getValue());
				}
				var sj = new StringJoiner(":");
				for (var v : values) sj.add(v.replace(":", "\r"));
				out.put(en.getKey(), sj.toString());
			}
			Log.debug("Loaded "+obj.size()+" translations for "+lang+" into puppet");
			return out;
		} catch (JsonParserException e) {
			Log.error("Failed to load language "+lang, e);
			return Collections.emptyMap();
		}
	}

	public static void tellPuppet(String order) {
		var out = puppetOut;
		if (!destroyed && out == null) {
			commandQueue.add(order);
			return;
		}
		if (out == null) return;
		synchronized (out) {
			try {
				byte[] utf = order.getBytes(StandardCharsets.UTF_8);
				for (byte b : utf) {
					if (b == 0) {
						// replace NUL with overlong NUL
						out.write(0xC0);
						out.write(0x80);
					} else {
						out.write(b);
					}
				}
				out.write(0);
				out.flush();
			} catch (IOException e) {
				if (!Agent.awaitingExit) {
					Log.warn("IO error while talking to puppet. Killing and continuing without GUI.", e);
					puppetCrashed();
				}
				puppet.destroyForcibly();
				puppet = null;
				puppetOut = null;
				for (Latch l : alertWaiters.values()) {
					l.release();
				}
			}
		}
	}

	public static void updateTitle(String title, boolean determinate) {
		PuppetHandler.title = title;
		tellPuppet(":prog=0");
		tellPuppet(":mode="+(determinate ? "det" : "ind"));
		tellPuppet(":title="+title);
	}

	public static void updateSubtitle(String subtitle) {
		tellPuppet(":subtitle="+subtitle);
	}

	public static void updateSubtitleDownloading(String... files) {
		StringJoiner joiner = new StringJoiner("\u001C");
		for (String s : files) joiner.add(s);
		tellPuppet(":downloading="+joiner);
	}

	public static void updateProgress(int prog) {
		tellPuppet(":prog="+prog);
		if (Math.abs(lastReportedProgress-prog) >= 100 || System.nanoTime()-lastReportedProgressTime > TimeUnit.SECONDS.toNanos(3)) {
			lastReportedProgress = prog;
			lastReportedProgressTime = System.nanoTime();
			Log.info(Agent.config().strings().get(title)+" "+(prog/10)+"%");
		}
	}

	public static AlertOption openAlert(String title, String body, AlertMessageType messageType, AlertOptionType optionType, AlertOption def) {
		if (puppetOut == null) {
			return def;
		} else {
			String name = Long.toString(ThreadLocalRandom.current().nextLong()&Long.MAX_VALUE, 36);
			Latch latch = new Latch();
			alertResults.put(name, def.name().toLowerCase(Locale.ROOT));
			alertWaiters.put(name, latch);
			tellPuppet("["+name+"]:alert="+title+":"+body+":"+messageType.name().toLowerCase(Locale.ROOT)+":"+optionType.name().toLowerCase(Locale.ROOT).replace("_", "")+":"+def.name().toLowerCase(Locale.ROOT).replace("_", ""));
			latch.awaitUninterruptibly();
			var res = alertResults.remove(name);
			assert res != null;
			return AlertOption.valueOf(res.replace("option.", "").replace("_", "").toUpperCase(Locale.ROOT));
		}
	}

	public static String openChoiceAlert(String title, String body, Collection<String> choices, String def) {
		if (puppetOut == null) {
			return def;
		} else {
			String name = Long.toString(ThreadLocalRandom.current().nextLong()&Long.MAX_VALUE, 36);
			Latch latch = new Latch();
			alertResults.put(name, def);
			alertWaiters.put(name, latch);
			StringJoiner joiner = new StringJoiner("\u001C");
			choices.forEach(c -> joiner.add(c.replace(':', '\u001B')));
			tellPuppet("["+name+"]:alert="+title+":"+body+":choice="+joiner+":"+def);
			latch.awaitUninterruptibly();
			return alertResults.remove(name);
		}
	}
	
	public static List<String> openFlavorSelectDialog(String title, String body, List<FlavorGroup> groups) {
		if (puppetOut == null) {
			return new ArrayList<>();
		} else {
			String name = Long.toString(ThreadLocalRandom.current().nextLong()&Long.MAX_VALUE, 36);
			Latch latch = new Latch();
			StringJoiner defaultJoiner = new StringJoiner("\u001C");
			StringJoiner groupJoiner = new StringJoiner("\u001D");
			for (FlavorGroup group : groups) {
				StringJoiner joiner = new StringJoiner("\u001C");
				joiner.add(group.id());
				joiner.add(group.name());
				joiner.add(group.description());
				for (var choice : group.choices()) {
					joiner.add(choice.id()).add(choice.name()).add(choice.description()).add(Boolean.toString(choice.def()));
					if (choice.def()) defaultJoiner.add(choice.id());
				}
				groupJoiner.add(joiner.toString());
			}
			alertResults.put(name, defaultJoiner.toString());
			alertWaiters.put(name, latch);
			tellPuppet("["+name+"]:pickFlavor="+groupJoiner.toString().replace(':', '\u001B'));
			latch.awaitUninterruptibly();
			var res = alertResults.remove(name);
			assert res != null;
			return Arrays.asList(res.split("\u001C"));
		}
	}

}
