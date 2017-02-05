package org.gusmp.CertBuster2.service;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.Getter;

import org.gusmp.CertBuster2.beans.IScanFileEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResumeService extends BaseService {

	public static enum ENTRY_TYPE {
		ENTRY, SUB_ENTRY, FULL_CSV_REPORT, FUTURE_CADUCITY_REPORT
	};

	@Value("${certbuster2.resume.baseResumeFile}")
	private String baseResumeFile;

	private static final int ENTRY_POSITION_RESUME_FILE = 0;
	private static final int SUB_ENTRY_POSITION_RESUME_FILE = 500;
	private static final int FULL_CSV_REPORT_POSITION_RESUME_FILE = 1000;
	private static final int FUTURE_CADUCITY_REPORT_POSITION_RESUME_FILE = 2000;

	private RandomAccessFile resumeFile;

	@Getter
	private String entry;

	@Getter
	private String subEntry;

	@PostConstruct
	public void init() throws Exception {

		resumeFile = new RandomAccessFile(baseResumeFile, "rws");
		entry = getEntry(ENTRY_TYPE.ENTRY);
		subEntry = getEntry(ENTRY_TYPE.SUB_ENTRY);

		if (entry == null)
			logger.info("No restoring point was found");
		else {
			logger.info("Restoring from " + entry);
			logger.info("Restoring from " + subEntry);
		}
	}

	public boolean processEntry(IScanFileEntry entry) {

		if (this.entry == null) {
			updateEntry(ENTRY_TYPE.ENTRY, entry.getEntry());
			return true;
		}

		if (this.entry.equalsIgnoreCase(entry.getEntry()) == false)
			return false;

		this.entry = null;
		return true;

	}

	public boolean processSubEntry(String subEntry) {

		if (this.subEntry == null) {
			updateEntry(ENTRY_TYPE.SUB_ENTRY, subEntry);
			return true;
		}

		if (this.subEntry.equalsIgnoreCase(subEntry) == false)
			return false;

		this.subEntry = null;
		return true;

	}

	public void updateSubEntry(String value) {
		updateEntry(ENTRY_TYPE.SUB_ENTRY, value);
	}

	public List<String> restoreOrSaveReport(ENTRY_TYPE entry, List<String> reportNames) {

		String reportNamesStr = getEntry(entry);
		if (reportNamesStr == null) {
			// JDK 1.8
			// updateEntry(entry, String.join(",", reportNames));
			updateEntry(entry, join(",", reportNames));
			return null;
		} else {
			return Arrays.asList(reportNamesStr.split(","));
		}
	}

	private String addTerminator(String value) {
		return value + "\n";
	}

	private String join(String delimiter, List<String> list) {
		String joinedString = "";
		for (String l : list) {
			joinedString += l + delimiter;
		}

		return joinedString.substring(0, joinedString.length() - 1);
	}

	private void updateEntry(ENTRY_TYPE entry, String value) {

		try {
			switch (entry) {
			case ENTRY:
				resumeFile.seek(ENTRY_POSITION_RESUME_FILE);
				resumeFile.writeBytes(addTerminator(value));
				break;
			case SUB_ENTRY:
				resumeFile.seek(SUB_ENTRY_POSITION_RESUME_FILE);
				resumeFile.writeBytes(addTerminator(value));
				break;
			case FULL_CSV_REPORT:
				resumeFile.seek(FULL_CSV_REPORT_POSITION_RESUME_FILE);
				resumeFile.writeBytes(addTerminator(value));
				break;
			case FUTURE_CADUCITY_REPORT:
				resumeFile.seek(FUTURE_CADUCITY_REPORT_POSITION_RESUME_FILE);
				resumeFile.writeBytes(addTerminator(value));
				break;
			}

		} catch (Exception exc) {
			System.out.println(">>" + exc.toString());
		}
	}

	private String getEntry(ENTRY_TYPE entry) {

		try {
			switch (entry) {
			case ENTRY:
				resumeFile.seek(ENTRY_POSITION_RESUME_FILE);
				return resumeFile.readLine();
			case SUB_ENTRY:
				resumeFile.seek(SUB_ENTRY_POSITION_RESUME_FILE);
				return resumeFile.readLine();
			case FULL_CSV_REPORT:
				resumeFile.seek(FULL_CSV_REPORT_POSITION_RESUME_FILE);
				return resumeFile.readLine();
			case FUTURE_CADUCITY_REPORT:
				resumeFile.seek(FUTURE_CADUCITY_REPORT_POSITION_RESUME_FILE);
				return resumeFile.readLine();
			}

		} catch (Exception exc) {
			System.out.println(">>" + exc.toString());
		}

		return null;
	}

	public void close() {

		try {
			resumeFile.close();
			// JDK 1.7
			// Files.deleteIfExists(new File(baseResumeFile).toPath());
			new File(baseResumeFile).delete();
		} catch (Exception exc) {
		}
	}

}
