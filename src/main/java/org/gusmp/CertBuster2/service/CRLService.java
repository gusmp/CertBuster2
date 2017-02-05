package org.gusmp.CertBuster2.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CRL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Getter;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.service.connection.NativeConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CRLService extends BaseService implements IValidatorService {

	public static enum CRL_STATUS {
		GOOD("Good"), REVOKED("Revoked"), EXPIRED("Expired"), NOT_VALID_YET("Not valid yet"), UNKNOWN("Unknown"), NO_CRL_DP(
				"No CRL distribution points are available"), ERROR("Error getting CRL"), NOT_CHECKED("Not checked");

		@Getter
		private String value;

		CRL_STATUS(String value) {
			this.value = value;
		}
	}

	@Autowired
	private NativeConnectionService nativeConnectionService;

	private final String CRLDP_EXTENSION = "2.5.29.31";

	private X509CRL getCrl(String url) throws Exception {
		X509CRL crl = null;

		logger.info("Downloading crl: " + url);
		InputStream crlStream = nativeConnectionService.getFile(url);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		crl = (X509CRL) cf.generateCRL(crlStream);
		crlStream.close();

		return crl;
	}

	private List<List<String>> getCrlDistributionPoints(byte[] crlDistributionPoints) {
		List<List<String>> crlDistributionPointList = new ArrayList<List<String>>(2);

		try {
			ASN1InputStream crlDPExtAsn1Stream = new ASN1InputStream(new ByteArrayInputStream(crlDistributionPoints));
			DEROctetString crlDPExtDerObjStr = (DEROctetString) crlDPExtAsn1Stream.readObject();
			ASN1InputStream asn1is = new ASN1InputStream(crlDPExtDerObjStr.getOctets());
			ASN1Sequence crlDPSeq = (ASN1Sequence) asn1is.readObject();
			for (int i = 0; i < crlDPSeq.size(); i++) {
				DistributionPoint crldp = new DistributionPoint((ASN1Sequence) crlDPSeq.getObjectAt(i).toASN1Primitive());
				GeneralNames gns = (GeneralNames) crldp.getDistributionPoint().getName();
				List<String> listUrlCRL = new ArrayList<String>(gns.getNames().length);
				for (int j = 0; j < gns.getNames().length; j++) {
					listUrlCRL.add(gns.getNames()[j].getName().toString());
				}

				crlDistributionPointList.add(listUrlCRL);
			}

			asn1is.close();
			crlDPExtAsn1Stream.close();
		} catch (IOException exc) {
			logger.error("CRL extension could not be parsed!");
		}

		return crlDistributionPointList;
	}

	private CRL_STATUS certificateRevoked(X509Certificate certificate) throws Exception {

		byte[] crlDpExtension = certificate.getExtensionValue(CRLDP_EXTENSION);
		if (crlDpExtension != null) {
			List<List<String>> crlDistributionPoints = getCrlDistributionPoints(certificate.getExtensionValue(CRLDP_EXTENSION));
			if (crlDistributionPoints.size() == 0) {
				logger.info("Crl distribution point exists but I could not find any url (not http and ended with .crl??)");
				return CRL_STATUS.NO_CRL_DP;
			}

			for (List<String> crlDp : crlDistributionPoints) {
				CRL crl = null;
				for (int i = 0; i < crlDp.size() && crl == null; i++) {
					crl = getCrl(crlDp.get(i));
				}

				if ((crl != null) && (crl.isRevoked(certificate))) {
					return CRL_STATUS.REVOKED;
				}
			}

			Date now = new Date();
			// expired
			if (certificate.getNotAfter().before(now)) {
				return CRL_STATUS.EXPIRED;
			}

			// not valid yet
			if (certificate.getNotBefore().after(now)) {
				return CRL_STATUS.NOT_VALID_YET;
			}

			return CRL_STATUS.GOOD;
		} else {
			logger.info("There is not crl distribution points (ext: 2.5.29.31)");
			return CRL_STATUS.NO_CRL_DP;
		}
	}

	public void checkStatus(CertificateInfo certificate) throws Exception {

		certificate.setCrlStatus(certificateRevoked(certificate.getCertificate()));
	}

}
