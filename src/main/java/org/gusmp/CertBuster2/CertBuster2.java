package org.gusmp.CertBuster2;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.gusmp.CertBuster2.aspect.FileCacheAspect;
import org.gusmp.CertBuster2.beans.IScanFileEntry;
import org.gusmp.CertBuster2.service.ReportingService;
import org.gusmp.CertBuster2.service.ResumeService;
import org.gusmp.CertBuster2.service.ScanFileEntryService;
import org.gusmp.CertBuster2.service.ScanFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@EnableAutoConfiguration
@Component
@ComponentScan
public class CertBuster2 {

	private static final Logger log = LoggerFactory.getLogger(CertBuster2.class);

	@Autowired
	private ScanFileService scanFileService;

	@Autowired
	private ScanFileEntryService scanFileEntryService;

	@Autowired
	private ReportingService reportingService;

	@Autowired
	private FileCacheAspect fileCacheAspect;

	@Autowired
	private ResumeService resumeService;
	
	@Value("${certbuster2.discovery.version}")
	private String version;
	
	@Value("${certbuster2.discovery.timestamp}")
	private String timeStamp;

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(CertBuster2.class, args);

		CertBuster2 mainObj = ctx.getBean(CertBuster2.class);

		mainObj.init(args);
		log.debug("CertBuster2 exited");

	}

	public void init(String[] args) {

		log.debug("CertBuster2 " + version + " started");

		if (Security.getProvider("BC") == null) {
			Security.addProvider(new BouncyCastleProvider());
		}

		for (int i = 0; i < scanFileService.size(); i++) {
			IScanFileEntry entry = scanFileService.getEntry(i);
			log.info("Scanning " + entry.getEntry());

			if (resumeService.processEntry(entry)) {
				scanFileEntryService.processEntry(entry, reportingService);
			}
		}

		reportingService.close();
		fileCacheAspect.cleanCache();
		resumeService.close();
	}

}
