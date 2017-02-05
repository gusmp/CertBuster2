package org.gusmp.CertBuster2.service.connection;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.Cleanup;

import org.gusmp.CertBuster2.beans.CertificateInfo;

public abstract class ExternalBaseConnection extends BaseConnection {

	private class GetCertificateTask implements Callable<String> {

		private ProcessBuilder processBuilder;
		private Process process;

		public GetCertificateTask(ProcessBuilder processBuilder) {
			this.processBuilder = processBuilder;
		}

		@Override
		public String call() throws Exception {

			process = processBuilder.start();

			@Cleanup
			InputStream is = process.getInputStream();
			@Cleanup
			InputStreamReader isr = new InputStreamReader(is);
			@Cleanup
			BufferedReader br = new BufferedReader(isr);

			String line;
			String x509CertificatePEM = "";
			boolean isCert = false;

			while ((line = br.readLine()) != null) {

				if (line.equalsIgnoreCase(BEGIN_SSL_CERTIFICATE) == true) {
					isCert = true;
				}

				if (line.equalsIgnoreCase(END_SSL_CERTIFICATE) == true) {
					isCert = false;
					x509CertificatePEM += line;
					break;
				}

				if (isCert == true) {
					x509CertificatePEM += line + "\n";
				}
			}

			process.destroy();

			return x509CertificatePEM;
		}

		public void cleanUp() {
			this.process.destroy();
		}
	}

	private static final String BEGIN_SSL_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
	private static final String END_SSL_CERTIFICATE = "-----END CERTIFICATE-----";
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	protected abstract List<String> getCommandLine(String host, int port);

	protected abstract boolean isValid() throws Exception;

	protected boolean isValidInternal(String executablePath) throws Exception {

		File f = new File(executablePath);
		if (!f.isFile())
			throw new Exception(executablePath + " is not a file");
		if (!f.canExecute())
			throw new Exception(executablePath + " is not an executable file");
		return true;
	}

	public CertificateInfo getCertificate(String host, Integer port) throws Exception {

		logger.info("Checking " + host + ":" + port);

		ProcessBuilder processBuilder = new ProcessBuilder(getCommandLine(host, port));

		Map<String, String> environment = processBuilder.environment();
		if (useProxy) {
			String urlProxy;
			if ((proxyUser != null) && (proxyPassword != null)) {
				urlProxy = String.format("http://%s:%s@%s:%d", proxyUser, proxyPassword, proxyHost, proxyPort);
			} else {
				urlProxy = String.format("http://%s:%d", proxyHost, proxyPort);
			}

			logger.info("Url proxy:  " + urlProxy);
			environment.put("https_proxy", urlProxy);
			environment.put("HTTPS_PROXY", urlProxy);
		}

		ExecutorService executor = Executors.newSingleThreadExecutor();
		GetCertificateTask getCertificateTask = new GetCertificateTask(processBuilder);
		Future<String> future = executor.submit(getCertificateTask);

		String x509CertificatePEM = "";
		try {
			x509CertificatePEM = future.get(timeOut, TimeUnit.MILLISECONDS);
		} catch (TimeoutException exc) {
			logger.info("timeout getting SSL certificate for " + host + ":" + port);
		}

		getCertificateTask.cleanUp();
		executor.shutdownNow();

		if (x509CertificatePEM.isEmpty()) {
			logger.info("No SSL certificate was found for " + host + ":" + port);
			throw new Exception(host + ":" + port + " no certificate was found");
		}

		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		// JDK 1.7 StandardCharsets.UTF_8
		X509Certificate sslCertificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(x509CertificatePEM
				.getBytes(UTF8_CHARSET)));

		return new CertificateInfo(sslCertificate);

	}
}
