package org.gusmp.CertBuster2.service;

import java.util.Arrays;

import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.beans.IScanFileEntry;
import org.gusmp.CertBuster2.beans.NetworkScanFileEntry;
import org.gusmp.CertBuster2.beans.NetworkScanFileEntry.PortRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NetworkEntryService extends BaseService implements IEntryService {

	@Autowired
	private CertificateService certificateService;

	@Autowired
	private ResumeService resumeService;

	public void processEntry(IScanFileEntry entry, ReportingService reportingService) {

		NetworkScanFileEntry networkEntry = (NetworkScanFileEntry) entry;

		String[] listOfHosts = networkEntry.getHostRange().getInfo().getAllAddresses();
		if (listOfHosts.length == 0) {
			listOfHosts = new String[1];
			listOfHosts[0] = networkEntry.getHostRange().getInfo().getAddress();
		}

		for (String ip : listOfHosts) {

			for (PortRange portRange : networkEntry.getPortRangeList()) {

				for (int port = portRange.getStart(); port <= portRange.getEnd(); port++) {

					if (!resumeService.processSubEntry(ip + ":" + port))
						continue;

					logger.debug("Checking... " + ip + ":" + port);
					try {
						CertificateInfo certificateInfo = certificateService.getSSLCertificate(ip, port);
						logger.debug("Found certificate in " + ip + ":" + port);
						reportingService.reportEntry(Arrays.asList(certificateInfo), ip + ":" + port, entry);
					} catch (Exception exc) {
						logger.debug(ip + ":" + port + " seems not to be alive or be a ssl port: " + exc.getMessage());
						reportingService.reportEntry(exc, ip + ":" + port, entry);
					}
				}
			}
		}
	}
}
