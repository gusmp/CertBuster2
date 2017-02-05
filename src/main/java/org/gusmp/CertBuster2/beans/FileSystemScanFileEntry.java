package org.gusmp.CertBuster2.beans;

import java.io.File;

import org.gusmp.CertBuster2.service.WrongScanFileEntryException;

import lombok.Getter;

public class FileSystemScanFileEntry implements IScanFileEntry {

	private String path;

	@Getter
	private File pathFile;

	public FileSystemScanFileEntry(String path) throws WrongScanFileEntryException {
		this.path = path;

		pathFile = new File(path);

		if ((!pathFile.isDirectory()) && (!pathFile.isFile())) {
			throw new WrongScanFileEntryException("File system entry " + path + " is not valid");
		}

	}

	@Override
	public ENTRY_TYPE getEntryType() {
		return ENTRY_TYPE.FILESYSTEM;
	}

	@Override
	public String getEntry() {
		return path;
	}

}
