package com.boarsoft.rpc.spring.cloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.stereotype.Component;

import com.boarsoft.rpc.bean.RpcRegistry;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.listener.RpcListener;

@Component
public class DefaultServiceRegister implements RpcListener {
	private final static Logger log = LoggerFactory.getLogger(DefaultServiceRegister.class);

	@Autowired
	protected ServiceRegistry<Registration> serviceRegistry;

	@Override
	public void onRegister(RpcRegistry reg) {
		log.info("Submit registry {} to spring cloud", reg);
		try {
			for (RpcServiceConfig sc : reg.getServiceMap().values()) {
				serviceRegistry.register(new MacRpcRegistration(sc));
			}
		} catch (Exception e) {
			log.error("Error on submit registry {} to spring cloud", reg, e);
		}
	}

	@Override
	public void onDeregister(RpcRegistry reg) {
		log.info("Remove registry {} from spring cloud", reg);
		try {
			for (RpcServiceConfig sc : reg.getServiceMap().values()) {
				serviceRegistry.deregister(new MacRpcRegistration(sc));
			}
		} catch (Exception e) {
			log.error("Error on remove registry {} from spring cloud", reg, e);
		}
	}

	@Override
	public void onRemoveLink(String addr, String reason) {
		// Nothing to do
	}

	@Override
	public void onUpdateRegistry(RpcRegistry rr) {
		// Nothing to do
	}
}
