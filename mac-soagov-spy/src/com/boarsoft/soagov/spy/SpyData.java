package com.boarsoft.soagov.spy;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.boarsoft.soagov.config.SimpleSvcConfig;
import com.boarsoft.soagov.config.SvcConfig;

/**
 * 一个具体的服务（某接口中的某个方法）对应一个SpyData
 * 
 * @author Mac_J
 *
 */
public class SpyData implements Serializable {
	private static final long serialVersionUID = -388960416219517561L;

	protected String key;
	/** */
	protected SvcConfig svcConfig = new SimpleSvcConfig();
	/** 全局限流配置项 */
	protected TpsBucket tpsBucket = new TpsBucket(Integer.MAX_VALUE);
	/** key: 指定的维度值的组合 */
	protected Map<String, TpsBucket> tpsBucketMap = new ConcurrentHashMap<String, TpsBucket>();
	/** */
	protected transient Object result;

	public SpyData() {
	}

	public SpyData(String key) {
		this.key = key;
	}

	/**
	 * 
	 * 
	 * @param key
	 *            SlaConfig的Key，也是TpsBucket的Key
	 */
	public void removeLimit(String key) {
		svcConfig.delSlaConfig(key);
		this.tpsBucketMap.remove(key);
	}

	public void clearLimit() {
		svcConfig.clearSlaConfigs();
		this.tpsBucketMap.clear();
	}

	/**
	 * 取得与限流维度值组合的TpsBucket
	 * 
	 * @param key
	 *            限流维度值的组合
	 * @return
	 */
	public TpsBucket getTpsBucket(String key) {
		TpsBucket tb = tpsBucketMap.get(key);
		if (tb == null) {
			synchronized (tpsBucketMap) {
				tb = tpsBucketMap.get(key);
				if (tb == null) {
					tb = new TpsBucket(Integer.MAX_VALUE);
					tpsBucketMap.put(key, tb);
				}
			}
		}
		return tb;
	}

	public void resetTpsBuckets() {
		tpsBucket.reset();
		for (TpsBucket b : tpsBucketMap.values()) {
			b.reset();
		}
	}

	public Map<String, TpsBucket> getTpsBucketMap() {
		return tpsBucketMap;
	}

	public void setTpsBucketMap(Map<String, TpsBucket> tpsBucketMap) {
		this.tpsBucketMap = tpsBucketMap;
	}

	public SvcConfig getSvcConfig() {
		return svcConfig;
	}

	public void setSvcConfig(SvcConfig svcConfig) {
		this.svcConfig = svcConfig;
	}

	/**
	 * 返回公共的TpsBucket
	 * 
	 * @return
	 */
	public TpsBucket getTpsBucket() {
		return tpsBucket;
	}

	public int getStatus() {
		return svcConfig.getStatus();
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public Object getResult() {
		return this.result;
	}

	public String getKey() {
		return key;
	}

	public void setSlaConfigOn(boolean b) {
		svcConfig.setSlaConfigOn(b);
	}

	public void setBwConfigOn(boolean b) {
		svcConfig.setBwConfigOn(b);
	}

	public boolean isSlaConfigOn() {
		return svcConfig.isSlaConfigOn();
	}

	public boolean isBwConfigOn() {
		return svcConfig.isBwConfigOn();
	}
}
