package org.gusmp.CertBuster2.reporter;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
public class FutureCaducityCsvReporter extends BaseReporter implements IReporter {

	@Value("${certbuster2.reporting.futureCaducityCsv.baseReportFile}")
	private String baseReportFile;

	@Value("#{'${certbuster2.reporting.futureCaducityCsv.periods}'.split(',')}")
	private List<Integer> periods;

	@Value("${certbuster2.reporting.futureCaducityCsv.sendReportByEmail}")
	private boolean sendReportByEmail;

	@Autowired
	private MailService mailService;

	@Autowired
	private ResumeService resumeService;

	private boolean enabled;

	private Map<Integer, ICsvBeanWriter> beanWriterMap;
	private List<String> reportsNameList;

	private SimpleDateFormat sdfYearFirst = new SimpleDateFormat("yyyy-MM-dd_hh_mm_ss");

	@Override
	public void init() throws Exception {

		if ((periods.size() == 0) || ((periods.size() == 1) && (periods.get(0) == null))) {
			logger.info("FutureCaducityCsvReporter has not defined any period. Nothing to do!");
			enabled = false;
			return;

		}

		enabled = true;
		beanWriterMap = new HashMap<Integer, ICsvBeanWriter>(periods.size());
		reportsNameList = new ArrayList<String>(periods.size());
		String reportFileName;

		for (int caducity : periods) {
			int index = baseReportFile.lastIndexOf(".");
			if (index > 0) {
				reportFileName = new StringBuilder(baseReportFile).replace(index, index + 1,
						"-caducity_" + caducity + "-" + sdfYearFirst.format(new Date()) + ".").toString();
			} else {
				reportFileName = baseReportFile + "-caducity_" + caducity + "-" + sdfYearFirst.format(new Date()) + ".csv";
			}

			reportsNameList.add(reportFileName);
		}

		List<String> storedReportFileNameList = resumeService.restoreOrSaveReport(ENTRY_TYPE.FUTURE_CADUCITY_REPORT, reportsNameList);

		int i = 0;
		for (int caducity : periods) {
			if (storedReportFileNameList != null)
				reportsNameList = storedReportFileNameList;

			reportFileName = reportsNameList.get(i);
			logger.info("Report file: " + reportFileName);

			CsvBeanWriter beanWriter;
			if (storedReportFileNameList == null) {
				beanWriter = new CsvBeanWriter(new FileWriter(reportFileName), getPreference());
				beanWriter.writeHeader(CsvLine.header);
				beanWriter.flush();
			} else {
				beanWriter = new CsvBeanWriter(new FileWriter(reportFileName, true), getPreference());
			}

			beanWriterMap.put(caducity, beanWriter);
			i++;
		}

	}

	@Override
	public void reportEntry(List<CertificateInfo> certificateInfoList, String object, IScanFileEntry entry) {

		if (enabled) {

			for (CertificateInfo certificateInfo : certificateInfoList) {

				long diff = certificateInfo.getCertificate().getNotAfter().getTime() - (new Date()).getTime();
				long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

				for (Integer caducity : periods) {

					if (days < caducity) {
						try {
							beanWriterMap.get(caducity).write(new CsvLine(certificateInfo, object, entry), CsvLine.header, CsvLine.processors);
							beanWriterMap.get(caducity).flush();
						} catch (Exception exc) {
							logger.error("Error writting entry for " + object);
						}
					}
				}
			}
		}
	}

	@Override
	public void reportEntry(Exception exception, String object, IScanFileEntry entry) {

		// Nothing to do
	}

	@Override
	public void close() {

		if (enabled) {

			for (ICsvBeanWriter beanWriter : beanWriterMap.values()) {
				try {
					beanWriter.close();
				} catch (Exception exc) {
				}
			}

			if (sendReportByEmail) {
				mailService.sendMail("Certbuster2: Future caducity report", "", reportsNameList);
			}
		}
	}

}
