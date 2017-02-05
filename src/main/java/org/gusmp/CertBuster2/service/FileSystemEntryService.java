package org.gusmp.CertBuster2.service;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.beans.FileSystemScanFileEntry;
import org.gusmp.CertBuster2.beans.IScanFileEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileSystemEntryService extends BaseService implements IEntryService {

	@Value("#{'${certbuster2.fileSystem.allowedExtensions}'.split(',')}")
	private String[] allowedExtensions;

	@Value("${certbuster2.fileSystem.maxSize}")
	private int maxSize;

	@Autowired
	private CertificateService certificateService;

	@Autowired
	private ResumeService resumeService;

	private FilenameFilter filter = new FilenameFilter() {
		public boolean accept(File dir, String name) {

			File nameFile = new File(dir, name);
			boolean ret = false;

			if (nameFile.isDirectory())
				return true;
			if (nameFile.length() > maxSize) {
				logger.debug(name + " exceded maximun size (" + maxSize + ")");
				return false;
			}

			for (String ext : allowedExtensions) {
				if (ext.equalsIgnoreCase("*"))
					return true;
				if (name.toLowerCase().endsWith(ext))
					return true;
			}

			return ret;
		}
	};

	public void processEntry(IScanFileEntry entry, ReportingService reportingService) {

		File f = ((FileSystemScanFileEntry) entry).getPathFile();
		if (f.isFile()) {
			openCertificate(f, entry, reportingService);
		} else {
			findCertificates(f.listFiles(filter), entry, reportingService);
		}
	}

	private void findCertificates(File[] files, IScanFileEntry entry, ReportingService reportingService) {

		for (File f : files) {
			if (f.isFile()) {
				openCertificate(f, entry, reportingService);
			} else {
				findCertificates(f.listFiles(filter), entry, reportingService);
			}
		}
	}

	private void openCertificate(File file, IScanFileEntry entry, ReportingService reportingService) {

		if (!resumeService.processSubEntry(file.getAbsolutePath()))
			return;
		logger.debug("Checking..." + file.getAbsolutePath());

		try {
			List<CertificateInfo> certificateList = certificateService.getCertificateInfo(file);
			reportingService.reportEntry(certificateList, file.getAbsolutePath(), entry);
			logger.debug(file.getAbsolutePath() + " processed correctly");
		} catch (Exception exc) {
			reportingService.reportEntry(exc, file.getAbsolutePath(), entry);
			logger.debug(file.getAbsolutePath() + " seems not to be a certificate: " + exc.getMessage());
		}
	}

}
