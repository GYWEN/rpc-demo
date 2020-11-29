package com.boarsoft.rpc.nacos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.boarsoft.common.bean.InetConfig;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.bean.RpcRegistry;
import com.boarsoft.rpc.listener.RpcListener;

public class NacosRpcListener implements RpcListener {
	private final static Logger log = LoggerFactory.getLogger(NacosRpcListener.class);

	protected NacosDiscoveryProperties props;

	protected String prefix = "pod";

	@Override
	public void onRegister(RpcRegistry reg) {
		int tag = reg.getTag();
		String appId = this.getAppId(prefix, tag, props.getService());
		String clusterName = this.getClusterName(props.getClusterName(), tag);
		// log.debug("Register app instance {}/{} to {}", appId, reg,
		// props.getServerAddr());
		//
		// Map<String, String> meta = new HashMap<String, String>();
		// for (RpcServiceConfig sc : reg.getServiceMap().values()) {
		// meta.put(sc.getName(), sc.getSign());
		// }
		//
		Instance instance = new Instance();
		instance.setIp(InetConfig.LOCAL_IP);
		instance.setPort(RpcConfig.getInt("rpc.http.port", 8080));
		instance.setWeight(props.getWeight());
		instance.setClusterName(clusterName);
		// instance.setMetadata(meta);
		try {
			props.namingServiceInstance().registerInstance(appId, instance);
		} catch (Exception e) {
			log.error("Failed to register app instance {} to {}", //
					reg, props.getServerAddr(), e);
		}
	}

	@Override
	public void onDeregister(RpcRegistry reg) {
		String appId = this.getAppId(prefix, reg.getTag(), props.getService());
		log.info("Deregister app instance {}/{} from {}", appId, reg, props.getServerAddr());
		try {
			props.namingServiceInstance().deregisterInstance(//
					appId, InetConfig.LOCAL_IP, RpcConfig.getInt("rpc.http.port", 8080));
		} catch (Exception e) {
			log.error("Failed to deregister app instance {} from {}", //
					reg, props.getServerAddr(), e);
		}
	}

	protected String getClusterName(String clusterName, int tag) {
		return new StringBuilder(clusterName).append(tag).toString();
	}

	protected String getAppId(String prefix, int tag, String appName) {
		// dom name can only have these characters: 0-9a-zA-Z-._:
		return new StringBuilder(prefix).append(tag).append(":").append(appName).toString();
	}

	@Override
	public void onRemoveLink(String addr, String reason) {
		// Nothing to do
	}

	@Override
	public void onUpdateRegistry(RpcRegistry rr) {
		// Nothing to do
	}

	public NacosDiscoveryProperties getProps() {
		return props;
	}

	public void setProps(NacosDiscoveryProperties props) {
		this.props = props;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
}
