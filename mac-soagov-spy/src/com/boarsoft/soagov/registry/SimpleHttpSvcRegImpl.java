package com.boarsoft.soagov.registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.config.core.Configable;
import com.boarsoft.soagov.health.HealthChecker;

public class SimpleHttpSvcRegImpl implements ServiceRegistry, Configable {
	private static final Logger log = LoggerFactory.getLogger(SimpleHttpSvcRegImpl.class);

	/** */
	protected HealthChecker healthChecker;
	/** */
	protected static final Map<String, List<String>> providerMap = //
			new ConcurrentHashMap<String, List<String>>();

	/**
	 * { Key：服务编号-服务提供者版本号，Value："服务提供者地址1,服务提供者地址2" }<br>
	 * 实际上此信息应从注册中心获取，由注册中心负责维护。
	 */
	protected Properties prop = new Properties();

	@PostConstruct
	public void init() {
		this.config(); // 初始化配置
		healthChecker.init(providerMap);
	}

	public void config() {
		ClassLoader cl = SimpleHttpSvcRegImpl.class.getClassLoader();
		try {
			log.info("Load conf/registry.properties ...");
			prop.load(cl.getResourceAsStream("conf/registry.properties"));
			providerMap.clear();
			for (String sk : prop.stringPropertyNames()) {
				String[] aa = prop.getProperty(sk, "").split(",");
				List<String> al = new ArrayList<String>();
				for (String a : aa) {
					al.add(a);
				}
				providerMap.put(sk, al);
			}
		} catch (IOException e) {
			log.error("Error on load conf/registry.properties.", e);
		}
	}

	@Override
	public List<String> getProviders(String sk) {
		return providerMap.get(sk);
	}

	public HealthChecker getHealthChecker() {
		return healthChecker;
	}

	public void setHealthChecker(HealthChecker healthChecker) {
		this.healthChecker = healthChecker;
	}
}
