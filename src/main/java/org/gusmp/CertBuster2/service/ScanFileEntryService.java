package org.gusmp.CertBuster2.service;

import org.gusmp.CertBuster2.beans.IScanFileEntry;
import org.gusmp.CertBuster2.beans.IScanFileEntry.ENTRY_TYPE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScanFileEntryService extends BaseService {

	@Autowired
	private FileSystemEntryService fileSystemEntryService;

	@Autowired
	private NetworkEntryService networkEntyService;

	public void processEntry(IScanFileEntry entry, ReportingService reportingService) {

		if (entry.getEntryType() == ENTRY_TYPE.FILESYSTEM) {
			fileSystemEntryService.processEntry(entry, reportingService);
		} else if (entry.getEntryType() == ENTRY_TYPE.NETWORK) {
			networkEntyService.processEntry(entry, reportingService);
		}
	}

}
