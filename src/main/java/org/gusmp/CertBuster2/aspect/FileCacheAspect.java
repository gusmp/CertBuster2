package org.gusmp.CertBuster2.aspect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.gusmp.CertBuster2.BaseRoot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FileCacheAspect extends BaseRoot {

	@Value("${certbuster2.certificate.enableCrlCache}")
	private boolean enableCache;

	private Map<String, String> cache = new HashMap<String, String>();

	@Around("execution(* org.gusmp.CertBuster2.service.connection.NativeConnectionService.getFile(..)) && args(String)")
	public Object log(ProceedingJoinPoint pjp) throws Throwable {

		if (!enableCache)
			return pjp.proceed();

		String url = (String) pjp.getArgs()[0];
		logger.info("Checking if " + url + " has already cached");

		Object ret;

		try {
			ret = readCache(cache.get(url));
			logger.debug(url + "was previously cached");
		} catch (Exception exc) {
			logger.debug("Execute method for " + url);
			ret = pjp.proceed();
			ret = setCache(url, (InputStream) ret);
		}

		logger.debug("Exiting cache");
		return ret;

	}

	public InputStream readCache(String filePath) throws Exception {
		return new FileInputStream(filePath);
	}

	public InputStream setCache(String url, InputStream inputStream) {

		try {
			File temp = File.createTempFile("certBuster2", ".crl");

			FileOutputStream fout = new FileOutputStream(temp);
			IOUtils.copy(inputStream, fout);
			fout.close();
			inputStream.close();

			inputStream = new FileInputStream(temp);

			cache.put(url, temp.getAbsolutePath());
			logger.debug("Create cache entry for " + url + " in " + temp.getAbsolutePath());

		} catch (Exception exc) {
			logger.error("Unable to create the file cache. " + exc.toString());
		}

		return inputStream;
	}

	public void cleanCache() {

		for (Entry<String, String> entry : cache.entrySet()) {
			new File(entry.getValue()).delete();
		}
	}
}
