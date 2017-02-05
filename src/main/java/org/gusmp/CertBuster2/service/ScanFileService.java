package org.gusmp.CertBuster2.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.gusmp.CertBuster2.beans.FileSystemScanFileEntry;
import org.gusmp.CertBuster2.beans.IScanFileEntry;
import org.gusmp.CertBuster2.beans.NetworkScanFileEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.Cleanup;

@Service
public class ScanFileService extends BaseService {

	@Value("${certbuster2.discovery.scanFile}")
	private String scanFile;

	private List<IScanFileEntry> entryList = new ArrayList<IScanFileEntry>();

	private static final String COMENTARY_MARK = "#";

	@PostConstruct
	private void init() {

		logger.debug("Scan file: " + scanFile);

		try {
			@Cleanup
			InputStream fis = new FileInputStream(scanFile);
			@Cleanup
			InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
			@Cleanup
			BufferedReader br = new BufferedReader(isr);
			String line = "";
			while ((line = br.readLine()) != null) {

				if (line.startsWith(COMENTARY_MARK))
					continue;
				line = line.trim();
				if (line.length() == 0)
					continue;

				try {
					IScanFileEntry entry = new FileSystemScanFileEntry(line);
					entryList.add(entry);
					logger.debug("File system entry " + line + " was added successfully");
					continue;
				} catch (WrongScanFileEntryException exc) {
					logger.info(exc.getMessage());
				}

				try {
					IScanFileEntry entry = new NetworkScanFileEntry(line);
					entryList.add(entry);
					logger.debug("Network entry " + line + " was added successfully");
					continue;
				} catch (WrongScanFileEntryException exc) {
					logger.info(exc.getMessage());
				}

				logger.info("Entry " + line + " has been discarted!!!");
			}

			logger.debug(entryList.size() + " entries were detected from " + scanFile);

		} catch (Exception exc) {
			logger.error("Scan file " + scanFile + " could not be opened. Check if the file exists, it can be read."
					+ "Furthermore check the property 'certbuster2.discovery.scanFile' in appliction.properties");
		}
	}

	public IScanFileEntry getEntry(int index) {
		return entryList.get(index);
	}

	public int size() {
		return entryList.size();
	}
}
