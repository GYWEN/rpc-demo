package com.boarsoft.config.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.util.StreamUtil;

/**
 * ConfigVarSync接口的默认实现，使用properties文件存储配置
 * 
 * @author Mac_J
 *
 */
public class ConfigVarSyncImpl implements ConfigVarSync {
	private static final Logger log = LoggerFactory.getLogger(ConfigVarSyncImpl.class);

	/** */
	protected Map<String, Properties> propMap = new HashMap<String, Properties>();
	/** */
	protected Map<String, ConfigListener> configListenerMap;

	protected ClassLoader classLoader = this.getClass().getClassLoader();

	@PostConstruct
	public void init() {
		for (String group : configListenerMap.keySet()) {
			Properties prop = new Properties();
			this.load(group, prop);
			propMap.put(group, prop);
		}
	}

	protected void load(String group, Properties prop) {
		ConfigListener a = configListenerMap.get(group);
		String p = classLoader.getResource(".").toString().concat(a.getPath());
		log.warn("Load config group {} from {}", group, p);
		InputStream fis = null;
		try {
			fis = classLoader.getResourceAsStream(a.getPath());
			prop.load(fis);
		} catch (IOException e) {
			log.error("Error on load properties {}", p, e);
		} finally {
			StreamUtil.close(fis);
		}
	}

	protected boolean save(String group, Properties prop, String comment) {
		ConfigListener cl = configListenerMap.get(group);
		String p = classLoader.getResource(".").getFile();
		p = p.concat(cl.getPath());
		log.warn("Save config group {} to {}", group, p);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(p));
			prop.store(fos, comment);
			return cl.onReady(group);
		} catch (IOException e) {
			log.error("Error on load properties {}", p, e);
			return false;
		} finally {
			StreamUtil.close(fos);
		}
	}

	@Override
	public Object get(String group, String key) {
		Properties prop = propMap.get(group);
		if (prop == null) {
			return null;
		}
		return prop.get(key);
	}

	@Override
	public boolean put(String group, String key, Object value) {
		Properties prop = propMap.get(group);
		if (prop == null) {
			return false;
		}
		// 重新读取，确保内存中的值与文件中的值一致再改
		this.load(group, prop);
		prop.put(key, value);
		return this.save(group, prop, String.format("put %s = %s", key, value));
	}

	@Override
	public boolean delete(String group, String key) {
		Properties prop = propMap.get(group);
		if (prop == null) {
			return false;
		}
		prop.remove(key);
		return this.save(group, prop, String.format("remove %s", key));
	}

	@Override
	public Map<String, Object> get() {
		Map<String, Object> rm = new HashMap<String, Object>();
		rm.putAll(propMap);
		return rm;
	}

	public Map<String, Properties> getPropMap() {
		return propMap;
	}

	public void setPropMap(Map<String, Properties> propMap) {
		this.propMap = propMap;
	}

	public Map<String, ConfigListener> getConfigListenerMap() {
		return configListenerMap;
	}

	public void setConfigListenerMap(Map<String, ConfigListener> configListenerMap) {
		this.configListenerMap = configListenerMap;
	}
}