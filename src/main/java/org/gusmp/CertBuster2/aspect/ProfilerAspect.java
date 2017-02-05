package org.gusmp.CertBuster2.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.gusmp.CertBuster2.BaseRoot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ProfilerAspect extends BaseRoot {

	@Value("${certbuster2.profiler.getCertificate}")
	Boolean getCertificateProfilerEnabled;

	@Around("execution(* org.gusmp.CertBuster2.service.connection.*.getCertificate(..)) && args(String,Integer)")
	public Object log(ProceedingJoinPoint pjp) throws Throwable {

		long start = System.nanoTime();
		Object ret = pjp.proceed();
		long end = System.nanoTime();

		if (getCertificateProfilerEnabled == true) {
			String url = (String) pjp.getArgs()[0];
			Integer port = (Integer) pjp.getArgs()[1];
			logger.debug("Time elapsed for " + url + ":" + port + " is: " + (end - start) / 1000000000.0);
		}

		return ret;
	}
}
