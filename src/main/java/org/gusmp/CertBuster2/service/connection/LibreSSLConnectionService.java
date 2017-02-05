package org.gusmp.CertBuster2.service.connection;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LibreSSLConnectionService extends ExternalBaseConnection {

	@Value("${certbuster2.connection.openSSLCastleConnectionService.libreSSLExecutable}")
	private String libreSSLExecutablePath;

	@Override
	protected List<String> getCommandLine(String host, int port) {
		List<String> cmdTokens = Arrays.asList(libreSSLExecutablePath, "s_client", "-connect", String.format("%s:%d", host, port));
		if (useProxy == true) {
			cmdTokens.add("--proxy");
			cmdTokens.add(String.format("%s:%d", proxyHost, proxyPort));
		}
		return cmdTokens;
	}

	@Override
	protected boolean isValid() throws Exception {
		return isValidInternal(libreSSLExecutablePath);
	}
}
