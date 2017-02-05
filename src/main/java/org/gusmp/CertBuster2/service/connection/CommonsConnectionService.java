package org.gusmp.CertBuster2.service.connection;

import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import lombok.Getter;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.springframework.stereotype.Service;

@Service
public class CommonsConnectionService extends BaseConnection {

	private class AllHostNamesGrantedVerifier implements HostnameVerifier {

		@Getter
		private Certificate[] certs;

		@Override
		public boolean verify(String hostname, SSLSession session) {

			try {
				certs = session.getPeerCertificates();
			} catch (SSLPeerUnverifiedException exc) {
			}

			return true;
		}
	}

	private class AllTrustedStrategy implements TrustStrategy {

		@Override
		public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			return true;
		}
	}

	public CertificateInfo getCertificate(String host, Integer port) throws Exception {

		logger.debug("Checking " + host + ":" + port);

		// https://hc.apache.org/httpcomponents-client-ga/examples.html
		URI uri = new URIBuilder().setScheme("https").setHost(host).setPort(port).build();
		HttpGet httpGet = new HttpGet(uri);

		Builder requestConfigBuilder = RequestConfig.custom().setConnectionRequestTimeout(timeOut).setConnectTimeout(timeOut)
				.setSocketTimeout(timeOut);

		if (useProxy) {
			requestConfigBuilder.setProxy(new HttpHost(proxyHost, proxyPort));
		}

		RequestConfig requestConfig = requestConfigBuilder.build();
		httpGet.setConfig(requestConfig);

		SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
		sslContextBuilder.loadTrustMaterial(new AllTrustedStrategy());
		SSLContext sslContext = sslContextBuilder.build();

		AllHostNamesGrantedVerifier savingTrustManager = new AllHostNamesGrantedVerifier();

		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, savingTrustManager);

		HttpClientBuilder clientBuilder = HttpClients.custom().setSSLSocketFactory(sslsf);

		if ((useProxy) && (!proxyUser.isEmpty()) && (!proxyPassword.isEmpty())) {

			logger.debug("Setted authentication settings");

			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(proxyUser, proxyPassword));

			clientBuilder.setDefaultCredentialsProvider(credsProvider);
		}

		CloseableHttpClient httpclient = clientBuilder.build();

		CloseableHttpResponse response = httpclient.execute(httpGet);
		response.close();
		httpclient.close();

		return new CertificateInfo((X509Certificate) savingTrustManager.certs[0], sslContext);
	}
}
