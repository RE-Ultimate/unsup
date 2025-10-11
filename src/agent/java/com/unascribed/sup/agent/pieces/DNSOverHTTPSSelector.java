package com.unascribed.sup.agent.pieces;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.Proxy.Type;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;

import com.github.bsideup.jabel.Desugar;
import com.unascribed.sup.agent.Agent;
import com.unascribed.sup.bootstrap.Util;
import com.unascribed.sup.util.Multimap;

public class DNSOverHTTPSSelector extends ProxySelector {
	
	@Desugar
	private record ExpiringRecord(InetAddress addr, long expires) {
		public boolean valid() {
			return System.currentTimeMillis() < expires;
		}
	}
	
	private final HttpClient client;
	private final URI endpoint;
	
	private final Multimap<String, ExpiringRecord> cache = new Multimap<>();
	
	public DNSOverHTTPSSelector(HttpClient.Builder client, List<InetAddress> hosts, URI endpoint) {
		this.client = client
				.proxy(hosts == null ? HttpClient.Builder.NO_PROXY : new ProxySelector() {
					@Override
					public List<Proxy> select(URI uri) {
						return hosts.stream()
								.map(i -> new Proxy(Type.DIRECT, new InetSocketAddress(i, 443)))
								.collect(Collectors.toList());
					}
					
					@Override
					public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
						
					}
				})
				.build();
		this.endpoint = endpoint;
	}

	@Override
	public List<Proxy> select(URI uri) {
		try {
			var name = Name.fromString(uri.getHost());
			var v4 = query(name, org.xbill.DNS.Type.A).thenApply(this::parseDnsMessage).get();
			var v6 = query(name, org.xbill.DNS.Type.AAAA).thenApply(this::parseDnsMessage).get();
			// TODO
		} catch (TextParseException | URISyntaxException | InterruptedException | ExecutionException e) {
			throw new RuntimeException("DNS resolution failed", e);
		}
		return null;
	}

	private CompletableFuture<HttpResponse<InputStream>> query(Name name, int type) throws URISyntaxException {
		var rec = Record.newRecord(name, org.xbill.DNS.Type.A, DClass.IN);
		var msg = Message.newQuery(rec);
		var b64 = Base64.getEncoder().encodeToString(msg.toWire());
		var req = HttpRequest.newBuilder(new URI(endpoint.getScheme(), endpoint.getUserInfo(), endpoint.getHost(), endpoint.getPort(), endpoint.getPath(),
					"?dns="+b64, null))
				.header("Accept", "application/dns-message")
				.build();
		return Agent.okhttp().sendAsync(req);
	}
	
	private Message parseDnsMessage(HttpResponse<InputStream> res) {
		if (res.statusCode() == 200) {
			try {
				var baos = new ByteArrayOutputStream();
				Util.copy(res.body(), baos);
				return new Message(baos.toByteArray());
			} catch (IOException e) {
				throw new RuntimeException("DNS resolution failure", e);
			}
		} else {
			throw new RuntimeException("DNS resolution failure - received status code "+res.statusCode()+" from DoH server");
		}
	}

	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		
	}

}
