package org.gusmp.CertBuster2.reporter;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.beans.IScanFileEntry;
import org.gusmp.CertBuster2.service.MailService;
import org.gusmp.CertBuster2.service.ResumeService;
import org.gusmp.CertBuster2.service.ResumeService.ENTRY_TYPE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;

@Component
public class FullCsvReporter extends BaseReporter implements IReporter {

	@Value("${certbuster2.reporting.fullCsv.baseReportFile}")
	private String baseReportFile;

	@Value("${certbuster2.reporting.fullCsv.sendReportByEmail}")
	private boolean sendReportByEmail;

	@Autowired
	private MailService mailService;

	@Autowired
	private ResumeService resumeService;

	private ICsvBeanWriter beanWriter;

	private SimpleDateFormat sdfYearFirst = new SimpleDateFormat("yyyy-MM-dd_hh_mm_ss");

	@Override
	public void init() throws Exception {

		int index = baseReportFile.lastIndexOf(".");

		if (index > 0) {
			baseReportFile = new StringBuilder(baseReportFile).replace(index, index + 1, "-" + sdfYearFirst.format(new Date()) + ".").toString();
		} else {
			baseReportFile = baseReportFile + "-" + sdfYearFirst.format(new Date()) + ".csv";
		}

		List<String> reportFileNames = resumeService.restoreOrSaveReport(ENTRY_TYPE.FULL_CSV_REPORT, Arrays.asList(baseReportFile));
		if (reportFileNames == null) {

			logger.info("Report file: " + baseReportFile);
			beanWriter = new CsvBeanWriter(new FileWriter(baseReportFile), getPreference());
			beanWriter.writeHeader(CsvLine.header);
		} else {

			baseReportFile = reportFileNames.get(0);
			logger.info("Report file: " + baseReportFile);
			beanWriter = new CsvBeanWriter(new FileWriter(baseReportFile, true), getPreference());
		}
		beanWriter.flush();
	}

	@Override
	public void reportEntry(List<CertificateInfo> certificateInfoList, String object, IScanFileEntry entry) {

		for (CertificateInfo cInfo : certificateInfoList) {
			try {

				beanWriter.write(new CsvLine(cInfo, object, entry), CsvLine.header, CsvLine.processors);
				beanWriter.flush();
			} catch (Exception exc) {
				logger.error("Error writting regular entry for " + object);
				logger.error(exc.toString());
			}
		}
	}

	@Override
	public void reportEntry(Exception exception, String object, IScanFileEntry entry) {
		try {

			beanWriter.write(new CsvLine(exception, object, entry), CsvLine.header, CsvLine.processors);
			beanWriter.flush();
		} catch (Exception exc) {
			logger.error("Error writting exception entry for  " + object);
			logger.error(exc.toString());
		}
	}

	@Override
	public void close() {
		try {
			beanWriter.close();
			if (sendReportByEmail)
				mailService.sendMail("Certbuster2: Full report", "", Arrays.asList(baseReportFile));

		} catch (Exception exc) {
		}
	}

}
