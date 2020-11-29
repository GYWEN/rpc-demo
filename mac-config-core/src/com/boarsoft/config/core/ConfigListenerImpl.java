package com.boarsoft.config.core;

import java.util.Map;

import com.boarsoft.config.core.ConfigListener;
import com.boarsoft.config.core.Configable;

/**
 * 配置文件同步后执行动作
 * 
 * @author Mac_J
 *
 */
public class ConfigListenerImpl implements ConfigListener {
	/** */
	protected String path;
	/** */
	protected Map<String, Configable> configableMap;

	@Override
	public boolean onReady(String code) {
		if (configableMap.containsKey(code)) {
			configableMap.get(code).config();
		}
		return true;
	}

	@Override
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Map<String, Configable> getConfigableMap() {
		return configableMap;
	}

	public void setConfigableMap(Map<String, Configable> configableMap) {
		this.configableMap = configableMap;
	}
}