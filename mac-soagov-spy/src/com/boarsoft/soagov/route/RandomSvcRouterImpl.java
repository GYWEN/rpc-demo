package com.boarsoft.soagov.route;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.util.RandomUtil;
import com.boarsoft.config.core.Configable;
import com.boarsoft.soagov.registry.ServiceRegistry;
import com.boarsoft.soagov.registry.SimpleHttpSvcRegImpl;

public class RandomSvcRouterImpl implements ServiceRouter, Configable {
	private static final Logger log = LoggerFactory.getLogger(SimpleHttpSvcRegImpl.class);

	/** 对接服务注册中心的数据 */
	protected ServiceRegistry registry;

	/** { Key: 服务编号-服务消费者（客户端）版本号， Value：服务提供者（Web应用）版本号 } */
	protected Properties prop = new Properties();

	@PostConstruct
	public void init() {
		this.config();
	}

	@Override
	public void config() {
		try {
			log.info("Load conf/router.properties ...");
			prop.load(SimpleHttpSvcRegImpl.class.getClassLoader().getResourceAsStream("conf/router.properties"));
		} catch (IOException e) {
			log.error("Error on load conf/registry.properties.", e);
		}
	}

	@Override
	public String select1(String sc, String pv) {
		return this.select1(String.format("%s-%s", sc, pv));
	}

	@Override
	public String select1(String sk) {
		// 根据服务提供者版本，取该版本的实例列表
		List<String> al = registry.getProviders(sk);
		// 如果没有对应的服务提供者（后台Web应用），则返回“服务不可用”
		if (al.isEmpty()) {
			return null;
		}
		int max = al.size() - 1;
		return al.get(max > 0 ? RandomUtil.random(0, max) : 0);
	}

	@Override
	public String getProvider(String sc, String cv) {
		// 根据 服务编号（Web应用上下文）和服务消费者（客户端）版本号取服务提供者版本号
		String pv = prop.getProperty(String.format("%s/%s", cv, sc));
		return this.select1(pv);
	}

	public ServiceRegistry getRegistry() {
		return registry;
	}

	public void setRegistry(ServiceRegistry registry) {
		this.registry = registry;
	}
}
