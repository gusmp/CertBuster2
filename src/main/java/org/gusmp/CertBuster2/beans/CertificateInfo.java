package org.gusmp.CertBuster2.beans;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.gusmp.CertBuster2.service.CRLService.CRL_STATUS;
import org.gusmp.CertBuster2.service.OCSPService.OCSP_STATUS;

import lombok.Getter;
import lombok.Setter;

public class CertificateInfo {

	@Getter
	private X509Certificate certificate;

	@Getter
	private SSLContext context;

	@Getter
	@Setter
	private CRL_STATUS crlStatus;

	@Getter
	@Setter
	private OCSP_STATUS OCSPStatus;

	public CertificateInfo(X509Certificate certificate) {
		this.certificate = certificate;
		this.crlStatus = CRL_STATUS.NOT_CHECKED;
		this.OCSPStatus = OCSP_STATUS.NOT_CHECKED;
	}

	public CertificateInfo(X509Certificate certificate, SSLContext context) {
		this(certificate);
		this.context = context;
	}

}
