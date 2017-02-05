package org.gusmp.CertBuster2.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.beans.IScanFileEntry;
import org.gusmp.CertBuster2.reporter.FullCsvReporter;
import org.gusmp.CertBuster2.reporter.FutureCaducityCsvReporter;
import org.gusmp.CertBuster2.reporter.IReporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReportingService extends BaseService {

	@Value("${certbuster2.reporting.fullCsv.enabled}")
	private Boolean fullCsvEnabled;

	@Value("${certbuster2.reporting.futureCaducityCsv.enabled}")
	private Boolean futureCaducityCsv;

	@Autowired
	private FullCsvReporter fullCsvReporter;

	@Autowired
	private FutureCaducityCsvReporter futureCaducityCsvReporter;

	private List<IReporter> reporterEnabledList;

	@PostConstruct
	public void init() throws Exception {

		reporterEnabledList = new ArrayList<IReporter>();
		if (fullCsvEnabled) {
			fullCsvReporter.init();
			reporterEnabledList.add(fullCsvReporter);
		}

		if (futureCaducityCsv) {
			futureCaducityCsvReporter.init();
			reporterEnabledList.add(futureCaducityCsvReporter);
		}

		if (reporterEnabledList.size() == 0)
			logger.info("No reporters were enabled!!!");
	}

	public void reportEntry(List<CertificateInfo> certificateInfoList, String object, IScanFileEntry entry) {

		for (IReporter r : reporterEnabledList) {
			r.reportEntry(certificateInfoList, object, entry);
		}
	}

	public void reportEntry(Exception exception, String object, IScanFileEntry entry) {

		for (IReporter r : reporterEnabledList) {
			r.reportEntry(exception, object, entry);
		}
	}

	public void close() {
		for (IReporter r : reporterEnabledList) {
			r.close();
		}
	}
}
