package com.boarsoft.config.core;

public interface ConfigListener {
	/**
	 * 当文件被成功下载或更新时触发此事件
	 * 
	 * @param code
	 * @return
	 */
	boolean onReady(String code);

	/**
	 * 获取配置文件保存路径
	 * 
	 * @return
	 */
	String getPath();
}
