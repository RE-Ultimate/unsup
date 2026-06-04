/*
 * This file is part of unsup.
 * Copyright © 2020-2026 Exa Skye, Ampflower 🌺
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import com.unascribed.sup.Unsup;

import diy.y2k.stator.Stopwatch;
import diy.y2k.stator.function.ExceptableRunnable;
import diy.y2k.unsup.agent.PuppetHandler.AlertOption;
import diy.y2k.unsup.agent.PuppetHandler.AlertOptionType;
import diy.y2k.unsup.agent.auth.Authorizer;
import diy.y2k.unsup.agent.auth.Authorizer.AuthorizerSpec;
import diy.y2k.unsup.agent.http.HTTP;
import diy.y2k.unsup.agent.pieces.QDIni;
import diy.y2k.unsup.agent.pieces.QDIni.QDIniException;
import diy.y2k.unsup.agent.signing.SigProvider;
import diy.y2k.unsup.ann.NotNull;
import diy.y2k.unsup.bootstrap.Bootstrapper;
import diy.y2k.unsup.bootstrap.Util;
import diy.y2k.unsup.data.AlertMessageType;
import diy.y2k.unsup.data.SysPropDefs;
import diy.y2k.unsup.data.SysProps;
import diy.y2k.unsup.util.Resources;

public class Agent {

	public static final long launchTime = System.nanoTime();

	static volatile boolean awaitingExit = false;
	
	public static boolean launched;
	static boolean standalone;
	
	private static List<ExceptableRunnable> cleanup = new ArrayList<>();
	private static @NotNull Config config = new Config();
	
	public static final SigProvider unsupSig = SigProvider.of("<built-in>", "signify RWTSwM40VCzVER3YWt55m4Fvsg0sjZLEICikuU3cD91gR/2lii/jk67B");
	
	// read by the Unsup class when it loads
	// be careful not to load that class until this is all initialized
	public static String sourceVersion;
	public static boolean updated;
	
	static boolean updatedComponents;

	private static JsonObject state;
	private static File stateFile;
	
	/** this mutex must be held while doing sensitive operations that shouldn't be interrupted */
	static final Object dangerMutex = new Object();

	public static void main(String[] args) {
		standalone = true;
		premain(args.length >= 1 ? args[0] : null);
	}
	
	public static void premain(String arg) {
		launched = true;
		try (var sw = Stopwatch.time(s -> Log.info("Finished after "+s+"."))) {
			Log.init();
			Log.info((standalone ? "Starting in standalone mode" : "Launch hijack successful")+". unsup v"+Util.VERSION);
			if (!preinit(arg)) return;
			
			if (config().serverAuthority()) {
				Log.info("Performing pre-update to check for a new config");
				if (UpdateHandler.checkForUpdate(state, config().format(), config().source(),
						true,
						false,
						true,
						res -> {
					res.componentVersions.clear();
					if (res.plan != null) {
						res.plan.skipStateApplication = true;
						if (!res.plan.files.containsKey("unsup.ini")) {
							res.plan = null;
							return;
						}
						deleteNonMatchingKeys(res.plan.expectedState, "unsup.ini");
						deleteNonMatchingKeys(res.plan.files, "unsup.ini");
					}
				})) {
					Log.info("Reinitializing with newly updated config");
					HTTP.destroy(false);
					if (!preinit(arg)) return;
				} else {
					Log.info("No config update. Proceeding as normal.");
				}
			}
			
			if (!config().noGui()) {
				if (SysProps.PUPPET_ASYNC.orBias()) {
					new Thread(PuppetHandler::create, "Puppet starter").start();
				} else {
					PuppetHandler.create();
				}
				addCleanupAction(PuppetHandler::destroy);
			}
			
			if (config().updateMMCPack()) {
				MMCUpdater.scan();
			}
			
			PuppetHandler.sendConfig();
			
			PuppetHandler.tellPuppet(":build");
			PuppetHandler.tellPuppet(":subtitle="+config().initialSubtitle());
			String delay;
			if (config().offerChangeFlavors()) {
				delay = "";
			} else {
				// we don't want to flash a window on the screen if things aren't going slow, so we tell
				// the puppet to wait before actually making the window visible, and assign an id to our
				// order so we can belay it later if we finished before the timer expired
				delay = "1250";
			}
			PuppetHandler.tellPuppet("[openTimeout]"+delay+":visible=true");
			
			// now that we're done with operations we prefer to quickly fail, turn on retries
			// for DNS resolution and connection failures
			HTTP.TENACIOUS_MODE = true;
			
			UpdateHandler.checkForUpdate(state, config().format(), config().source(),
					!config().behavior().promptUpdates(),
					SysProps.DRY_RUN.orBias(),
					false,
					res -> {}
				);

			if (awaitingExit) Agent.blockForever();

			PuppetHandler.tellPuppet(":belay=openTimeout");
			if (updatedComponents) {
				PuppetHandler.openAlert("dialog.component_update.title",
						"dialog.component_update",
						AlertMessageType.INFO, AlertOptionType.OK, AlertOption.OK);
			} else if (PuppetHandler.puppetOut != null) {
				Log.info("Waiting for puppet to complete done animation...");
				PuppetHandler.tellPuppet(":title=title.done");
				PuppetHandler.tellPuppet(":mode=done");
				if (!PuppetHandler.puppet.waitFor(3, TimeUnit.SECONDS)) {
					Log.warn("Tired of waiting, killing the puppet.");
					PuppetHandler.puppet.destroyForcibly();
				}
			}
			
			if (SysProps.DRY_RUN.orBias()) {
				Log.warn("Performed a dry run, per your request. No files in the working directory were changed!");
			}
			if (updatedComponents) {
				Log.info("A component update has been applied - exiting for game restart.");
				throw ExitCode.SUCCESS.exit();
			} else if (standalone) {
				Log.info("Ran in standalone mode, no program will be started.");
			} else {
				Log.info("All done, handing over control.");
				// poke the Unsup class so it loads and finalizes all of its values
				if (Unsup.SOURCE_VERSION != null) Unsup.poke();
				String cmd = System.getProperty("sun.java.command");
				if ("org.multimc.EntryPoint".equals(cmd)) {
					// we actually run before MultiMC's Java-side launcher code, so print a
					// blank line to put "Using onesix launcher." in an island like it's
					// supposed to be
					System.out.println();
				}
			}
		} catch (QDIniException e) {
			Log.error("Config file error: "+e.getMessage()+"! Exiting.");
			throw ExitCode.CONFIG_ERROR.exit();
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		} finally {
			cleanup();
		}
	}

	private static boolean preinit(String arg) {
		var ini = loadConfig();
		if (ini == null) {
			Log.warn("Cannot find a config file, giving up.");
			// by returning but not exiting, we yield control to the program whose launch we hijacked, if any
			return false;
		}
		config = Config.parse(ini, arg);
		
		HTTP.setup(Optional.empty());
		
		stateFile = new File(".unsup-state.json");
		if (stateFile.exists()) {
			try (InputStream in = new FileInputStream(stateFile)) {
				state = JsonParser.object().from(in);
			} catch (Exception e) {
				Log.error("Couldn't load state file! Exiting.", e);
				throw ExitCode.CONSISTENCY_ERROR.exit();
			}
		} else {
			state = new JsonObject();
		}
		return true;
	}
	
	private static QDIni loadConfig() {
		File configFile = new File("unsup.ini");
		if (configFile.exists()) {
			QDIni ini;
			try {
				ini = QDIni.load(configFile);
				checkForbiddenKeys(ini, "strings.dialog.progress.title", "strings.dialog.progress.title.branded");
				Log.debug("Found and loaded unsup.ini. What secrets does it hold?");
			} catch (Exception e) {
				Log.error("Found unsup.ini, but couldn't parse it! Exiting.", e);
				throw ExitCode.CONFIG_ERROR.exit();
			}
			checkRequiredKeys(ini, "version", "source_format", "source");
			int version = ini.getInt("version", -1);
			if (version != 1) {
				Log.error("Config file error: Unknown version "+version+" at "+ini.getBlame("version")+"! Exiting.");
				throw ExitCode.CONFIG_ERROR.exit();
			}
			ini = mergePreset(ini, "__global__", true);
			if (ini.containsKey("preset")) {
				ini = mergePreset(ini, ini.get("preset"), true);
			}
			return ini;
		} else {
			if (SysProps.BOOTSTRAP_URL.isPresent()) {
				var rawUri = SysProps.BOOTSTRAP_URL.get();
				URI uri;
				try {
					uri = new URI(SysProps.BOOTSTRAP_URL.get());
					var censoredUserInfo = uri.getUserInfo();
					if (censoredUserInfo != null) censoredUserInfo = censoredUserInfo.replaceAll("[^:]", "*");
					Log.info("No config found, bootstrapping from "+new URI(uri.getScheme(), censoredUserInfo, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment()));
				} catch (URISyntaxException e) {
					Log.error("Bootstrap error: failed to parse URL! Exiting.", e);
					throw ExitCode.CONFIG_ERROR.exit();
				}
				Optional<SigProvider> key = SysProps.BOOTSTRAP_KEY.map(s -> s.replaceFirst("^([^ %]+)%", "$1 "))
						.map(Util.faulty(s -> SigProvider.parse("-D"+SysPropDefs.BOOTSTRAP_KEY, s),
							e -> {
								Log.error("Bootstrap error: failed to parse key! Exiting.", e);
								throw ExitCode.CONFIG_ERROR.exit();
							}));
				Optional<AuthorizerSpec> auth;
				if (SysProps.BOOTSTRAP_AUTH.isPresent()) {
					if (uri.getRawUserInfo() != null) {
						Log.error("Bootstrap error: Cannot specify an explicit authorizer and in-URL authentication at the same time! Exiting.");
						throw ExitCode.CONFIG_ERROR.exit();
					}
					auth = Authorizer.parseSpec(rawUri, SysProps.BOOTSTRAP_AUTH.orElse("").replaceFirst("^([^ %]+)%", "$1 "));
				} else if (uri.getRawUserInfo() != null) {
					auth = Authorizer.parseSpec(rawUri, "Basic "+uri.getUserInfo());
				} else {
					auth = Optional.empty();
				}
				HTTP.setup(auth);
				int M = 1024*1024;
				try {
					var data = HTTP.loadAndVerify(uri, 16*M,
							new URI(SysProps.BOOTSTRAP_URL.get()+".sig"), key.orElse(null));
					Files.write(configFile.toPath(), data);
					Log.info("Successfully downloaded bootstrap config");
					HTTP.destroy(false);
					return loadConfig();
				} catch (Exception e) {
					Log.error("Bootstrap error: Download failed! Exiting.", e);
					throw ExitCode.BOOTSTRAP_FAILED.exit();
				}
			}
			Log.warn("No config file found? Doing nothing.");
			return null;
		}
	}
	
	private static void checkForbiddenKeys(QDIni ini, String... keys) {
		for (var key : keys) {
			if (ini.containsKey(key)) {
				Log.error("Attempt to override a forbidden key: "+key);
				throw ExitCode.CONFIG_ERROR.exit();
			}
		}
	}

	private static void checkRequiredKeys(QDIni ini, String... requiredKeys) {
		for (String req : requiredKeys) {
			if (!ini.containsKey(req)) {
				Log.error("Config file error: "+req+" is required, but was not defined! Exiting.");
				throw ExitCode.CONFIG_ERROR.exit();
			}
		}
	}
	
	private static QDIni mergePreset(QDIni config, String presetName, boolean mustExist) {
		URL u = Resources.get("presets/"+presetName+".ini");
		if (u == null) {
			if (!mustExist) {
				Log.debug("Optional preset "+presetName+" not found");
				return config;
			}
			Log.error("Config file error: Preset "+presetName+" not found at "+config.getBlame("preset")+"! Exiting.");
			throw ExitCode.CONFIG_ERROR.exit();
		}
		try (InputStream in = u.openStream()) {
			QDIni preset = QDIni.load("<preset "+presetName+">", in);
			config = preset.merge(config);
		} catch (IOException e) {
			Log.error("Failed to load preset "+presetName+"! Exiting.", e);
			throw ExitCode.CONFIG_ERROR.exit();
		}
		return config;
	}
	
	public static void cleanup() {
		config = null;
		for (ExceptableRunnable er : cleanup) {
			try {
				er.run();
			} catch (Throwable t) {}
		}
		HTTP.destroy(true);
		if (Bootstrapper.universe != null) Bootstrapper.universe.forget();
		cleanup = null;
	}
	
	static void saveState() throws IOException {
		saveJson(stateFile, state);
	}
	
	static void saveJson(File file, JsonObject obj) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			JsonWriter.on(fos).object(obj).done();
		}
	}
	
	/* (non-Javadoc)
	 * used in the agent to suspend the update flow at a safe point if we're waiting for a
	 * System.exit due to the user closing the puppet dialog (the puppet handling is multithreaded,
	 * and a mutex is used to ensure we don't kill the updater during a sensitive period that could
	 * corrupt the directory state)
	 */
	public static void blockForever() {
		while (true) {
			try {
				Thread.sleep(Integer.MAX_VALUE);
			} catch (InterruptedException e) {}
		}
	}

	private static <K, V> void deleteNonMatchingKeys(Map<K, V> map, K key) {
		var iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			if (!Objects.equals(iter.next().getKey(), key)) {
				iter.remove();
			}
		}
	}
	
	public static void addCleanupAction(ExceptableRunnable r) {
		cleanup.add(r);
	}

	public static @NotNull Config config() {
		return config;
	}

}
