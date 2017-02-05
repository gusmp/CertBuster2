package org.gusmp.CertBuster2.service.connection;

import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.service.BaseService;
import org.springframework.beans.factory.annotation.Value;

public abstract class BaseConnection extends BaseService {

	@Value("${certbuster2.proxy.useProxy}")
	protected boolean useProxy;

	@Value("${certbuster2.proxy.host}")
	protected String proxyHost;

	@Value("${certbuster2.proxy.port}")
	protected Integer proxyPort;

	@Value("${certbuster2.proxy.user}")
	protected String proxyUser;

	@Value("${certbuster2.proxy.password}")
	protected String proxyPassword;

	@Value("${certbuster2.connection.timeout}")
	protected int timeOut;

	public abstract CertificateInfo getCertificate(String host, Integer port) throws Exception;

}
