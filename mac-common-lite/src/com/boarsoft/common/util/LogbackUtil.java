package com.boarsoft.common.util;

import java.io.InputStream;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class LogbackUtil {
	public static void initLogback(Class<?> clazz, String fileName)
			throws JoranException {
		InputStream is = null;
		try {
			is = clazz.getClassLoader().getResourceAsStream(fileName);
			LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
			lc.reset();
			JoranConfigurator jc = new JoranConfigurator();
			jc.setContext(lc);
			jc.doConfigure(is); // loads logback file
		} finally {
			StreamUtil.close(is);
		}
	}
}
