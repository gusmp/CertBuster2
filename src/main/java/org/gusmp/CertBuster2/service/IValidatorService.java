package org.gusmp.CertBuster2.service;

import org.gusmp.CertBuster2.beans.CertificateInfo;

public interface IValidatorService {

	public void checkStatus(CertificateInfo certificate) throws Exception;

}
