package org.gusmp.CertBuster2.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.service.CRLService.CRL_STATUS;
import org.gusmp.CertBuster2.service.OCSPService.OCSP_STATUS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ValidationCertificateService extends BaseService {

	@Value("${certbuster2.certificate.validate}")
	private Boolean validateCertificate;

	@Value("#{'${certbuster2.certificate.validateChain}'.split(',')}")
	private List<VALIDATE_METHOD> validateChain;

	public static enum VALIDATE_METHOD {
		CRL, OCSP
	};

	@Autowired
	private CRLService crlService;

	@Autowired
	private OCSPService ocspService;

	@PostConstruct
	private void init() throws Exception {

		if (validateCertificate == true) {
			if (validateChain.size() == 0) {
				throw new Exception(
						"'validateChain' property is empty.Check the property 'certbuster2.certificate.validateChain' in your application.properties");
			}
		}

	}

	public CertificateInfo validateCertificate(CertificateInfo certificate) {
		if (validateCertificate) {

			validationsLoop: for (VALIDATE_METHOD validateMethod : validateChain) {

				switch (validateMethod) {
				case CRL:
					try {
						crlService.checkStatus(certificate);
						break validationsLoop;
					} catch (Exception exc) {
						logger.error("Error checking CRL " + exc.toString());
						certificate.setCrlStatus(CRL_STATUS.ERROR);
					}
					break;
				case OCSP:
					try {
						ocspService.checkStatus(certificate);

						if ((certificate.getOCSPStatus() == OCSP_STATUS.GOOD) || (certificate.getOCSPStatus() == OCSP_STATUS.EXPIRED)
								|| (certificate.getOCSPStatus() == OCSP_STATUS.REVOKED)) {
							break validationsLoop;
						}
					} catch (Exception exc) {
						logger.error("Error checking OCSP " + exc.toString());
						certificate.setCrlStatus(CRL_STATUS.ERROR);
					}
					break;
				}
			}

		}

		return certificate;
	}
}
