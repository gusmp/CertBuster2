package org.gusmp.CertBuster2.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.Cleanup;

import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.service.connection.BaseConnection;
import org.gusmp.CertBuster2.service.connection.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CertificateService extends BaseService {

	@Value("${certbuster2.connection.reTryNumber}")
	private int reTryNumber;

	@Autowired
	private ConnectionService connectionService;


	@Autowired
	private ValidationCertificateService validationCertificateService;

	private List<BaseConnection> enabledConnectionServiceList = new ArrayList<BaseConnection>();

	@PostConstruct
	private void init() throws Exception {
		enabledConnectionServiceList = connectionService.getEnabledConnectionService();
	}

	public List<CertificateInfo> getCertificateInfo(File certificate) throws Exception {

		// X509
		try {
			CertificateInfo certificateInfo = openX509Certificate(certificate);
			logger.debug(certificate.getAbsolutePath() + " is an X509 certificate");
			return Arrays.asList(certificateInfo);

		} catch (Exception exc) {
			logger.debug(certificate.getAbsolutePath() + " is not an X509 certificate." + exc.toString());
		}

		// JKS
		try {
			List<CertificateInfo> certificateInfoList = openJKSCertificate(certificate);
			logger.debug(certificate.getAbsolutePath() + " is a JKS keystore");
			return certificateInfoList;

		} catch (Exception exc) {
			logger.debug(certificate.getAbsolutePath() + " is not a JKS keystore." + exc.toString());
		}

		throw new Exception(certificate.getAbsolutePath() + " ist not a supported certificate file");
	}

	private CertificateInfo openX509Certificate(File certificate) throws Exception {

		@Cleanup
		InputStream inStream = new FileInputStream(certificate);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
		return validationCertificateService.validateCertificate(new CertificateInfo(cert));
	}

	private List<CertificateInfo> openJKSCertificate(File certificate) throws Exception {

		@Cleanup
		InputStream is = new FileInputStream(certificate);
		KeyStore keystore = KeyStore.getInstance("JKS");
		keystore.load(is, null);

		List<CertificateInfo> certificateInfoList = new ArrayList<CertificateInfo>();

		Enumeration<String> enumeration = keystore.aliases();
		while (enumeration.hasMoreElements()) {
			String alias = (String) enumeration.nextElement();

			X509Certificate x509certificate = (X509Certificate) keystore.getCertificate(alias);
			if (x509certificate != null) {
				// note: pkcs#12 can be openned as jks without providing a
				// password! However, no certificate (public key can be
				// retrieved)
				certificateInfoList.add(validationCertificateService.validateCertificate(new CertificateInfo(x509certificate)));
			}
		}

		return certificateInfoList;
	}

	public CertificateInfo getSSLCertificate(String host, int port) throws Exception {

		for (BaseConnection c : enabledConnectionServiceList) {

			for (int i = 1; i <= reTryNumber; i++) {
				try {
					return validationCertificateService.validateCertificate(c.getCertificate(host, port));
				} catch (Exception exc) {
					logger.debug("Error getting SSL certificate. Host " + host + " Port: " + port + " Try: " + i + " Exc: " + exc.toString());

					if (enabledConnectionServiceList.indexOf(c) == (enabledConnectionServiceList.size() - 1)) {
						if (i == reTryNumber) {
							throw exc;
						}
					}
				}
			}
		}

		return null;
	}

}
