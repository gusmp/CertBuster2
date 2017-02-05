package org.gusmp.CertBuster2.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Random;

import lombok.Getter;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.service.connection.NativeConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OCSPService extends BaseService implements IValidatorService {

	private class OCSPInfo {

		@Getter
		private String ocspUrl = null;

		@Getter
		private String issuerUrl = null;

		public OCSPInfo(String ocspUrl, String issuerUrl) {
			this.ocspUrl = ocspUrl;
			this.issuerUrl = issuerUrl;
		}
	}

	private class OCSPException extends Exception {

		private static final long serialVersionUID = -1888614672057006898L;

		@Getter
		private OCSP_STATUS status;

		public OCSPException(OCSP_STATUS status) {
			this.status = status;
		}
	}

	public static enum OCSP_STATUS {
		GOOD("Good"), REVOKED("Revoked"), EXPIRED("Expired"), UNKNOWN("Unknown"), NO_AIA_EXTENSION(
				"The certificate has no authority information access extension"), NO_URL_OCSP(
				"The certificate has AIA extension but no OCSP url could be found"), NO_URL_ISSUER(
				"The certificate has AIA extension and OCSP url but the issuer url could not be found"), MALFORMED_REQUEST(
				"Request was formed incorrectly"), INTERNAL_ERROR("There was an error in the OCSP Server"), TRY_LATER(
				"OCSP server is not ready. Try later"), SIG_REQUIRED("OCSP request must be signed"), UNAUTHORIZED("Request are not accepted"), UNKNOWN_SERVER_CODE(
				"OCSP server returns an unknown return code"), NOT_CHECKED("Not checked");

		private String value;

		OCSP_STATUS(String value) {
			this.value = value;
		}

		public String getValue() {
			return (this.value);
		}
	}

	@Autowired
	private NativeConnectionService nativeConnectionService;

	private final String AIA_EXTENSION = "1.3.6.1.5.5.7.1.1";
	private final String OCSP_URL = "1.3.6.1.5.5.7.48.1";
	private final String ISSUER_URL = "1.3.6.1.5.5.7.48.2";

	private OCSPInfo getOcspInfo(X509Certificate certificate) throws Exception {
		String ocspUrl = null;
		String issuerUrl = null;

		byte[] aiaExtensionDER = certificate.getExtensionValue(AIA_EXTENSION);
		ASN1InputStream asn1InputStream = null;

		if (aiaExtensionDER == null) {
			throw new OCSPException(OCSP_STATUS.NO_AIA_EXTENSION);
		}

		try {
			ASN1InputStream aiaExtensionSeq = new ASN1InputStream(new ByteArrayInputStream(aiaExtensionDER));
			DEROctetString derObjectString = (DEROctetString) aiaExtensionSeq.readObject();
			aiaExtensionSeq.close();
			asn1InputStream = new ASN1InputStream(derObjectString.getOctets());
			ASN1Sequence asn1Sequence = (ASN1Sequence) asn1InputStream.readObject();
			asn1InputStream.close();
			AccessDescription accessDescription = null;

			for (int i = 0; i < asn1Sequence.size(); i++) {
				accessDescription = AccessDescription.getInstance((ASN1Sequence) asn1Sequence.getObjectAt(i).toASN1Primitive());
				if (accessDescription.getAccessMethod().toString().equalsIgnoreCase(OCSP_URL) == true) {
					GeneralName gn = accessDescription.getAccessLocation();
					ocspUrl = gn.getName().toString();
					continue;
				}
				if (accessDescription.getAccessMethod().toString().equalsIgnoreCase(ISSUER_URL) == true) {
					GeneralName gn = accessDescription.getAccessLocation();
					issuerUrl = gn.getName().toString();
					continue;
				}
			}

			if (ocspUrl == null) {
				throw new OCSPException(OCSP_STATUS.NO_URL_OCSP);
			}
			if (issuerUrl == null) {
				throw new OCSPException(OCSP_STATUS.NO_URL_ISSUER);
			}

			return new OCSPInfo(ocspUrl, issuerUrl);

		} catch (IOException exc) {
			throw new Exception("It was an error while getting extension in order to get the OCSP url.\n" + exc.toString());
		} finally {
			if (asn1InputStream != null) {
				try {
					asn1InputStream.close();
				} catch (IOException excIO) {
				}
			}
		}
	}

	public OCSP_STATUS getStatus(X509Certificate issuer, BigInteger serialNumber, String ocspUrl) throws Exception {
		try {
			logger.debug("Getting status of " + serialNumber + " through OCSP");

			// create OCSP request
			X509CertificateHolder certificateHolder = new X509CertificateHolder(issuer.getEncoded());

			DigestCalculatorProvider digCalcProv = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();
			CertificateID certificateId = new CertificateID(digCalcProv.get(CertificateID.HASH_SHA1), certificateHolder, serialNumber);

			OCSPReqBuilder ocspRequestBuilder = new OCSPReqBuilder();
			ocspRequestBuilder.addRequest(certificateId);

			// add nonce
			byte[] sampleNonce = new byte[16];
			Random rand = new Random();
			rand.nextBytes(sampleNonce);
			ExtensionsGenerator extGen = new ExtensionsGenerator();
			extGen.addExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false, new DEROctetString(sampleNonce));
			ocspRequestBuilder.setRequestExtensions(extGen.generate());
			logger.debug("None OK for " + serialNumber);

			// add requestor name
			ocspRequestBuilder.setRequestorName(new GeneralName(GeneralName.directoryName, new X500Name(issuer.getSubjectDN().getName())));

			OCSPReq ocspRequest = ocspRequestBuilder.build();
			logger.debug("Add requestor name successfully for " + serialNumber);

			// send OCSP request
			byte[] bOcspResponse = nativeConnectionService.sendData(ocspRequest.getEncoded(), "application/ocsp-request", ocspUrl);

			OCSPResp ocspResponse = new OCSPResp(bOcspResponse);
			logger.debug("OCSP status for " + serialNumber + " " + ocspResponse.getStatus());

			switch (ocspResponse.getStatus()) {
			case OCSPResponseStatus.SUCCESSFUL:
				logger.info("OCSP response was: SUCCESSFUL");
				BasicOCSPResp resp = (BasicOCSPResp) ocspResponse.getResponseObject();

				if (resp.getResponses()[0].getCertStatus() == CertificateStatus.GOOD) {
					logger.info("Status of " + serialNumber + " is GOOD");
					return OCSP_STATUS.GOOD; // (CertificateStatusValues.V);
				} else if (resp.getResponses()[0].getCertStatus() instanceof RevokedStatus) {
					logger.info("Status of " + serialNumber + " is REVOKED");
					return OCSP_STATUS.REVOKED; // (CertificateStatusValues.R);
				} else if (resp.getResponses()[0].getCertStatus() instanceof UnknownStatus) {
					logger.info("Status of " + serialNumber + " is UNKNOWN");
					return OCSP_STATUS.UNKNOWN; // (CertificateStatusValues.U);
				}
				break;
			case OCSPResponseStatus.MALFORMED_REQUEST:
				logger.info("OCSP response was: MALFORMED_REQUEST");
				return OCSP_STATUS.MALFORMED_REQUEST;
			case OCSPResponseStatus.INTERNAL_ERROR:
				logger.info("OCSP response was: INTERNAL_ERROR");
				return OCSP_STATUS.INTERNAL_ERROR;
			case OCSPResponseStatus.TRY_LATER:
				logger.info("OCSP response was: TRY_LATER");
				return OCSP_STATUS.TRY_LATER;
			case OCSPResponseStatus.SIG_REQUIRED:
				logger.info("OCSP response was: SIG_REQUIRED");
				return OCSP_STATUS.SIG_REQUIRED;
			case OCSPResponseStatus.UNAUTHORIZED:
				logger.info("OCSP response was: UNAUTHORIZED");
				return OCSP_STATUS.UNAUTHORIZED;
			}

			logger.info("OCSP server returns for " + serialNumber + " and unknown error code");
			return OCSP_STATUS.UNKNOWN;

		} catch (IOException exc) {
			logger.error("Error when creating OCSP request." + exc.getMessage());
			throw new Exception("Error when creating the certificate." + exc.getMessage());
		} catch (OperatorCreationException exc) {
			logger.error("Error generating OCSP request." + exc.getMessage());
			throw new Exception("Error generating OCSP request." + exc.getMessage());
		} catch (org.bouncycastle.cert.ocsp.OCSPException exc) {
			logger.error("Error creating CertificateID object.\n" + exc.toString());
			throw new Exception("Error creating CertificateID object.\n" + exc.toString());
		} catch (CertificateEncodingException exc) {
			logger.error("The certificate to test has an unknown encoding format.\n" + exc.toString());
			throw new Exception("The certificate to test has an unknown encoding format.\n" + exc.toString());
		}
	}

	@Override
	public void checkStatus(CertificateInfo certificate) throws Exception {

		try {
			OCSPInfo ocspInfo = getOcspInfo(certificate.getCertificate());

			InputStream inStream = nativeConnectionService.getFile(ocspInfo.getIssuerUrl());
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			X509Certificate issuerCert = (X509Certificate) certFactory.generateCertificate(inStream);

			certificate.setOCSPStatus(getStatus(issuerCert, certificate.getCertificate().getSerialNumber(), ocspInfo.getOcspUrl()));
		} catch (OCSPException exc) {
			certificate.setOCSPStatus(exc.getStatus());
		}

	}

}
