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

package com.unascribed.sup.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateEncodingException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.github.bsideup.jabel.Desugar;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import com.unascribed.sup.Unsup;
import com.unascribed.sup.agent.PuppetHandler.AlertOption;
import com.unascribed.sup.agent.PuppetHandler.AlertOptionType;
import com.unascribed.sup.agent.auth.Authorizer;
import com.unascribed.sup.agent.auth.Authorizer.AuthorizerSpec;
import com.unascribed.sup.agent.pieces.QDIni;
import com.unascribed.sup.agent.pieces.QDIni.QDIniException;
import com.unascribed.sup.agent.pieces.pseudolocale.AccentedEnglish;
import com.unascribed.sup.agent.pieces.pseudolocale.PigLatin;
import com.unascribed.sup.agent.signing.SigProvider;
import com.unascribed.sup.agent.util.RequestHelper;
import com.unascribed.sup.ann.NotNull;
import com.unascribed.sup.bootstrap.Bootstrapper;
import com.unascribed.sup.bootstrap.Util;
import com.unascribed.sup.data.AlertMessageType;
import com.unascribed.sup.data.SysPropDefs;
import com.unascribed.sup.data.SysProps;
import com.unascribed.sup.pieces.ExceptableRunnable;
import com.unascribed.sup.util.Multimap;
import com.unascribed.sup.util.Resources;

import diy.y2k.stator.Stopwatch;
import diy.y2k.stator.io.Slice;
import diy.y2k.stator.io.hash.StandardHashers;

import javahttpclient.HttpClient;
import javahttpclient.HttpClient.Redirect;

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
	
	private static HttpClient http;

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
					destroyHttp(false);
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
		String lang = SysProps.LANGUAGE.orBias();
		Log.debug("Language: "+lang);
		var ini = loadConfig(lang);
		if (ini == null) {
			Log.warn("Cannot find a config file, giving up.");
			// by returning but not exiting, we yield control to the program whose launch we hijacked, if any
			return false;
		}
		config = Config.parse(ini, arg, lang);
		
		setupHttp(Optional.empty());
		
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
	
	private static QDIni loadConfig(String lang) {
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
			if (!"en-US".equals(lang)) {
				ini = mergePreset(ini, "lang/"+lang, false);
			}
			ini = mergePreset(ini, "lang/en-US", true);
			Function<String, String> pseudolocale = null;
			if ("en-PIG".equals(lang)) {
				pseudolocale = PigLatin::toPigLatin;
			} else if ("en-XA".equals(lang)) {
				pseudolocale = AccentedEnglish::toEnXA;
			}
			if (pseudolocale != null) {
				StringBuilder sb = new StringBuilder();
				for (String k : ini.keySet()) {
					if (k.startsWith("strings.")) {
						sb.append(k).append("=").append(pseudolocale.apply(ini.get(k))).append("\n");
					}
				}
				ini = ini.merge(QDIni.load("<pseudolocale>", sb.toString()));
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
				System.out.println(auth);
				setupHttp(auth);
				int M = 1024*1024;
				try {
					var data = RequestHelper.loadAndVerify(uri, 16*M,
							new URI(SysProps.BOOTSTRAP_URL.get()+".sig"), key.orElse(null));
					Files.write(configFile.toPath(), data);
					Log.info("Successfully downloaded bootstrap config");
					destroyHttp(false);
					return loadConfig(lang);
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

	@Desugar
	private record CertDef(String source, X509Certificate cert) {}
	
	private static void setupHttp(Optional<AuthorizerSpec> additionalAuthorizer) throws AssertionError {
		Set<String> insecureHosts = new HashSet<>();
		List<CertDef> certDefs = new ArrayList<>();
		if (config().usePlatformCaCerts()) {
			try {
				var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init((KeyStore)null);
				var x509 = findX509(tmf);
				for (var cert : x509.getAcceptedIssuers()) {
					certDefs.add(new CertDef("platform", cert));
				}
			} catch (Throwable t) {
				Log.error("Can't add platform CA certificates due to an unexpected exception", t);
			}
		} else {
			Log.debug("Skipping addition of platform-provided CA certificates");
		}
		if (config().useBuiltinCaCerts()) {
			for (var cert : CACerts.certs) {
				certDefs.add(new CertDef("built-in", cert));
			}
		} else {
			Log.debug("Skipping addition of built-in CA certificates");
		}
		for (var cert : config().additionalCaCerts()) {
			certDefs.add(new CertDef("config", cert));
		}
		for (String host : config().insecureHosts()) {
			Log.debug("Adding insecure host "+host);
			insecureHosts.add(host);
		}
		if (certDefs.isEmpty()) {
			// the first connection attempt will crash with "the trustAnchors parameter must be non-empty" in this state
			Log.error("Config error: No CA certificates were added! Exiting.");
			throw ExitCode.CONFIG_ERROR.exit();
		} else {
			Multimap<Slice, CertDef> defsByEncoded = new Multimap<>();
			for (var def : certDefs) {
				byte[] encoded;
				try {
					encoded = def.cert.getEncoded();
				} catch (CertificateEncodingException e) {
					throw new AssertionError(e);
				}
				defsByEncoded.put(new Slice(encoded), def);
			}
			Set<String> locations = new LinkedHashSet<>();
			for (var en : defsByEncoded.mapEntries()) {
				var def = en.getValue().get(0);
				for (var specificDef : en.getValue()) {
					locations.add(specificDef.source);
				}
				String fp = StandardHashers.sha2_256().hash(en.getKey().toByteArray()).toHex()
								.replaceAll("..(?!$)", "$0:");
				var sj = new StringJoiner(", ");
				for (var loc : locations) {
					sj.add(loc);
				}
				Log.debug("Adding CA cert "+def.cert.getSubjectX500Principal().getName()+" (defined by "+sj+" - SHA256 fingerprint: "+fp+")");
				locations.clear();
			}
		}
		var authorizers = new ArrayList<AuthorizerSpec>();
		if (additionalAuthorizer.isPresent()) {
			authorizers.add(additionalAuthorizer.get());
		}
		authorizers.addAll(config().authorizers());
		
		try {
			var ctx = SSLContext.getInstance("TLS");
			
			var defaultTm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			defaultTm.init((KeyStore)null);
			var defaultX509 = findX509(defaultTm);
			
			var ourTm = TrustManagerFactory.getInstance("PKIX");
			var pkixParams = new PKIXBuilderParameters(certDefs.stream()
					.map(cert -> new TrustAnchor(cert.cert, null))
					.collect(Collectors.toSet()), null);
			pkixParams.addCertPathChecker((PKIXCertPathChecker)CertPathValidator.getInstance("PKIX").getRevocationChecker());
			var trustpath = new CertPathTrustManagerParameters(pkixParams);
			ourTm.init(trustpath);
			var trustManager = findX509(ourTm);
			
			SecureRandom sr;
			try {
				sr = SecureRandom.getInstance("NativePRNGNonBlocking");
			} catch (NoSuchAlgorithmException e) {
				sr = new SecureRandom();
			}
			ctx.init(null, new TrustManager[] { trustManager }, sr);
			http = HttpClient.newBuilder()
				.followRedirects(Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(30))
				.cookieHandler(new CookieManager(null, null))
				.proxy(config().proxySelector())
				.sslContext(ctx)
				.build();
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | KeyManagementException | KeyStoreException e) {
			throw new AssertionError(e);
		}
	}

	private static X509TrustManager findX509(TrustManagerFactory tmf) {
		for (var mgr : tmf.getTrustManagers()) {
			if (mgr instanceof X509TrustManager xtm) {
				return xtm;
			}
		}
		throw new AssertionError("No X509TrustManager");
	}
	
	private static void destroyHttp(boolean wait) {
		if (http != null) {
			http.shutdown();
			if (wait) {
				try {
					http.awaitTermination(Duration.ofMillis(500));
				} catch (InterruptedException e) {}
			}
			http = null;
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
		destroyHttp(true);
		var tarray = new Thread[16];
		Thread.enumerate(tarray);
		for (var t : tarray) {
			if (t != null && ("OkHttp TaskRunner".equals(t.getName()) || "com.unascribed.sup.lib.okhttp3.OkHttp TaskRunner".equals(t.getName()))) {
				// silence meaningless errors due to OkHttp not being designed for its background trash to ever be thrown away
				t.setUncaughtExceptionHandler((x, e) -> {});
			}
		}
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

	public static HttpClient http() {
		return http;
	}

}
