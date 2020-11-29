package com.boarsoft.soagov.spy;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.util.JsonUtil;
import com.boarsoft.soagov.config.BwConfig;
import com.boarsoft.soagov.config.SlaConfig;
import com.boarsoft.soagov.config.SvcConfig;
import com.boarsoft.soagov.svc.SvcReqReader;

/**
 * 被注入给RpcContext，在业务方法执行前调用check进行访问控制
 * 
 * @author Mac_J
 *
 */
public class SimpleSvcSpyImpl implements SvcSpy, Runnable {
	public static final Logger log = LoggerFactory.getLogger(SimpleSvcSpyImpl.class);

	/** 面向当前节点的全局服务控制 */
	protected SpyData globalData = new SpyData("*");
	/** 由Spring注入，并由ComboCache负责初始化，管控中心通过RPC调用Cache接口实现缓存更新，key为服务编号 */
	protected Map<String, SpyData> dataMap = new ConcurrentHashMap<String, SpyData>();
	/** */
	protected SvcReqReader reqReader;
	/** */
	protected ScheduledExecutorService scheduler;

	@PostConstruct
	public void init() {
		scheduler.scheduleWithFixedDelay(this, 1, 1, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		globalData.resetTpsBuckets();
		for (SpyData d : dataMap.values()) {
			d.resetTpsBuckets();// 每隔一秒重新装满令牌桶
		}
	}

	public void setScheduler(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public int check(Object input) {
		// 检查全局服务开关
		if (this.checkStatus(globalData) == SvcConfig.STATUS_DISABLE) {
			log.debug("Global servcie control: status is off");
			return CHECK_OFF;
		}
		// 后面的检查都依赖reqReader
		if (reqReader == null) {
			return CHECK_PASS;
		}
		// 检查全局黑白名单
		if (!this.checkBwList(globalData, input)) {
			log.debug("Global servcie control: Back/White list reject");
			return CHECK_BLACK;
		}
		// 检查全局SLA（限流）
		if (!this.checkSla(globalData, input)) {
			log.debug("Global servcie control: SLA control reject");
			return CHECK_SLA;
		}
		// 获取当前服务的编号
		// String code = gsc.getCode(input);
		String key = reqReader.getCode(input);
		// 根据服务编码取得当前服务的配置
		SpyData data = this.getSpyData(key);
		// 检查当前服务的服务开关
		int status = this.checkStatus(data);
		if (status == SvcConfig.STATUS_DISABLE) {
			log.debug("Self servcie control: status is off");
			return CHECK_OFF;
		} else if (status == SvcConfig.STATUS_MOCKING) {
			return CHECK_MOCK;
		}
		// 检查当前服务的黑白名单
		if (!this.checkBwList(data, input)) {
			log.debug("Self servcie control: Back/White list reject");
			return CHECK_BLACK;
		}
		// 检查当前服务的SLA（限流）
		if (!this.checkSla(data, input)) {
			log.debug("Self servcie control: SLA control reject");
			return CHECK_SLA;
		}
		return CHECK_PASS;
	}

	@Override
	public int checkStatus(SpyData data) {
		return data.getSvcConfig().getStatus();
	}

	@Override
	public boolean checkBwList(SpyData sd, Object input) {
		if (!sd.isBwConfigOn()) {
			return true;
		}
		SvcConfig sc = sd.getSvcConfig();
		Queue<BwConfig> bwCfgQu = sc.getBwConfigs();
		if (bwCfgQu.isEmpty()) {
			return true;// 如果没有限流配置，直接放行
		}
		// 逐条检查当前请求的参数是否满足限流配置条件
		for (BwConfig a : bwCfgQu) {
			// request parameter value
			Map<String, String> dm = a.getDimMap();
			if (dm.isEmpty()) {
				continue;
			}
			StringBuilder sb = new StringBuilder();
			for (String k : dm.keySet()) {
				Object v = reqReader.pick(input, k);
				sb.append(k).append("=").append(v).append("&");
			}
			sb.setLength(sb.length() - 1);
			if (a.getKey().equals(sb.toString())) {
				// 如果维度值能匹配，检查是黑还是白
				// if (a.isBlack()) {
				// log.warn("Service invoking {} hit black list on {}",
				// sd.getKey(), a.getKey());
				// } else {
				// log.warn("Service invoking {} hit black list on {}",
				// sd.getKey(), a.getKey());
				// }
				return !a.isBlack(); // 黑不通过，白直接通过
			}
		}
		return true;
	}

	@Override
	public boolean checkSla(SpyData sd, Object input) {
		if (!sd.isSlaConfigOn()) {
			return true;
		}
		SvcConfig sc = sd.getSvcConfig();
		Queue<SlaConfig> slaCfgQu = sc.getSlaConfigs();
		// 逐条检查当前请求的参数是否满足限流配置条件
		for (SlaConfig a : slaCfgQu) {
			// request parameter value
			Map<String, String> dm = a.getDimMap();
			if (dm.isEmpty()) {
				continue;
			}
			StringBuilder sb = new StringBuilder();
			for (String k : dm.keySet()) {
				Object v = reqReader.pick(input, k);
				sb.append(k).append("=").append(v).append("&");
			}
			sb.setLength(sb.length() - 1);
			String k = a.getKey();
			if (k.equals(sb.toString())) {
				// 如果维度值能匹配，检查TpsBucket
				if (!sd.getTpsBucket(k).get()) {
					// log.warn("Service invoking {] reach TPS limit on {}",
					// sd.getKey(), k);
					return false;
				}
			}
		}
		// 最后检查主限流配置
		if (!sd.getTpsBucket().get()) {
			// log.warn("Service invoking {] reach TPS limit", sd.getKey());
			return false;
		}
		return true;
	}

	@Override
	public boolean limit(SpyData data, int tpsLimit) {
		log.warn("Limit service {} TPS to {}", data.getKey(), tpsLimit);
		TpsBucket tb = data.getTpsBucket();
		if (tpsLimit < 0) {
			tpsLimit = Integer.MAX_VALUE;
		}
		tb.limit(tpsLimit);
		return true;
	}

	@Override
	public boolean limit(SpyData data, SlaConfig slaCfg, int tpsLimit) {
		log.warn("Limit service {} TPS to {}", data.getKey(), tpsLimit);
		String ck = slaCfg.getKey();
		// 先创建对应的TpsBucket
		TpsBucket tb = data.getTpsBucket(ck);
		tb.limit(tpsLimit);
		// 再添加到限流配置集合，以让它生效
		data.getSvcConfig().addSlaConfig(slaCfg);
		return true;
	}

	@Override
	public boolean unlimit(SpyData data, String slaCfgKey) {
		log.warn("Remove service {} limit item {}", data.getKey(), slaCfgKey);
		data.removeLimit(slaCfgKey);
		return true;
	}

	@Override
	public boolean unlimit(SpyData data) {
		log.warn("Remove service {} global limit item", data.getKey());
		TpsBucket tb = data.getTpsBucket();
		tb.limit(Integer.MAX_VALUE);
		// data.clearLimit();
		return true;
	}

	@Override
	public boolean unlimit() {
		log.warn("Unlimit global service config");
		TpsBucket tb = globalData.getTpsBucket();
		tb.limit(Integer.MAX_VALUE);
		// data.clearLimit();
		return true;
	}

	@Override
	public boolean limit(String serviceKey, int tpsLimit) {
		SpyData sd = this.getSpyData(serviceKey);
		return this.limit(sd, tpsLimit);
	}

	@Override
	public boolean limit(int tpsLimit) {
		return this.limit(globalData, tpsLimit);
	}

	@Override
	public boolean limit(SlaConfig sc, int tpsLimit) {
		return this.limit(globalData, sc, tpsLimit);
	}

	@Override
	public boolean limit(String serviceKey, SlaConfig sc, int tpsLimit) {
		SpyData sd = this.getSpyData(serviceKey);
		return this.limit(sd, sc, tpsLimit);
	}

	@Override
	public boolean unlimit(String serviceKey) {
		SpyData sd = this.getSpyData(serviceKey);
		return this.unlimit(sd);
	}

	@Override
	public boolean unlimit(String serviceKey, String slaCfgKey) {
		SpyData sd = this.getSpyData(serviceKey);
		return this.unlimit(sd, slaCfgKey);
	}

	@Override
	public boolean up(String key) {
		SpyData sd = this.getSpyData(key);
		return this.up(sd);
	}

	@Override
	public boolean down(String key) {
		SpyData sd = this.getSpyData(key);
		return this.down(sd);
	}

	@Override
	public boolean up() {
		return this.up(globalData);
	}

	@Override
	public boolean down() {
		return this.down(globalData);
	}

	protected boolean up(SpyData sd) {
		log.warn("Up service {}", sd.getKey());
		SvcConfig sc = sd.getSvcConfig();
		sc.setStatus(SvcConfig.STATUS_ENABLE);
		return true;
	}

	protected boolean down(SpyData sd) {
		log.warn("Down service {}", sd.getKey());
		SvcConfig sc = sd.getSvcConfig();
		// 要求mock但未指定mock返回值时，不设置resultMap
		sc.setStatus(SvcConfig.STATUS_DISABLE);
		return true;
	}

	public Object disableProvider(String addr, String key) {
		log.warn("Disable provider not supported");
		return false;
	}

	public Object enableProvider(String addr, String key) {
		log.warn("Enable provider not supported");
		return false;
	}

	@Override
	public void setResult(String key, Object result) {
		log.warn("Mock result of  service {} with {}", key, result);
		SpyData sd = this.getSpyData(key);
		sd.setResult(result);
	}

	@Override
	public Object getResult(String key) {
		SpyData sd = this.getSpyData(key);
		return sd.getResult();
	}

	@Override
	public boolean mock(String key) {
		SpyData sd = this.getSpyData(key);
		return this.mock(sd);
	}

	@Override
	public boolean mock(String key, String mockType, String mockJson) throws ClassNotFoundException {
		SpyData sd = this.getSpyData(key);
		return this.mock(sd, mockType, mockJson);
	}

	@Override
	public boolean mock() {
		return this.mock(globalData);
	}

	protected boolean mock(SpyData sd) {
		log.warn("Mock service {}", sd.getKey());
		SvcConfig sc = sd.getSvcConfig();
		// 要求mock但未指定mock返回值时，不设置resultMap
		sc.setStatus(SvcConfig.STATUS_MOCKING);
		return true;
	}

	protected boolean mock(SpyData sd, String mockType, String mockJson) throws ClassNotFoundException {
		log.warn("Mock service {} with {}/{}", sd.getKey(), mockType, mockJson);
		SvcConfig sc = sd.getSvcConfig();
		sc.setStatus(SvcConfig.STATUS_MOCKING);
		Object o = null;
		switch (mockType) {
		case "java.lang.String":
			o = mockJson;
			break;
		case "byte":
		case "java.lang.Byte":
			o = Byte.parseByte(mockJson);
			break;
		case "boolean":
		case "java.lang.Boolean":
			o = Boolean.parseBoolean(mockJson);
			break;
		case "short":
		case "java.lang.Short":
			o = Short.parseShort(mockJson);
			break;
		case "long":
		case "java.lang.Long":
			o = Long.parseLong(mockJson);
			break;
		case "java.lang.Integer":
			o = Integer.parseInt(mockJson);
			break;
		case "float":
		case "java.lang.Float":
			o = Float.parseFloat(mockJson);
			break;
		case "double":
		case "java.lang.Double":
			o = Double.parseDouble(mockJson);
			break;
		default:
			o = JsonUtil.parseObject(mockJson, Class.forName(mockType));
			break;
		}
		// 有配置mocker就调mocker，否则返回指定的假值
		this.setResult(sd.getKey(), o);
		return true;
	}

	@Override
	public Object mock(Object input) {
		return null;
	}

	@Override
	public boolean addBwConfig(String serviceKey, BwConfig bwCfg) {
		log.warn("Add {} into {} list of service {}", bwCfg, serviceKey);
		SpyData data = this.getSpyData(serviceKey);
		SvcConfig sc = data.getSvcConfig();
		sc.addBwConfig(bwCfg);
		return true;
	}

	@Override
	public boolean delBwConfig(String serviceKey, BwConfig bwCfg) {
		log.warn("Delete {} from {} list of service {}", bwCfg, serviceKey);
		SpyData data = this.getSpyData(serviceKey);
		SvcConfig sc = data.getSvcConfig();
		sc.delBwConfig(bwCfg);
		return true;
	}

	@Override
	public boolean delSlaConfig(String serviceKey, String slaCfgKey) {
		log.warn("Delete SLA item {} of service {}", slaCfgKey, serviceKey);
		SpyData data = this.getSpyData(serviceKey);
		SvcConfig sc = data.getSvcConfig();
		sc.delSlaConfig(slaCfgKey);
		return true;
	}

	public SvcReqReader getReqReader() {
		return reqReader;
	}

	public void setReqReader(SvcReqReader reqReader) {
		this.reqReader = reqReader;
	}

	@Override
	public Object getDataMap() {
		return dataMap;
	}

	@Override
	public SpyData getGlobalSpyData() {
		return globalData;
	}

	@Override
	public SpyData getSpyData(String key) {
		SpyData sd = dataMap.get(key);
		if (sd == null) {
			synchronized (dataMap) {
				sd = dataMap.get(key);
				if (sd == null) {
					sd = new SpyData(key);
					dataMap.put(key, sd);
				}
			}
		}
		return sd;
	}

	@Override
	public Object getGlobalData() {
		return globalData;
	}

	@Override
	public Object getData(String key) {
		return this.getSpyData(key);
	}

	@Override
	public boolean setSlaConfigOn(String key, boolean b) {
		log.warn("Set SLA limit {} for service {}", b ? "on" : "off", key);
		SpyData sd = this.getSpyData(key);
		sd.setSlaConfigOn(b);
		return true;
	}

	@Override
	public boolean setBwConfigOn(String key, boolean b) {
		log.warn("Set black/white list {} for service {}", b ? "on" : "off", key);
		SpyData sd = this.getSpyData(key);
		sd.setBwConfigOn(b);
		return true;
	}

	@Override
	public boolean isSlaConfigOn(String key) {
		SpyData sd = this.getSpyData(key);
		return sd.isSlaConfigOn();
	}

	@Override
	public boolean isBwConfigOn(String key) {
		SpyData sd = this.getSpyData(key);
		return sd.isBwConfigOn();
	}

	@Override
	public boolean setSlaConfigOn(boolean b) {
		log.warn("Set global SLA limit {}", b ? "on" : "off");
		globalData.setSlaConfigOn(b);
		return true;
	}

	@Override
	public boolean setBwConfigOn(boolean b) {
		log.warn("Set global black/white list {}", b ? "on" : "off");
		globalData.setBwConfigOn(b);
		return true;
	}

	@Override
	public boolean isSlaConfigOn() {
		return globalData.isSlaConfigOn();
	}

	@Override
	public boolean isBwConfigOn() {
		return globalData.isBwConfigOn();
	}
}
