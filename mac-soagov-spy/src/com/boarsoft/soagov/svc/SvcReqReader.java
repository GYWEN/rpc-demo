package com.boarsoft.soagov.svc;

public interface SvcReqReader {
	public static final String DM_IP = "ip";
	public static final String DM_ADDR = "addr";

	/**
	 * 获取某个请求参数
	 * 
	 * @param input
	 * @param key
	 * @return
	 */
	Object pick(Object input, String key);

	/**
	 * 返回当前服务的编号
	 * 
	 * @param input
	 * @return
	 */
	String getCode(Object input);

}
