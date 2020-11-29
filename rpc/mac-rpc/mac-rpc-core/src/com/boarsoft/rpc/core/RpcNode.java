package com.boarsoft.rpc.core;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.boarsoft.rpc.bean.RpcMethod;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcRegistry;

/**
 * 节点内置服务接口
 * 
 * @author Mac_J
 *
 */
public interface RpcNode {
	/**
	 * 提交注册表
	 * 
	 * @param reg
	 * @return
	 */
	@RpcMethod(id = RpcMethodConfig.ID_POP_REGISTRY)
	Map<String, RpcRegistry> popRegistry(RpcRegistry reg);

	/**
	 * 将当前通道与指定的地址的逻辑连接绑定
	 * 
	 * @param from
	 * @throws TimeoutException
	 */
	@RpcMethod(id = RpcMethodConfig.ID_BIND_LINK, timeout = 100000)
	void bindLink(String from) throws TimeoutException;

	/**
	 * 推送注册表
	 * 
	 * @param reg
	 * @param exLt
	 */
	@RpcMethod(id = RpcMethodConfig.ID_PUSH_REGISTRY)
	void pushRegistry(RpcRegistry reg, List<String> exLt);

	/**
	 * 仅用于向注册中心发送监控信息，不用于断线检查，也不用于同步注册表
	 * 
	 * @param from
	 * @param vm
	 *            所有注册表的版本
	 * @return
	 */
	@RpcMethod(id = RpcMethodConfig.ID_HEART_BEAT)
	Map<String, RpcRegistry> heartBeat(String from, Map<String, Date> vm);

	/**
	 * 用于广播通知目标节点，host节点正在停机，把host从所有服务的服务提供者列表中移除
	 * 
	 * @param host
	 * @param exLt
	 */
	@RpcMethod(id = RpcMethodConfig.ID_SHUTTING_DOWN, type = RpcMethodConfig.TYPE_ASYNC_NOTICE)
	void shuttingDown(final String host, final int timeout, final List<String> exLt);

	/**
	 * 用于通知目标节点，host节点已断开，把host从所有服务的服务提供者列表中移除，并移除连接
	 * 
	 * @param host
	 * @param exLt
	 */
	@RpcMethod(id = RpcMethodConfig.ID_NODE_OFF, type = RpcMethodConfig.TYPE_ASYNC_NOTICE)
	void nodeOff(String host, List<String> exLt);

	/**
	 * 在本地禁止对某个远程节点的所有服务的访问
	 * 
	 * @param remoteHost
	 */
	@RpcMethod(id = RpcMethodConfig.ID_DISABLE_PROVIDERS)
	void disableProviders(String remoteHost);

	/**
	 * 在本地禁止对某个远程节点的某个服务的访问
	 * 
	 * @param remoteHost
	 * @param serviceKey
	 */
	@RpcMethod(id = RpcMethodConfig.ID_DISABLE_PROVIDER)
	void disableProvider(String remoteHost, String serviceKey);

	/**
	 * 在本地禁止对所有远程节点的某个服务的访问
	 * 
	 * @param serviceKey
	 */
	@RpcMethod(id = RpcMethodConfig.ID_DISABLE_SERVICE)
	void disableService(String serviceKey);

	/**
	 * 在本地放开对某个远程节点的所有服务的访问
	 * 
	 * @param remoteHost
	 * @param serviceKey
	 */
	@RpcMethod(id = RpcMethodConfig.ID_ENABLE_PROVIDERS)
	void enableProviders(String remoteHost);

	/**
	 * 在本地放开对某个远程节点的指定服务的访问
	 * 
	 * @param remoteHost
	 * @param serviceKey
	 */
	@RpcMethod(id = RpcMethodConfig.ID_ENABLE_PROVIDER)
	void enableProvider(String remoteHost, String serviceKey);

	/**
	 * 在本地放开对所有远程节点的某个服务的访问
	 * 
	 * @param serviceKey
	 */
	@RpcMethod(id = RpcMethodConfig.ID_ENABLE_SERVICE)
	void enableService(String serviceKey);

	/**
	 * 返回本地所有注册表信息
	 * 
	 * @return
	 */
	@RpcMethod(id = RpcMethodConfig.ID_GET_INFO)
	String getInfo();
}
