package com.boarsoft.config.core;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Mac_J
 *
 */
public class SystemPropsListener implements ConfigListener {
	private static final Logger log = LoggerFactory.getLogger(SystemPropsListener.class);

	protected String path;

	@Override
	public boolean onReady(String code) {
		if (!path.endsWith(".properties")) {
			return false;
		}
		Properties p = new Properties();
		try {
			p.load(new FileReader(new File(path)));
			System.getProperties().putAll(p);
			return true;
		} catch (IOException e) {
			log.error("Error on read config file {}", path, e);
		}
		return false;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
