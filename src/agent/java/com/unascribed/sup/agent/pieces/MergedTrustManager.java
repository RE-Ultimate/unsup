package com.unascribed.sup.agent.pieces;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

public class MergedTrustManager implements X509TrustManager {
	private final X509Certificate[] certs;
	private final X509TrustManager a;
	private final X509TrustManager b;
	
	public static X509TrustManager of(X509TrustManager a, X509TrustManager b) {
		if (a instanceof X509ExtendedTrustManager ax && b instanceof X509ExtendedTrustManager bx)
			return new MergedExtendedTrustManager(ax, bx);
		return new MergedTrustManager(a, b);
	}

	private MergedTrustManager(X509TrustManager a, X509TrustManager b) {
		this.a = a;
		this.b = b;
		this.certs = mergeCerts(a, b);
	}

	private static X509Certificate[] mergeCerts(X509TrustManager a, X509TrustManager b) {
		return Stream.concat(
					Arrays.stream(b.getAcceptedIssuers()),
					Arrays.stream(a.getAcceptedIssuers())
				).toArray(X509Certificate[]::new);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return certs;
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		try {
			b.checkServerTrusted(chain, authType);
		} catch (CertificateException e) {
			a.checkServerTrusted(chain, authType);
		}
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		try {
			b.checkClientTrusted(chain, authType);
		} catch (CertificateException e) {
			a.checkClientTrusted(chain, authType);
		}
	}
	
	// why is this a class and not an interface
	public static class MergedExtendedTrustManager extends X509ExtendedTrustManager {
		private final X509Certificate[] certs;
		private final X509ExtendedTrustManager a;
		private final X509ExtendedTrustManager b;
		
		private MergedExtendedTrustManager(X509ExtendedTrustManager a, X509ExtendedTrustManager b) {
			this.a = a;
			this.b = b;
			this.certs = mergeCerts(a, b);
		}
		
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return certs;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			try {
				b.checkServerTrusted(chain, authType);
			} catch (CertificateException e) {
				a.checkServerTrusted(chain, authType);
			}
		}
		
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
			try {
				b.checkServerTrusted(chain, authType, socket);
			} catch (CertificateException e) {
				a.checkServerTrusted(chain, authType, socket);
			}
		}
		
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
			try {
				b.checkServerTrusted(chain, authType, engine);
			} catch (CertificateException e) {
				a.checkServerTrusted(chain, authType, engine);
			}
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			try {
				b.checkClientTrusted(chain, authType);
			} catch (CertificateException e) {
				a.checkClientTrusted(chain, authType);
			}
		}
		
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
			try {
				b.checkClientTrusted(chain, authType, socket);
			} catch (CertificateException e) {
				a.checkClientTrusted(chain, authType, socket);
			}
		}
		
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
			try {
				b.checkClientTrusted(chain, authType, engine);
			} catch (CertificateException e) {
				a.checkClientTrusted(chain, authType, engine);
			}
		}

	}
}