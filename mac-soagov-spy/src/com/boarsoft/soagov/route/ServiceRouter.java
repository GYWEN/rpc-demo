package com.boarsoft.soagov.route;

public interface ServiceRouter {
	/**
	 * 根据服务编号和服务消费者版本获取对应服务提供者的某个具体实例的地址
	 * 
	 * @param sc
	 *            serviceCode
	 * @param cv
	 *            consumerVersion
	 * @return
	 */
	String getProvider(String sc, String cv);

	/**
	 * 从指定服务的所有提供者中挑选一个
	 * 
	 * @param pv
	 *            serviceCode - providerVersion
	 * @return
	 */
	String select1(String pv);

	/**
	 * 从指定服务的所有提供者中挑选一个
	 * 
	 * @param sc
	 *            serviceCode
	 * @param pv
	 *            providerVersion
	 * @return
	 */
	String select1(String sc, String pv);
}
