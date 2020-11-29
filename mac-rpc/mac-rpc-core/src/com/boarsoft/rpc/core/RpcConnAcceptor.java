package com.boarsoft.rpc.core;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.util.SocketUtil;

/**
 * 用于受理Socket连接请求
 * 
 * @author Mac_J
 *
 */
public class RpcConnAcceptor implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(RpcConnAcceptor.class);

	protected Future<AsynchronousSocketChannel> future;
	protected RpcCore rpcCore;

	public RpcConnAcceptor(RpcCore rpcCore) {
		this.rpcCore = rpcCore;
	}

	@Override
	public void run() {
		while (RpcCore.isRunning()) {
			this.future = this.rpcCore.asyncServerSocketChannel.accept();
			AsynchronousSocketChannel asc = null;
			String rh = null;
			try {
				asc = (AsynchronousSocketChannel) this.future.get();
				rh = SocketUtil.getSocketAddressStr(asc.getRemoteAddress());
				log.info("Remote host {} connected", rh);
			} catch (Exception e) {
				if (RpcCore.isStopping() || RpcCore.isStopped()) {
					return; // 停机过程或停机后都中不再接收新连接
				}
				log.error("Error on accept remote socket", e);
				continue;
			}
			// 这里还不能创建RpcLink，因为还不知道对方监听的端号，需要等对方推送注册表后方可
			this.rpcCore.newInChannel(asc);
		}
	}
}
