package com.boarsoft.soagov.config;

import java.io.Serializable;
import java.util.Map;

public interface SlaConfig extends Serializable {
	/**
	 * 返回所要求的各维度值的组合
	 * 
	 * @return
	 */
	String getKey();

	/**
	 * 
	 * @param key
	 * @return
	 */
	boolean setKey(String key);

	/**
	 * 
	 * @return 所有限流的维度及其值
	 */
	Map<String, String> getDimMap();
}
