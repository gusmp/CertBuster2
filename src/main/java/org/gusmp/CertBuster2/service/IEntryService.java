package org.gusmp.CertBuster2.service;

import org.gusmp.CertBuster2.beans.IScanFileEntry;

public interface IEntryService {

	public void processEntry(IScanFileEntry entry, ReportingService reportingService);

}
