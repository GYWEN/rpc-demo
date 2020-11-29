package com.boarsoft.rpc.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boarsoft.common.bean.InetConfig;
import com.boarsoft.common.util.JsonUtil;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcRegistry;

@Component("rpcKeeper")
public class RpcKeeper implements RpcNode {
	private static final Logger log = LoggerFactory.getLogger(RpcKeeper.class);

	@Autowired
	protected RpcCore rpcCore;
	@Autowired
	protected RpcContext rpcContext;

	@Override
	public Map<String, RpcRegistry> popRegistry(final RpcRegistry rr) {
		log.info("Received poped registry {}", rr.getKey());
		boolean ok = this.rpcCore.updateRegistry(rr);
		// 如果更新本地成功，且当前节点是主节点，就广播推送
		if (ok & rpcCore.isMaster()) {
			rpcCore.pushRegistry(rr);
		}
		// 收到注册表，就表示这个连接可以用了，需要开启心跳检查
		RpcLink lo = rpcCore.getLink(rr.getKey());
		lo.setStatus(RpcLink.STATUS_READY);
		return this.rpcContext.getRegistryMap();
	}

	@Override
	public void bindLink(String from) throws TimeoutException {
		log.info("Bind link with {}", from);
		this.rpcCore.bindChannel(from);
	}

	@Override
	public Map<String, RpcRegistry> heartBeat(String from, Map<String, Date> vm) {
		// log.debug("Received heart beat from {}", from);
		Map<String, RpcRegistry> rm = new HashMap<String, RpcRegistry>();
		try {
			// 所有方法调用（包括心跳方法）都会在RpcChannel.onRequest方法中更新心跳时间
			// RpcLink lo = this.rpcCore.getLink(from);
			// lo.setLastBeat(System.currentTimeMillis());
			// 比较当前节点与收到的远程节点的各个注册表的版本
			for (String k : vm.keySet()) {
				if (vm.get(k) == null) {
					continue;
				}
				RpcRegistry r = rpcContext.getRegistry(k);
				if (r == null || r.getVersion() == null) {
					continue;
				}
				if (r.getVersion().after(vm.get(k))) {
					rm.put(k, r);
				}
			}
		} catch (Throwable e) {
			log.error("Error on process heart beat from {}", from, e);
		}
		return rm;
	}

	@Override
	public void pushRegistry(final RpcRegistry rr, final List<String> exLt) {
		log.info("Received pushed registry of {}", rr.getKey());
		this.rpcCore.updateRegistry(rr);
		if (rpcCore.isMaster()) {
			rpcCore.getThreadPool().execute(new Runnable() {
				public void run() {
					List<String> lt = new ArrayList<String>(exLt);
					lt.addAll(RpcKeeper.this.rpcCore.getLinkMap().keySet());
					// 推送注册表，设置超时时间
					RpcMethodConfig mc = RpcContext.getMyMethodConfig(RpcMethodConfig.ID_PUSH_REGISTRY);
					mc.setTimeout(RpcConfig.PUSH_REG_TIMEOUT);
					rpcCore.broadcast(mc, new Object[] { rr, lt }, exLt);
				}
			});
		}
	}

	@Override
	public void shuttingDown(final String host, final int timeout, final List<String> exLt) {
		log.info("Remote host {} is shutting down", host);
		// 仅移除provider，不去移除和关闭逻辑连接，以便当前方法的结果能传回
		rpcContext.removeProvider(host);
		if (this.rpcCore.isMaster()) {
			rpcCore.getThreadPool().execute(new Runnable() {
				public void run() {
					exLt.add(host); // 排除掉源节点
					exLt.add(InetConfig.LOCAL_ADDR); // 排除自己
					List<String> lt = new ArrayList<String>(exLt);
					// 让被通知的其它主节点，排除掉与自己相连的其它节点，避免重复通知
					lt.addAll(RpcKeeper.this.rpcCore.getLinkMap().keySet());
					try {
						// 推送停机通知，设置超时时间
						RpcMethodConfig mc = RpcContext.getMyMethodConfig(RpcMethodConfig.ID_SHUTTING_DOWN);
						mc.setTimeout(RpcConfig.SHUTDOWN_TIMEOUT);
						rpcCore.broadcast(mc, new Object[] { host, timeout, lt }, exLt);
					} catch (Throwable e) {
						log.error("Error on broadcast SHUTTING_DOWN {}, {}", host, lt);
					}
				}
			});
		}
		// 不再定时去断开连接，因为远程节点可能快速重启
	}

	@Override
	public void nodeOff(final String host, final List<String> exLt) {
		log.info("Remote host {} is off", host);
		// 移除连接时会移除provider
		this.rpcCore.removeLink(host, true, "the node off");
		if (this.rpcCore.isMaster()) {
			rpcCore.getThreadPool().execute(new Runnable() {
				public void run() {
					List<String> lt = new ArrayList<String>(exLt);
					lt.add(InetConfig.LOCAL_ADDR); // 排除自己
					lt.addAll(RpcKeeper.this.rpcCore.getLinkMap().keySet());
					// 推送离线通知，设置超时时间
					RpcMethodConfig mc = RpcContext.getMyMethodConfig(RpcMethodConfig.ID_NODE_OFF);
					mc.setTimeout(RpcConfig.NODE_OFF_TIMEOUT);
					rpcCore.broadcast(mc, new Object[] { host, lt }, exLt);
				}
			});
		}
	}

	@Override
	public void disableProviders(String remoteHost) {
		rpcContext.disableProviders(remoteHost);
	}

	@Override
	public void disableProvider(String remoteHost, String serviceKey) {
		rpcContext.disableProvider(remoteHost, serviceKey);
	}

	@Override
	public void disableService(String serviceKey) {
		rpcContext.disableService(serviceKey);
	}

	@Override
	public void enableProviders(String remoteHost) {
		rpcContext.enableProviders(remoteHost);
	}

	@Override
	public void enableProvider(String remoteHost, String serviceKey) {
		rpcContext.enableProvider(remoteHost, serviceKey);
	}

	@Override
	public void enableService(String serviceKey) {
		rpcContext.enableService(serviceKey);
	}

	@Override
	public String getInfo() {
		Map<String, Object> rm = new HashMap<String, Object>();
		// My Enabled Providers
		rm.put("mep", rpcContext.getProviderMap());
		// My Disabled Providers
		rm.put("mdp", rpcContext.getProviderMap2());
		rm.put("ref", rpcContext.getStubMap());
		rm.put("reg", rpcContext.getRegistryMap());
		return JsonUtil.from(rm, "interfaceClazz,methodExeNoMap,methodExeNo");
	}
}
