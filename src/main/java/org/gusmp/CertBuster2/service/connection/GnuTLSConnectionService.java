package org.gusmp.CertBuster2.service.connection;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GnuTLSConnectionService extends ExternalBaseConnection {

	@Value("${certbuster2.connection.gnuTLSCastleConnectionService.gnutlsClientExecutable}")
	private String gnuTLSExecutablePath;

	@Override
	protected List<String> getCommandLine(String host, int port) {
		return Arrays.asList(gnuTLSExecutablePath, "--print_cert", "--port", String.valueOf(port), host);
	}

	@Override
	protected boolean isValid() throws Exception {
		return isValidInternal(gnuTLSExecutablePath);
	}
}
