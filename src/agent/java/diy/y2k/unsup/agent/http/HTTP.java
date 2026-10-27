/*
 * This file is part of unsup.
 * Copyright © 2023-2026 Exa Skye, Skye
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

package diy.y2k.unsup.agent.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.ConnectException;
import java.net.CookieManager;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateEncodingException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.brotli.dec.BrotliInputStream;

import com.github.bsideup.jabel.Desugar;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.moandjiezana.toml.Toml;

import diy.y2k.stator.io.NullOutputStream;
import diy.y2k.stator.io.Slice;
import diy.y2k.stator.io.hash.StandardHashers;
import diy.y2k.unsup.agent.Agent;
import diy.y2k.unsup.agent.ExitCode;
import diy.y2k.unsup.agent.Log;
import diy.y2k.unsup.agent.auth.Authorizer.AuthorizerSpec;
import diy.y2k.unsup.agent.data.HashFunction;
import diy.y2k.unsup.agent.http.HTTP.ResourceRef.Kind;
import diy.y2k.unsup.agent.signing.SigProvider;
import diy.y2k.unsup.agent.util.CorruptionHelper;
import diy.y2k.unsup.agent.util.CorruptionHelper.CorruptionType;
import diy.y2k.unsup.ann.NotNull;
import diy.y2k.unsup.ann.Nullable;
import diy.y2k.unsup.bootstrap.Util;
import diy.y2k.unsup.data.SysProps;
import diy.y2k.unsup.util.Bases;
import diy.y2k.unsup.util.Multimap;
import diy.y2k.unsup.util.SuppressFBWarnings;
import javahttpclient.HttpClient;
import javahttpclient.HttpRequest;
import javahttpclient.HttpResponse;
import javahttpclient.HttpClient.Redirect;
import javahttpclient.HttpResponse.BodyHandlers;

public class HTTP {
	
	public static final int K = 1024;
	public static final int M = K*1024;
	
	/**
	 * When true, retry on initial connection failures rather than giving up.
	 * <p>
	 * Disabled during initial manifest retrieval, so that an offline internet connection can fail
	 * somewhat gracefully.
	 */
	public static volatile boolean TENACIOUS_MODE = false;
	
	public static final long ONE_SECOND_IN_NANOS = TimeUnit.SECONDS.toNanos(1);
	
	private static HttpClient client;
	private static final List<AuthorizerSpec> authorizers = new ArrayList<>();
	
	@Desugar
	private record CertDef(String source, X509Certificate cert) {}
	
	public static void setup(Optional<AuthorizerSpec> additionalAuthorizer) throws AssertionError {
		Set<String> insecureHosts = new HashSet<>();
		List<CertDef> certDefs = new ArrayList<>();
		if (Agent.config().usePlatformCaCerts()) {
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
		if (Agent.config().useBuiltinCaCerts()) {
			for (var cert : CACerts.certs) {
				certDefs.add(new CertDef("built-in", cert));
			}
		} else {
			Log.debug("Skipping addition of built-in CA certificates");
		}
		for (var cert : Agent.config().additionalCaCerts()) {
			certDefs.add(new CertDef("config", cert));
		}
		for (String host : Agent.config().insecureHosts()) {
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
		authorizers.clear();
		additionalAuthorizer.ifPresent(authorizers::add);
		authorizers.addAll(Agent.config().authorizers());
		try {
			var ctx = SSLContext.getInstance("TLS");
			
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
			client = HttpClient.newBuilder()
				.followRedirects(Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(30))
				.cookieHandler(new CookieManager(null, null))
				.proxy(Agent.config().proxySelector())
				.sslContext(ctx)
				.build();
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | KeyManagementException e) {
			throw new AssertionError(e);
		}
	}
	
	public static void destroy(boolean wait) {
		if (client != null) {
			client.shutdown();
			if (wait) {
				try {
					client.awaitTermination(Duration.ofMillis(500));
				} catch (InterruptedException e) {}
			}
			client = null;
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
	
	public static String checkSchemeMismatch(URI src, String url) throws URISyntaxException {
		if (url == null) return null;
		URI dst = new URI(url);
		boolean ok = switch (src.getScheme()) {
			case "file" ->
				switch (dst.getScheme()) {
					// promoting from file to http is ok, as well as using files from files
					case "http", "https", "file" -> true;
					default -> false;
				};
			case "http", "https" ->
				switch (dst.getScheme()) {
					// going between http and https is ok
					case "http", "https" -> true;
					default -> false;
				};
			default -> false;
		};
		if (!ok) {
			Log.warn("Ignoring custom URL with bad scheme "+dst.getScheme());
		}
		return ok ? url : null;
	}
	
	public static byte[] loadAndVerify(URI src, int sizeLimit, URI sigUrl) throws IOException {
		return loadAndVerify(src, sizeLimit, sigUrl, Agent.config().packSig(), Agent.config().altPackSig());
	}
	
	public static byte[] loadAndVerify(URI src, int sizeLimit, URI sigUrl, @Nullable SigProvider... keys) throws IOException {
		byte[] resp = downloadToMemory(src, sizeLimit);
		if (resp == null) {
			throw new IOException(src+" is larger than "+(sizeLimit/K)+"K, refusing to continue downloading");
		}
		if (sigUrl != null) {
			boolean anyVerified = false;
			byte[] sigResp = null;
			int maxLen = 0;
			for (var key : keys) {
				if (key == null) continue;
				maxLen = Math.max(key.maximumSignatureLength(), maxLen);
			}
			for (var key : keys) {
				if (key == null) continue;
				try {
					if (sigResp == null) {
						sigResp = downloadToMemory(sigUrl, maxLen);
					}
					if (key.verify(resp, sigResp)) {
						anyVerified = true;
						Log.debug("Signature for "+src+" (retrieved from "+sigUrl+") is valid with key "+key.origin());
					}
				} catch (Throwable t) {
					throw new IOException("Failed to validate signature for "+src, t);
				}
			}
			if (sigResp != null && !anyVerified) {
				throw new IOException("Failed to validate signature for "+src);
			}
		}
		return resp;
	}

	public static byte[] downloadToMemory(URI url, int sizeLimit) throws IOException {
		return downloadToMemory(url, sizeLimit, false);
	}
	
	public static byte[] downloadToMemory(URI url, int sizeLimit, boolean sendLimitToServer) throws IOException {
		return withRetries(10, () -> {
			try {
				InputStream conn = get(url, false, 0, sendLimitToServer ? sizeLimit-1 : 0).stream();
				var resp = collectLimited(conn, sizeLimit);
				if (resp.isPresent()) return resp.get();
				throw new IOException("Received more than "+sizeLimit+" bytes from server");
			} catch (SocketTimeoutException e) {
				throw new Retry("Connection to "+url.getHost()+" timed out",
						SocketTimeoutException::new);
			} catch (ConnectException e) {
				if (!TENACIOUS_MODE) throw e;
				throw new Retry("Connection to "+url.getHost()+" failed",
						ConnectException::new);
			}
		});
	}
	
	private static String currentFirefoxVersion;
	private static final Set<String> alwaysHostile = new HashSet<>(Arrays.asList(Bases.b64ToString("YmV0YS5jdXJzZWZvcmdlLmNvbXx3d3cuY3Vyc2Vmb3JnZS5jb218Y3Vyc2Vmb3JnZS5jb218bWluZWNyYWZ0LmN1cnNlZm9yZ2UuY29tfG1lZGlhZmlsZXouZm9yZ2VjZG4ubmV0fG1lZGlhZmlsZXMuZm9yZ2VjZG4ubmV0fGZvcmdlY2RuLm5ldHxlZGdlLmZvcmdlY2RuLm5ldHxzdGF0aWMucGxhbmV0bWluZWNyYWZ0LmNvbQ==").split("\\|")));
	private static final Pattern RAW_GITHUB = Pattern.compile("^/([^/]+/[^/]+)/raw/(.*)$");
	private static final Slice LFS_HEADER = Slice.fromString("version https://git-lfs.github.com/spec/v1", StandardCharsets.UTF_8);
	
	@Desugar
	public record ResourceRef(InputStream stream, OptionalLong size, Kind kind) implements Closeable {
		
		public enum Kind {
			UNCOMPRESSED_WITH_RANGE,
			UNCOMPRESSED,
			COMPRESSED,
			;
			public boolean supportsRange() {
				return this == UNCOMPRESSED_WITH_RANGE;
			}
			public boolean compressed() {
				return this == COMPRESSED;
			}
			public static Kind compose(boolean supportsRange, boolean compressed) {
				if (compressed) return COMPRESSED;
				if (supportsRange) return UNCOMPRESSED_WITH_RANGE;
				return UNCOMPRESSED;
			}
		}
		
		public ResourceRef(InputStream stream, Kind kind) { this(stream, OptionalLong.empty(), kind); }
		
		public ResourceRef(InputStream stream, long size, Kind kind) { this(stream, OptionalLong.of(size), kind); }
		
		@Override
		public void close() throws IOException { stream.close(); }
		
	}
	
	public static ResourceRef get(URI url) throws IOException {
		return get(url, false);
	}

	public static ResourceRef get(URI url, boolean hostile) throws IOException {
		return get(url, hostile, 0);
	}
	
	public static ResourceRef get(URI url, boolean hostile, long startAt) throws IOException {
		return get(url, hostile, startAt, 0);
	}
	
	public static ResourceRef get(URI url, boolean hostile, long startAt, long endAt) throws IOException {
		if (SysProps.DEBUG_REQUESTS.orBias()) {
			Log.debug((hostile ? "Carefully r" : "R")+"etrieving "+url+(startAt == 0 ? "" : " (starting at byte "+startAt+")"));
		}
		if ("file".equals(url.getScheme())) {
			var f = new File(url);
			var fis = new FileInputStream(f);
			var ch = fis.getChannel();
			ch.position(startAt);
			return new ResourceRef(fis, ch.size()-startAt, Kind.UNCOMPRESSED_WITH_RANGE);
		}
		if (Agent.config().autofix() && "github.com".equals(url.getHost())) {
			var m = RAW_GITHUB.matcher(url.getPath());
			if (m.matches()) {
				try {
					var corrected = new URI(url.getScheme(), url.getUserInfo(), "raw.githubusercontent.com", url.getPort(), "/"+m.group(1)+"/"+m.group(2), url.getQuery(), url.getFragment());
					Log.debug("Correcting bad 502-prone github.com/*/*/raw/ URL to raw.githubusercontent.com ("+url+" -> "+corrected+")");
					try {
						var header = new Slice(downloadToMemory(corrected, LFS_HEADER.size(), true));
						if (header.equals(LFS_HEADER)) {
							Log.debug("Detected LFS header, correcting again to media.githubusercontent.com (-> "+corrected+")");
							corrected = new URI(url.getScheme(), url.getUserInfo(), "media.githubusercontent.com", url.getPort(), "/media/"+m.group(1)+"/"+m.group(2), url.getQuery(), url.getFragment());
						}
					} catch (IOException e) {
						Log.debug("Exception while checking for LFS header, continuing without LFS check", e);
					}
					return get(corrected, hostile, startAt);
				} catch (URISyntaxException e) {
				}
			}
		}
		if (!hostile && alwaysHostile.contains(url.getHost())) {
			hostile = true;
		}
		if (hostile && currentFirefoxVersion == null) {
			try {
				JsonObject data = loadJson(new URI("https://product-details.mozilla.org/1.0/firefox_versions.json"), 4*K, null);
				currentFirefoxVersion = data.getString("LATEST_FIREFOX_VERSION");
			} catch (Throwable t) {
				currentFirefoxVersion = "158.0";
			}
			int firstDot = currentFirefoxVersion.indexOf('.');
			if (firstDot != -1) {
				int nextDot = currentFirefoxVersion.indexOf('.', firstDot+1);
				if (nextDot != -1) {
					// the UA only has the MAJOR.MINOR, no PATCH
					currentFirefoxVersion = currentFirefoxVersion.substring(0, nextDot);
				}
			}
		}
		final boolean fhostile = hostile;
		return withRetries(10, () -> {
			try {
				HttpRequest.Builder reqbldr = HttpRequest.newBuilder(url)
						.header("User-Agent",
								fhostile ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:"+currentFirefoxVersion+") Gecko/20100101 Firefox/"+currentFirefoxVersion
								         : "unsup/"+Util.VERSION+" (+https://unsup.y2k.diy)"
							);
				if (endAt > 0) {
					reqbldr.header("Range", "bytes="+startAt+"-"+endAt);
				} else if (endAt < 0) {
					reqbldr.header("Range", "bytes"+endAt);
				} else if (startAt != 0) {
					reqbldr.header("Range", "bytes="+startAt+"-");
				}
				if (fhostile) {
					reqbldr.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
					reqbldr.header("Accept-Language", "en-US,en;q=0.9");
					reqbldr.header("Accept-Encoding", endAt != 0 ? "identity" : "gzip, deflate, br, zstd");
					reqbldr.header("Sec-Fetch-Dest", "document");
					reqbldr.header("Sec-Fetch-Mode", "navigate");
					reqbldr.header("Sec-Fetch-Site", "cross-site");
					reqbldr.header("Sec-Fetch-User", "?1");
					reqbldr.header("Priority", "u=0, i");
					reqbldr.header("TE", "trailers");
				} else if (endAt != 0
						// request no compression for likely large files so we can do range-based download resumption.
						// also, these are already compressed anyway.
						|| url.getPath().endsWith(".jar") || url.getPath().endsWith(".zip")) {
					reqbldr.header("Accept-Encoding", "identity");
				} else {
					reqbldr.header("Accept-Encoding", "gzip, deflate, br");
				}
				for (var en : authorizers) {
					if (url.toString().startsWith(en.urlPrefix())) {
						en.auth().authorize(reqbldr);
					}
				}
				HttpResponse<InputStream> res = client.send(reqbldr.build(), BodyHandlers.ofInputStream());
				if (res.statusCode() != (startAt == 0 && endAt == 0 ? 200 : 206)) {
					if (res.statusCode() == 429) {
						int delay = 0;
						var val = res.headers().firstValue("Retry-After");
						if (val.isPresent()) {
							try {
								delay = Integer.parseInt(val.get());
							} catch (NumberFormatException e) {
								try {
									var zdt = ZonedDateTime.parse(val.get(), DateTimeFormatter.RFC_1123_DATE_TIME);
									long l = Instant.now().until(zdt, ChronoUnit.SECONDS);
									if (l < 0) l = 0;
									if (l >= Integer.MAX_VALUE) l = Integer.MAX_VALUE;
									delay = (int)l;
								} catch (DateTimeParseException e2) {
									Log.debug("Failed to parse Retry-After header: "+val, e2);
								}
							}
						}
						throw new Retry(url.getHost()+" asked us to back off (HTTP 429)",
								new IOException(url.getHost()+" asked us to back off, but we're out of retries"))
							.definedDelay(delay);
					}
					if (res.statusCode() == 404 || res.statusCode() == 410) throw new FileNotFoundException(url.toString());
					String s = collectLimited(decompress(res).orElseGet(res::body), 512)
							.map(b -> new String(b, StandardCharsets.UTF_8))
							.orElse("(response too long)");
					if (res.statusCode()/100 == 500) {
						throw new Retry(url.getHost()+" responded with a server error for "+url+" ("+res.statusCode()+")",
								new IOException("Received non-200 response from server for "+url+": "+res.statusCode()+"\n"+s));
					} else if (res.statusCode()/100 == 300) {
						throw new IOException("Received unhandled 3XX response from server for "+url+": "+res.statusCode()+" "+res.headers().firstValue("Location")+"\n"+s);
					} else {
						throw new IOException("Received non-200 response from server for "+url+": "+res.statusCode()+"\n"+s);
					}
				}
				var len = res.headers().firstValueAsLong("Content-Length");
				boolean supportsRange = "identity".equals(res.headers().firstValue("Content-Encoding").orElse("identity"))
						&& "bytes".equals(res.headers().firstValue("Accept-Ranges").orElse(""));
				var decomp = decompress(res);
				return new ResourceRef(decomp.orElseGet(res::body), len, Kind.compose(supportsRange, decomp.isPresent()));
			} catch (InterruptedIOException e) {
				throw new Retry("Connection to "+url.getHost()+" timed out",
						e);
			} catch (UnknownHostException e) {
				if (!TENACIOUS_MODE) throw e;
				throw new Retry("DNS resolution of "+url.getHost()+" failed",
						e);
			} catch (ConnectException e) {
				if (!TENACIOUS_MODE) throw e;
				throw new Retry("Connection to "+url.getHost()+" failed",
						ConnectException::new);
			} catch (SSLHandshakeException e) {
				var cause = e.getCause();
				if (cause != null) {
					var msg = cause.getMessage();
					if (msg != null && msg.contains(" path building failed ")) {
						throw new Retry(url.getHost()+" has an invalid TLS certificate — incorrect system time or broken antivirus?",
							e);
					}
				}
				throw new IOException("Failed to retrieve "+url, e);
			} catch (SSLException e) {
				var msg = e.getMessage();
				if (msg != null && msg.contains(" unrecognized ")) {
					throw new Retry(url.getHost()+" violated TLS protocol — weird VPN or parental controls?",
						e);
				}
				throw new IOException("Failed to retrieve "+url, e);
			} catch (FileNotFoundException e) {
				throw e;
			} catch (IOException e) {
				var msg = e.getMessage();
				if (msg != null && msg.contains(" preface ")) {
					throw new Retry(url.getHost()+" violated HTTP/2 protocol — weird VPN?",
						e);
				}
				throw new IOException("Failed to retrieve "+url, e);
			} catch (Retry e) {
				throw e;
			} catch (Throwable e) {
				// fallback... OkHttp likes to rethrow off-thread exceptions without wrapping
				// we want to at least add context
				throw new IOException("Failed to retrieve "+url, e);
			}
		});
	}
	
	private static Optional<InputStream> decompress(HttpResponse<InputStream> res) throws IOException {
		var ce = res.headers().firstValue("Content-Encoding").orElse("identity");
		return switch (ce) {
			case "", "identity" -> Optional.empty();
			case "deflate" -> Optional.of(new InflaterInputStream(res.body()));
			case "gzip" -> Optional.of(new GZIPInputStream(res.body()));
			case "br" -> Optional.of(new BrotliInputStream(res.body()));
			case "zstd" -> throw new IllegalArgumentException("We lied about supporting zstd to look like a browser, and the remote host called our bluff!");
			default -> throw new IllegalArgumentException("Unknown Content-Encoding: "+ce);
		};
	}

	public static class Retry extends Exception {
		private int definedDelay = 0;
		
		public Retry(String msg, Function<String, Throwable> ifCantRetry) {
			this(msg, ifCantRetry.apply(msg));
		}
		public Retry(String msg, Throwable ifCantRetry) {
			super(msg, ifCantRetry);
		}
		
		public Retry definedDelay(int definedDelay) {
			this.definedDelay = definedDelay;
			return this;
		}
	}
	
	public interface RetryCallable<T, E extends Throwable> {
		T call() throws E, Retry;
	}
	
	@SuppressWarnings("unchecked")
	public static <T, E extends Throwable> T withRetries(int tries, RetryCallable<T, E> call) throws E {
		int nextRetryDelay = 1;
		while (true) {
			boolean canRetry = tries > 0;
			tries--;
			try {
				return call.call();
			} catch (Retry r) {
				if (canRetry) {
					int delay = r.definedDelay;
					if (delay == 0) {
						delay = nextRetryDelay;
						nextRetryDelay += (nextRetryDelay+2)/3;
					}
					Log.warn(r.getMessage()+". Trying again in "+delay+" second"+(delay == 1 ? "" : "s"+(r.definedDelay == 0 ? "" : " (as requested by server)"))
							+", "+(tries == 0 ? "final retry" : tries+" retr"+(tries == 1 ? "y" : "ies")+" left"));
					try {
						TimeUnit.SECONDS.sleep(delay);
					} catch (InterruptedException ignore) {}
				} else {
					var cause = r.getCause();
					assert cause != null;
					throw (E)cause;
				}
			}
		}
	}
	
	public static String loadString(URI src, int sizeLimit, URI sigUrl) throws IOException {
		return new String(loadAndVerify(src, sizeLimit, sigUrl), StandardCharsets.UTF_8);
	}
	
	public static JsonObject loadJson(URI src, int sizeLimit, URI sigUrl) throws IOException, JsonParserException {
		try {
			return JsonParser.object().from(new ByteArrayInputStream(loadAndVerify(src, sizeLimit, sigUrl)));
		} catch (JsonParserException e) {
			throw new IOException("Failed to parse "+src+" as JSON: "+e.getMessage()+" (at line "+e.getLinePosition()+" column "+e.getCharPosition()+")");
		}
	}
	
	public static Toml loadToml(URI src, int sizeLimit, URI sigUrl) throws IOException {
		try {
			return new Toml().read(new InputStreamReader(new ByteArrayInputStream(loadAndVerify(src, sizeLimit, sigUrl)), StandardCharsets.UTF_8));
		} catch (IllegalStateException e) {
			throw new IOException("Failed to parse "+src+" as TOML: "+e.getMessage());
		}
	}
	
	public static Toml loadToml(URI src, int sizeLimit, HashFunction func, String expectedHash) throws IOException {
		byte[] data = downloadToMemory(src, sizeLimit);
		if (data == null) throw new IOException("Size limit of "+(sizeLimit/K)+"K for "+src+" exceeded");
		String hash = func.getHasher().hash(data).toHexLower();
		if (!hash.equals(expectedHash)) {
			CorruptionType type = CorruptionHelper.checkForCorruption(func, expectedHash, new ByteArrayInputStream(data));
			String culprit = switch (type) {
				case EXPECTED_DOS_GOT_UNIX -> "is corrupted by a DOS-to-Unix line ending conversion. Ensure autocrlf is disabled in Git.";
				case EXPECTED_UNIX_GOT_DOS -> "is corrupted by a Unix-to-DOS line ending conversion. Ensure autocrlf is disabled in Git.";
				case LFS_POINTER -> "appears to be a Git LFS pointer file. Double-check your URLs.";
				case UNKNOWN -> null;
			};
			String msg;
			if (culprit != null) {
				msg = src+" "+culprit+". (Expected "+expectedHash+", but got "+hash+")";
			} else {
				msg = "Expected "+expectedHash+" from "+src+", but got "+hash;
			}
			throw new IOException(msg);
		}
		return new Toml().read(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8));
	}

	public interface Progressor {
		void updateProgress(State state, long bytesDownloaded, OptionalLong contentLength);
		
		public enum State { DOWNLOADING, FAILED, COMPLETE }
	}
	
	/**
	 * @param hash null if no hash function was specified
	 */
	@Desugar
	public record DownloadedFile(@Nullable String hash, File file) {}
	
	public static DownloadedFile downloadToFile(URI url, File dir, long size, Progressor progressCb, HashFunction hashFunc, boolean hostile) throws IOException {
		File file = dir == null ? null : File.createTempFile("download", "", dir);
		if (file != null) Agent.addCleanupAction(file::delete);
		return withRetries(10, () -> {
			try {
				long readTotal = 0;
				OptionalLong actualSize = OptionalLong.empty();
				long lastProgressUpdate = 0;
				var digest = hashFunc == null ? null : hashFunc.getHasher().newSession();
				var ref = get(url, hostile);
				try {
					var in = ref.stream();
					actualSize = ref.kind().compressed() ? OptionalLong.empty() : ref.size();
					if (actualSize.isPresent() && size != -1 && actualSize.getAsLong() != size) {
						throw new IOException("Expected "+size+" bytes, but got "+actualSize.getAsLong());
					}
					boolean canUseRange = ref.kind().supportsRange();
					byte[] buf = new byte[16384];
					try (OutputStream out = file == null ? NullOutputStream.INSTANCE : new FileOutputStream(file)) {
						while (true) {
							int read;
							try {
								read = in.read(buf);
							} catch (IOException e) {
								if (canUseRange) {
									Log.debug("IO error while downloading "+url+"; trying to resume", e);
									if (!SysProps.DEBUG.orBias()) Log.warn("IO error while downloading "+url+"; trying to resume");
									try { ref.close(); } catch (Throwable t) {}
									try {
										ref = get(url, hostile, readTotal);
										in = ref.stream();
									} catch (Throwable t) {
										t.addSuppressed(e);
										throw t;
									}
									continue;
								} else {
									throw e;
								}
							}
							if (read == -1) break;
							readTotal += read;
							if (size != -1 && readTotal > size)
								throw new IOException("Overread; expected "+size+" bytes, but got at least "+readTotal);
							if (actualSize.isPresent() && readTotal > actualSize.getAsLong())
								throw new IOException("Overread; was told to expect "+actualSize.getAsLong()+" bytes, but got at least "+readTotal);
							out.write(buf, 0, read);
							if (digest != null) digest.putBytes(buf, 0, read);
							if (progressCb != null && System.nanoTime()-lastProgressUpdate > ONE_SECOND_IN_NANOS/90) {
								lastProgressUpdate = System.nanoTime();
								progressCb.updateProgress(Progressor.State.DOWNLOADING, readTotal, actualSize);
							}
						}
					}
				} catch (IOException e) {
					if (progressCb != null) progressCb.updateProgress(Progressor.State.FAILED, readTotal, actualSize);
					throw e;
				} finally {
					ref.close();
				}
				if (size != -1 && readTotal != size)
					throw new IOException("Underread; expected "+size+" bytes, but only got "+readTotal);
				if (actualSize.isPresent() && readTotal != actualSize.getAsLong())
					throw new IOException("Underread; was told to expect "+actualSize.getAsLong()+" bytes, but only got "+readTotal);
				if (progressCb != null) progressCb.updateProgress(Progressor.State.COMPLETE, readTotal, actualSize);
				String hash = null;
				if (digest != null) {
					hash = digest.finish().toHexLower();
				}
				return new DownloadedFile(hash, file);
			} catch (InterruptedIOException e) {
				throw new Retry("Connection to "+url.getHost()+" timed out",
						e);
			} catch (Throwable e) {
				// add context
				throw new IOException("Failed to retrieve "+url, e);
			}
		});
	}
	
	public static String hash(HashFunction func, File f) throws IOException {
		var digest = func.getHasher().newSession();
		byte[] buf = new byte[16384];
		try (FileInputStream in = new FileInputStream(f)) {
			while (true) {
				int read = in.read(buf);
				if (read == -1) break;
				digest.putBytes(buf, 0, read);
			}
		}
		return digest.finish().toHexLower();
	}

	/**
	 * Closes the stream when done.
	 */
	@SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
	public static @NotNull Optional<byte[]> collectLimited(InputStream in, int limit) throws IOException {
		try (in) {
			var baos = new ByteArrayOutputStream();
			int totalRead = 0;
			byte[] buf = new byte[limit / 4];
			while (true) {
				int read = in.read(buf);
				if (read == -1) break;
				totalRead += read;
				if (totalRead > limit) {
					return Optional.empty();
				}
				baos.write(buf, 0, read);
			}
			return Optional.of(baos.toByteArray());
		}
	}

}
