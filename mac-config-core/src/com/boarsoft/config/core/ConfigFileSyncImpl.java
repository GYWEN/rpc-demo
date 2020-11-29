package com.boarsoft.config.core;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.util.FileUtil;

public class ConfigFileSyncImpl implements ConfigFileSync {
	private static final Logger log = LoggerFactory.getLogger(ConfigFileSyncImpl.class);

	/** */
	protected Map<String, ConfigListener> configListenerMap;
	
	protected ClassLoader classLoader = this.getClass().getClassLoader();

	@Override
	public Object syncAll(byte[] bytes, String code) {
		return this.sync(bytes, code);
	}

	@Override
	public String syncOne(byte[] bytes, String code) {
		return this.sync(bytes, code);
	}

	protected String sync(byte[] bytes, String code) {
		log.info("Recevied new config file {}", code);
		if (configListenerMap == null) {
			return "no_config";
		}
		ConfigListener cl = configListenerMap.get(code);
		if (cl == null) {
			return "not_found";// 找不到合适的适配器表示不做处理，仍然返回true
		}
		if (this.save(cl.getPath(), bytes)) {
			if (cl.onReady(code)) {
				return "success";
			}
			return "reconfig_failed";
		}
		return "save_failed";
	}

	protected boolean save(String path, byte[] bytes) {
		String fp = classLoader.getResource(".").getFile().concat(path);
		log.warn("Save config file to {}", fp);
		try {
			File f = new File(fp);
			FileUtil.makePath(f.getParent());
			FileUtil.writeBytes(f, bytes);
			return true;
		} catch (Exception e) {
			log.error("Error on write config file {}", fp, e);
			return false;
		}
	}

	public Map<String, ConfigListener> getConfigListenerMap() {
		return configListenerMap;
	}

	public void setConfigListenerMap(Map<String, ConfigListener> configListenerMap) {
		this.configListenerMap = configListenerMap;
	}
}