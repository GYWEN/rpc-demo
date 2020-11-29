package com.boarsoft.soagov.config;

import java.io.Serializable;
import java.util.Map;

public interface BwConfig extends Serializable {
	/**
	 * 
	 * @return 所有限流的维度及其值
	 */
	Map<String, String> getDimMap();

	boolean isBlack();

	String getKey();

	boolean setKey(String key);
}
