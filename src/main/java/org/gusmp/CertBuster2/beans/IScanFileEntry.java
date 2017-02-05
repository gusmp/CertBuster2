package org.gusmp.CertBuster2.beans;

public interface IScanFileEntry {

	public enum ENTRY_TYPE {
		FILESYSTEM, NETWORK
	};

	public String getEntry();

	public ENTRY_TYPE getEntryType();

}
