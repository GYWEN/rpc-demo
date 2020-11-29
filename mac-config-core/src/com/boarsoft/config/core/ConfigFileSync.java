package com.boarsoft.config.core;

/**
 * 配置文件同步接口，供配置中心调用<br>
 * 传递的配置文件可能是二进制的，也可能是压缩包
 * 
 * @author Mac_J
 *
 */
public interface ConfigFileSync {

	/**
	 * 广播方法，同步指定的配置文件（文本文件）
	 * 
	 * @param bytes
	 * @param code
	 * @return
	 */
	Object syncAll(byte[] bytes, String code);

	/**
	 * 将配置文件同步给指定节点
	 * 
	 * @param bytes
	 * @param code
	 * @return
	 */
	String syncOne(byte[] bytes, String code);
}
