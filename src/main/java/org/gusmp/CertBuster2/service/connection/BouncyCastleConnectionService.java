package org.gusmp.CertBuster2.service.connection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import lombok.Cleanup;
import lombok.Getter;

import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.DefaultTlsClient;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsClientProtocol;
import org.bouncycastle.crypto.tls.TlsCredentials;
import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.springframework.stereotype.Service;

@Service
public class BouncyCastleConnectionService extends BaseConnection {

	private class MyDefaultTlsClient extends DefaultTlsClient {

		@Getter
		private X509Certificate sslCertificate;

		@Override
		public TlsAuthentication getAuthentication() throws IOException {
			TlsAuthentication auth = new TlsAuthentication()

			{
				// Capture the server certificate information!
				public void notifyServerCertificate(org.bouncycastle.crypto.tls.Certificate serverCertificate) throws IOException {

					logger.debug(serverCertificate.getCertificateAt(0).getSubject().toString());
					try {
						CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
						sslCertificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(serverCertificate
								.getCertificateAt(0).getEncoded()));
					} catch (Exception exc) {
						sslCertificate = null;
					}
				}

				public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {
					return null;
				}
			};
			return auth;
		}
	}

	public CertificateInfo getCertificate(String host, Integer port) throws Exception {

		logger.debug("Checking " + host + ":" + port);

		java.security.SecureRandom secureRandom = new java.security.SecureRandom();
		@Cleanup
		Socket socket = new Socket(java.net.InetAddress.getByName(host), port);
		socket.setSoTimeout(timeOut);
		TlsClientProtocol protocol = new TlsClientProtocol(socket.getInputStream(), socket.getOutputStream(), secureRandom);

		MyDefaultTlsClient client = new MyDefaultTlsClient();

		try {
			protocol.connect(client);
		} catch (Exception exc) {
			logger.debug(exc.toString());
		} finally {
			protocol.close();
		}

		if (client.getSslCertificate() == null) {
			logger.info(host + ":" + port + " is not an SSL port");
			throw new Exception(host + ":" + port + " is listening but it is not a SSL port");
		}

		logger.debug(host + ":" + port + " is an SSL port");

		return new CertificateInfo(client.getSslCertificate(), SSLContext.getDefault());

	}
}
