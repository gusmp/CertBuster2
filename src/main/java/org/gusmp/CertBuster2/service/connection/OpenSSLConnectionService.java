package org.gusmp.CertBuster2.service.connection;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenSSLConnectionService extends ExternalBaseConnection {

	@Value("${certbuster2.connection.openSSLCastleConnectionService.openSSLExecutable}")
	private String openSSLExecutablePath;

	@Override
	protected List<String> getCommandLine(String host, int port) {
		return Arrays.asList(openSSLExecutablePath, "s_client", "-connect", String.format("%s:%d", host, port));
	}

	@Override
	protected boolean isValid() throws Exception {
		return isValidInternal(openSSLExecutablePath);
	}
}
