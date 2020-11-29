package com.boarsoft.rpc.listener;

import com.boarsoft.rpc.bean.RpcRegistry;

public interface RpcListener {
	/**
	 * 节点断开事件
	 * 
	 * @param addr
	 * @param reason
	 */
	void onRemoveLink(String addr, String reason);

	/**
	 * 节点启动/心跳时更新注册
	 * 
	 * @param reg
	 */
	void onRegister(RpcRegistry reg);

	/**
	 * 节点停机时更新注册
	 * 
	 * @param reg
	 */
	void onDeregister(RpcRegistry reg);

	/**
	 * 当某个注册表更新时
	 * 
	 * @param rr
	 *            收到的更新的注册表
	 */
	void onUpdateRegistry(RpcRegistry rr);

}
