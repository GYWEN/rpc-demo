package com.boarsoft.config.core;

import java.util.Map;

/**
 * 内存配置项服务
 * 
 * @author Mac_J
 *
 */
public interface ConfigVarSync {
	/**
	 * 供远程配置中心调，向应用节点推送单个配置参数
	 * 
	 * @param group
	 *            参数归属的分组（配置文件名），可以为空
	 * @param key
	 *            请求的配置参数编号（key）
	 * @param value
	 *            参数值（通常是基本数据类型）
	 * @return
	 */
	boolean put(String group, String key, Object value);

	/**
	 * 供远程配置中心调，用于检视应用节点单个配置参数
	 * 
	 * @param group
	 *            参数归属的配置文件，可以为空
	 * @param key
	 *            请求的配置参数编号（key）
	 * @param value
	 *            参数值（通常是基本数据类型）
	 * @return
	 */
	Object get(String group, String key);

	/**
	 * 删除某个配置项
	 * 
	 * @param group
	 * @param key
	 * @return 
	 */
	boolean delete(String group, String key);

	/**
	 * 提取当前全部配置
	 * 
	 * @return
	 */
	Map<String, Object> get();
}
