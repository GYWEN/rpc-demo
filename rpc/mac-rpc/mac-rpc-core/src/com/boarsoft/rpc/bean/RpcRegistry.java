package com.boarsoft.rpc.bean;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.boarsoft.common.bean.InetConfig;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.core.RpcCore;

public class RpcRegistry implements Serializable {
	private static final long serialVersionUID = 1262809103083863664L;

	protected Date version;
	/** node addr */
	protected String key;
	/** node tag */
	protected int tag;
	/** 放附加属性，如：http.port */
	protected Properties meta = new Properties();
	/** k: refereceId */
	protected Map<String, RpcReferenceConfig> referenceMap = new HashMap<String, RpcReferenceConfig>();
	/** */
	protected Map<String, RpcServiceConfig> serviceMap = new HashMap<String, RpcServiceConfig>();

	public RpcRegistry() {
		this.key = InetConfig.LOCAL_ADDR;
		this.tag = RpcCore.getTag();
		this.version = new Date();
		this.meta.putAll(RpcConfig.getProperties());
	}

	@Override
	public String toString() {
		return key;
	}

	public int getServiceMethodId(String serviceKey, Method method) {
		RpcServiceConfig sc = this.serviceMap.get(serviceKey);
		RpcMethodConfig mc = sc.getMethodConfig(method);
		return mc.getRelativeId();
	}

	public Map<String, RpcReferenceConfig> getReferenceMap() {
		return this.referenceMap;
	}

	public void setReferenceMap(Map<String, RpcReferenceConfig> referenceMap) {
		this.referenceMap = referenceMap;
	}

	public Map<String, RpcServiceConfig> getServiceMap() {
		return this.serviceMap;
	}

	public void setServiceMap(Map<String, RpcServiceConfig> serviceMap) {
		this.serviceMap = serviceMap;
	}

	public Date getVersion() {
		return this.version;
	}

	public void setVersion(Date version) {
		this.version = version;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}

	public Properties getMeta() {
		return meta;
	}

	public void setMeta(Properties meta) {
		this.meta = meta;
	}
}
