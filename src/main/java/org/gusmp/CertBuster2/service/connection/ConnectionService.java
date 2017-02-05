package org.gusmp.CertBuster2.service.connection;

import java.util.ArrayList;
import java.util.List;

import org.gusmp.CertBuster2.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConnectionService extends BaseService {

	@Value("#{'${certbuster2.connection.connectionChain}'.split(',')}")
	private List<CONNECTION_METHOD> connectionChain;

	@Autowired
	private NativeConnectionService nativeConnectionService;

	@Autowired
	private CommonsConnectionService commonsConnectionService;

	@Autowired
	private BouncyCastleConnectionService bouncyCastleConnectionService;

	@Autowired
	private GnuTLSConnectionService gnuTLSConnectionService;

	@Autowired
	private OpenSSLConnectionService openSSLConnectionService;
	
	@Autowired
	private LibreSSLConnectionService libreSSLConnectionService;
	

	public static enum CONNECTION_METHOD {
		NATIVE, COMMONS, BC, GNUTLS, OPENSSL, LIBRESSL
	};

	public List<BaseConnection> getEnabledConnectionService() throws Exception {

		List<BaseConnection> enabledConnectionServiceList = new ArrayList<BaseConnection>();

		if (connectionChain.size() == 0) {
			logger.info("No connection services were enabled. Using nativeConnectionService");
			enabledConnectionServiceList.add(nativeConnectionService);
		} else {
			for (CONNECTION_METHOD connection : connectionChain) {
				switch (connection) {
				case NATIVE:
					enabledConnectionServiceList.add(nativeConnectionService);
					break;
				case COMMONS:
					enabledConnectionServiceList.add(commonsConnectionService);
					break;
				case BC:
					enabledConnectionServiceList.add(bouncyCastleConnectionService);
					break;
				case GNUTLS:
					try {
						((ExternalBaseConnection) gnuTLSConnectionService).isValid();
						enabledConnectionServiceList.add(gnuTLSConnectionService);
					} catch (Exception exc) {
						logger.info("Wrong gnuTLS configuration: " + exc.getMessage());
					}
					break;
				case OPENSSL:
					try {
						((ExternalBaseConnection) openSSLConnectionService).isValid();
						enabledConnectionServiceList.add(openSSLConnectionService);
					} catch (Exception exc) {
						logger.info("Wrong openSSL configuration: " + exc.getMessage());
					}
				case LIBRESSL:
					try {
						((ExternalBaseConnection) libreSSLConnectionService).isValid();
						enabledConnectionServiceList.add(libreSSLConnectionService);
					} catch (Exception exc) {
						logger.info("Wrong libreSSL configuration: " + exc.getMessage());
					}
				}
			}
		}

		if (enabledConnectionServiceList.size() == 0) {
			logger.info("No connection services were enabled because the selected connection services are not configured properly");
			throw new Exception("No connection services were enabled because the selected connection services are not configured properly");
		}

		return enabledConnectionServiceList;
	}

}
