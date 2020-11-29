package com.boarsoft.soagov.spy;

import com.boarsoft.soagov.config.BwConfig;
import com.boarsoft.soagov.config.SlaConfig;

/**
 * SvcSp的方法是否实现、如何实现，取决于如何使用它
 * 
 * @author Mac_J
 *
 */
public interface SvcSpy {
	/** 表示检查通过 */
	static final int CHECK_PASS = 0;
	/** 表示服务开关检查不通过 */
	static final int CHECK_OFF = 1;
	/** 表示需要模拟结果 */
	static final int CHECK_MOCK = 2;
	/** 表示黑白名单不通过 */
	static final int CHECK_BLACK = 3;
	/** 表示限流检查不通过 */
	static final int CHECK_SLA = 4;

	/**
	 * 集成所有检查项的方法）
	 * 
	 * @param input
	 * @return
	 */
	int check(Object input);

	/**
	 * 检查服务状态（开关与升降级）
	 * 
	 * @param data
	 * @return
	 */
	int checkStatus(SpyData data);

	/**
	 * 黑白名单检查
	 * 
	 * @param data
	 * @param input
	 * @return
	 */
	boolean checkBwList(SpyData data, Object input);

	/**
	 * 限流检查
	 * 
	 * @param data
	 * @param input
	 * @return
	 */
	boolean checkSla(SpyData data, Object input);

	/**
	 * 服务升级（开启）
	 * 
	 * @param serviceKey
	 * @param on
	 * @return
	 */
	boolean up(String serviceKey);

	/**
	 * 服务关闭
	 * 
	 * @param serviceKey
	 * @param mock
	 * @return
	 */
	boolean down(String serviceKey);

	/**
	 * 服务降级（关闭），调用模拟器返回假结果（如果有的话）或者直接抛出拒绝执行的异常
	 * 
	 * @param serviceKey
	 * @param mock
	 * @return
	 */
	boolean mock(String serviceKey);

	/**
	 * 服务降级，服务调用发生时，总是返回指定的结果
	 * 
	 * @param serviceKey
	 * @param mockType
	 * @param mockJson
	 * @return
	 * @throws ClassNotFoundException
	 */
	boolean mock(String serviceKey, String mockType, String mockJson) throws ClassNotFoundException;

	/**
	 * 打开全局服务开关
	 * 
	 * @return
	 */
	boolean up();

	/**
	 * 关闭全局服务开关
	 * 
	 * @return
	 */
	boolean down();

	/**
	 * 打开全局服务模拟开关，所有配置了mocker的服务将返回模拟结果，反之返回null
	 * 
	 * @return
	 */
	boolean mock();

	/**
	 * TPS限流
	 * 
	 * @param serviceKey
	 *            服务编号
	 * @param tpsLimit
	 *            TPS上限
	 * @return
	 */
	boolean limit(String serviceKey, int tpsLimit);

	/**
	 * 服务TPS限流
	 * 
	 * @param serviceKey
	 *            服务编号
	 * @param sc
	 *            SLA限流配置
	 * @param tpsLimit
	 *            TPS上限
	 * @return
	 */
	boolean limit(String serviceKey, SlaConfig sc, int tpsLimit);

	/**
	 * 服务TPS限流
	 * 
	 * @param data
	 *            service spy data
	 * @param slaCfg
	 *            SlaConfig
	 * @param tpsLimit
	 *            TPS上限
	 * @return
	 */
	boolean limit(SpyData data, SlaConfig slaCfg, int tpsLimit);

	/**
	 * 服务TPS限流
	 * 
	 * @param data
	 *            service spy data
	 * @param slaCfg
	 *            SlaConfig
	 * @param tpsLimit
	 *            TPS上限
	 * @return
	 */
	boolean limit(SpyData data, int tpsLimit);

	/**
	 * 取消某个服务的某项限流配置
	 * 
	 * @param data
	 * @param slaCfgKey
	 * @return
	 */
	boolean unlimit(SpyData data, String slaCfgKey);

	/**
	 * 取消某服务所有的限流配置
	 * 
	 * @param data
	 * @return
	 */
	boolean unlimit(SpyData data);

	/**
	 * 广播方法，返回某服务的监控数据
	 * 
	 * @param serviceKey
	 * @return SpyData
	 */
	Object getData(String serviceKey);

	/**
	 * 广播方法，返回所有服务的监控数据
	 * 
	 * @return Map<String, SpyData>
	 */
	Object getDataMap();

	/**
	 * 返回某服务的监控数据
	 * 
	 * @param key
	 * @return
	 */
	SpyData getSpyData(String key);

	/**
	 * 返回全局服务配置数据
	 * 
	 * @return
	 */
	SpyData getGlobalSpyData();

	/**
	 * 广播方法，返回全局服务配置数据
	 * 
	 * @return
	 */
	Object getGlobalData();

	/**
	 * 暴露此服务，允许修改mock返回值
	 * 
	 * @param serviceKey
	 * @param result
	 */
	void setResult(String serviceKey, Object result);

	/**
	 * 返回当前服务的模拟返回结果，但并不作为远程方法暴露
	 * 
	 * @param serviceKey
	 * @return
	 */
	Object getResult(String serviceKey);

	/**
	 * 取消某个服务的所有限流配置
	 * 
	 * @param code
	 *            service code
	 * @return
	 */
	boolean unlimit(String code);

	/**
	 * 取消某个服务的某项限流配置
	 * 
	 * @param code
	 *            service code
	 * @param key
	 *            限流配置的key
	 * @return
	 */
	boolean unlimit(String code, String key);

	/**
	 * 结果模拟接口：通过调用模拟器来返回模拟的结果
	 * 
	 * @param input
	 * @return
	 */
	Object mock(Object input);

	/**
	 * 设置某个服务的TPS限流
	 * 
	 * @param key
	 *            服务编号
	 * @param b
	 * @return
	 */
	boolean setSlaConfigOn(String key, boolean b);

	/**
	 * 设置某个服务的黑白名单控制
	 * 
	 * @param key
	 *            服务编号
	 * @param b
	 * @return
	 */
	boolean setBwConfigOn(String key, boolean b);

	/**
	 * 返回某个服务是否开启了限流
	 * 
	 * @param key
	 *            服务编号
	 * @return
	 */
	boolean isSlaConfigOn(String key);

	/**
	 * 返回某个服务是否开启了全局黑白名单控制
	 * 
	 * @param key
	 *            服务编号
	 * @return
	 */
	boolean isBwConfigOn(String key);

	/**
	 * 返回是否开启了全局黑白名单控制
	 * 
	 * @return
	 */
	boolean isBwConfigOn();

	/**
	 * 返回是否开启了全局限流
	 * 
	 * @return
	 */
	boolean isSlaConfigOn();

	/**
	 * 设置全局黑白名单开关
	 * 
	 * @param b
	 * @return
	 */
	boolean setBwConfigOn(boolean b);

	/**
	 * 设置全局限流配置开关
	 * 
	 * @param b
	 * @return
	 */
	boolean setSlaConfigOn(boolean b);

	/**
	 * 修改针对些SvcSpy的全局配置，添加限流配置项
	 * 
	 * @param sc
	 * @param tpsLimit
	 * @return
	 */
	boolean limit(SlaConfig sc, int tpsLimit);

	/**
	 * 修改针对些SvcSpy的全局配置，设置全局TPS上限
	 * 
	 * @param tpsLimit
	 * @return
	 */
	boolean limit(int tpsLimit);

	/**
	 * 取消全局限流配置
	 * 
	 * @param data
	 * @return
	 */
	boolean unlimit();

	/**
	 * 添加黑白名单项
	 * 
	 * @param serviceKey
	 * @param bwCfg
	 * @return
	 */
	boolean addBwConfig(String serviceKey, BwConfig bwCfg);

	/**
	 * 删除黑白名单项
	 * 
	 * @param serviceKey
	 * @param bwCfg
	 * @return
	 */
	boolean delBwConfig(String serviceKey, BwConfig bwCfg);

	/**
	 * 删除限流配置
	 * 
	 * @param serviceKey
	 * @param slaCfgKey
	 * @return
	 */
	boolean delSlaConfig(String serviceKey, String slaCfgKey);

	/**
	 * 
	 * @param addr
	 * @param code
	 */
	Object disableProvider(String addr, String code);

	/**
	 * 
	 * @param addr
	 * @param key
	 * @return
	 */
	Object enableProvider(String addr, String key);
}