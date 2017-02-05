package org.gusmp.CertBuster2.service.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.springframework.stereotype.Service;

@Service
public class NativeConnectionService extends BaseConnection {

	private static class SavingTrustManager implements X509TrustManager {

		private final X509TrustManager tm;
		private X509Certificate[] chain;

		public SavingTrustManager(X509TrustManager tm) {
			this.tm = tm;
		}

		public X509Certificate[] getAcceptedIssuers() {
			throw new UnsupportedOperationException();
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			throw new UnsupportedOperationException();
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			this.chain = chain;
			tm.checkServerTrusted(chain, authType);
		}
	}

	private final int BUFFER_SIZE = 5000;

	@PostConstruct
	private void init() {

		if (useProxy) {
			logger.info("Setting proxy configuration");
			setProxyConfiguration();
		}
	}

	private void setProxyConfiguration() {

		System.setProperty("http.proxyHost", proxyHost);
		System.setProperty("http.proxyPort", String.valueOf(proxyPort));

		System.setProperty("https.proxyHost", proxyHost);
		System.setProperty("https.proxyPort", String.valueOf(proxyPort));

		logger.debug("Setted http(s).proxyHost / http(s).proxyPort setting");

		if ((!proxyUser.isEmpty()) && (!proxyPassword.isEmpty())) {

			logger.debug("Setted authentication settings");
			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
				}
			});
		}
	}

	public CertificateInfo getCertificate(String host, Integer port) throws Exception {

		logger.debug("Checking " + host + ":" + port);

		/*
		 * if (useProxy) { logger.info("Enable proxy for " + host + ":" + port);
		 * setProxyConfiguration(); }
		 */

		// set up a temporal keystore
		KeyStore ks = KeyStore.getInstance("JKS");

		// get a SSLContext
		SSLContext context = SSLContext.getInstance("TLS");

		// create a TrustManagerFactory
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];

		SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);

		// initialize
		context.init(null, new TrustManager[] { tm }, null);

		SSLSocketFactory factory = context.getSocketFactory();
		SSLSocket socket = (SSLSocket) factory.createSocket();
		socket.setSoTimeout(timeOut);

		try {
			socket.connect(new InetSocketAddress(host, port), timeOut);
			socket.startHandshake();

		} catch (SocketTimeoutException exc) {
			logger.info(host + ":" + port + " connection timeout");
			throw new Exception(host + ":" + port + " connection timeout. This port might be listening o filteed by a firewall");

		} catch (Exception exc) {
			logger.debug("Connection to " + host + ":" + port + " failed:  " + exc.toString());
		} finally {
			socket.close();
		}

		X509Certificate[] chain = tm.chain;
		if (chain == null) {
			logger.info(host + ":" + port + " is not an SSL port");
			throw new Exception(host + ":" + port + " is listening but it is not a SSL port");
		}

		logger.debug(host + ":" + port + " is an SSL port. Chain length: " + chain.length);

		return new CertificateInfo(chain[0], context);
	}

	public InputStream getFile(String url) throws Exception {

		logger.debug("Downloading " + url);

		/*
		 * if (useProxy) { logger.info("Enable proxy for " + url);
		 * setProxyConfiguration(); }
		 */

		URL urlObj = new URL(url);
		return urlObj.openConnection().getInputStream();
	}

	public byte[] sendData(byte[] data, String mime, String target) throws Exception {
		try {
			/*
			 * if (useProxy) {
			 * logger.info("Enable proxy for sending raw data to " + target);
			 * setProxyConfiguration(); }
			 */

			URL url = new URL(target);
			URLConnection con = url.openConnection();
			con.setDoInput(true);
			con.setDoOutput(true);

			con.setRequestProperty("Content-Type", mime);
			con.connect();
			OutputStream outStream = con.getOutputStream();
			outStream.write(data);

			InputStream inputStream = con.getInputStream();
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			int byteRead;
			byte[] buffer = new byte[BUFFER_SIZE];

			while ((byteRead = inputStream.read(buffer)) != -1) {
				byteArrayOutputStream.write(buffer, 0, byteRead);
			}

			return (byteArrayOutputStream.toByteArray());
		} catch (MalformedURLException exc) {
			logger.error(target + " is not a valid url\nDetails:\n" + exc.toString());
			throw new Exception(target + " is not a valid url\nDetails:\n" + exc.toString());
		} catch (IOException exc) {
			logger.error("It was impossible to connect to " + target + "\nDetails:\n" + exc.toString());
			throw new Exception("It was impossible to connect to " + target + "\nDetails:\n" + exc.toString());
		}
	}

}
