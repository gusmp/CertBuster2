package org.gusmp.CertBuster2.reporter;

import java.util.List;

import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.beans.IScanFileEntry;

public interface IReporter {

	public void init() throws Exception;

	public void reportEntry(List<CertificateInfo> certificateInfoList, String object, IScanFileEntry entry);

	public void reportEntry(Exception exception, String object, IScanFileEntry entry);

	public void close();

}
