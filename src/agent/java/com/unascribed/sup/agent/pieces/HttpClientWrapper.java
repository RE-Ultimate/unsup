package com.unascribed.sup.agent.pieces;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLSession;

import org.brotli.dec.BrotliInputStream;

import com.unascribed.sup.agent.Agent;

public class HttpClientWrapper {
	
	private final HttpClient delegate;
	
	public HttpClientWrapper(HttpClient delegate) {
		this.delegate = delegate;
	}
	
	public CompletableFuture<HttpResponse<InputStream>> sendAsync(HttpRequest req)  {
		return delegate.sendAsync(modifyRequest(req), BodyHandlers.ofInputStream())
				.thenApply(this::wrapResponse);
	}
	
	public HttpResponse<InputStream> send(HttpRequest req) throws IOException, InterruptedException {
		return wrapResponse(delegate.send(modifyRequest(req), BodyHandlers.ofInputStream()));
	}
	
	private HttpRequest modifyRequest(HttpRequest req) {
		var bldr = HttpRequest.newBuilder(req, (k, v) -> true);
		if (!req.headers().firstValue("Accept-Encoding").filter(s -> s.contains("br")).isPresent()) {
			bldr.header("Accept-Encoding", "gzip, deflate, br");
		}
		String url = req.uri().toString();
		for (var en : Agent.config().authorizers()) {
			if (url.startsWith(en.urlPrefix())) {
				en.auth().authorize(req, bldr);
			}
		}
		return bldr.build();
	}
	
	private HttpResponse<InputStream> wrapResponse(HttpResponse<InputStream> res) {
		return new HttpResponse<>() {

			@Override
			public int statusCode() {
				return res.statusCode();
			}

			@Override
			public HttpRequest request() {
				return res.request();
			}

			@Override
			public Optional<HttpResponse<InputStream>> previousResponse() {
				return res.previousResponse();
			}

			@Override
			public HttpHeaders headers() {
				return res.headers();
			}

			@Override
			public InputStream body() {
				if ("br".equals(headers().firstValue("Content-Encoding").get())) {
					try {
						return new BrotliInputStream(res.body());
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
				return res.body();
			}

			@Override
			public Optional<SSLSession> sslSession() {
				return res.sslSession();
			}

			@Override
			public URI uri() {
				return res.uri();
			}

			@Override
			public Version version() {
				return res.version();
			}
			
		};
	}

}
