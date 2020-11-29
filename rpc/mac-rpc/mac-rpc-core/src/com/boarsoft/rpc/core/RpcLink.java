package com.boarsoft.rpc.core;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.boarsoft.common.bean.InetConfig;
import com.boarsoft.common.util.RandomUtil;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcInvoking;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcRegistry;
import com.boarsoft.rpc.bean.RpcStub;

/**
 * 节点与节点之间的逻辑连接
 * 
 * @author Mac_J
 *
 */
public class RpcLink {
	private static final Logger log = LoggerFactory.getLogger(RpcLink.class);

	public static final int STATUS_TEMP = 0;
	public static final int STATUS_BOUND = 1;
	public static final int STATUS_READY = 2;

	protected RpcCore rpcCore;
	protected RpcContext rpcContext;

	/** 远端节点的地址：IP:PORT */
	protected String remoteAddr;
	/** 在这个连接上最后一次发送心跳的时间，用于补充检测失效的节点 */
	protected AtomicLong lastBeat = new AtomicLong(new Date().getTime());
	/** 有来标识当前连接是否已准备好开始用于应用的方法调用 */
	protected AtomicInteger status = new AtomicInteger(0);
	/** 归属于当前逻辑连接的多个逻辑通道 */
	protected Map<String, RpcChannel> channelMap = new ConcurrentHashMap<String, RpcChannel>();

	public RpcLink(RpcCore core, String addr) {
		this.rpcCore = core;
		this.rpcContext = core.getRpcContext();
		this.remoteAddr = addr;
	}

	/**
	 * 开启新的逻辑连接通道
	 * 
	 * @return
	 * @throws Throwable
	 */
	public RpcChannel openChannel() throws Throwable {
		// 创建RpcChannel，建立实际的连接通道
		RpcChannel rc = new RpcChannel(this);
		this.channelMap.put(rc.getKey(), rc);
		// 调用框架内置的远程服务方法，绑定新建的连接通道到逻辑连接
		log.debug("Invoke bind link method of {}", remoteAddr);
		// 调用远程节点的bindLink方法，ID_BIND_LINK为方法ID，参数为当前节点的地址
		RpcMethodConfig mc = RpcContext.getMyMethodConfig(RpcMethodConfig.ID_BIND_LINK);
		mc.setTimeout(RpcConfig.BIND_LINK_TIMEOUT);
		this.invoke(rc, mc, new Object[] { InetConfig.LOCAL_ADDR });
		return rc;
	}

	/**
	 * 开启至少一个逻辑连接通道，一个逻辑连接通道与一个实际的连接通道关联
	 * 
	 * @throws Throwable
	 */
	public void openChannels() throws Throwable {
		log.info("Open {} channels to {}", RpcConfig.LINK_CHANNELS, remoteAddr);
		// 连接通道数是可配置的，但默认值为1，因为通常只需要一个
		for (int i = 0; i < RpcConfig.LINK_CHANNELS; i++) {
			this.openChannel();
		}
	}

	public RpcChannel getChannel() throws Throwable {
		int r = 0;
		if (this.channelMap.size() > 1) {
			r = RandomUtil.random(0, channelMap.size());
		}
		for (RpcChannel rc : this.channelMap.values()) {
			if (r > 0) {
				r--;
				continue;
			}
			return rc;
		}
		return openChannel();
	}

	public void putChannel(RpcChannel rc) {
		this.channelMap.put(rc.getKey(), rc);
	}

	public boolean isConnected() {
		// 判断当前逻辑连接是否可用（至少有一个有效的逻辑通道）
		for (RpcChannel rc : channelMap.values()) {
			if (rc.isConnected()) {
				continue;
			}
			channelMap.remove(rc.key);
		}
		return !channelMap.isEmpty();
	}

	/**
	 * 关闭当前逻辑连接，及其下属逻辑通道和实际的连接<br>
	 * 但这不会导致远程节点也关闭对应的连接通道，可能导致半关连接问题<br>
	 * 需要在关闭连接前发送nodeOff离线通知，让相应的节点也关闭它那一方的连接<br>
	 * 同时，为了保险起见，远程节点在收到shuttingDown通知后，会在一定时间后主动关闭连接。
	 * 
	 * @param reason
	 */
	public void close(String reason) {
		for (RpcChannel rc : this.channelMap.values()) {
			rc.close(reason);
		}
		this.channelMap.clear();
	}

	protected Object invoke(RpcChannel rc, RpcMethodConfig mc, Object[] args) throws Throwable {
		// 通过RpcReference来获取（来自远程节点的）方法ID和调用序号，而不是通过注册表
		RpcStub ref = rpcContext.getStub(remoteAddr);
		// 但当方法为广播方法，不同的remoteHost的methodId不同，必须这样取
		Integer methodId = ref.getMethodId(mc.getKey());
		// 内部方法用上面的办法取不到methodId
		if (methodId == null) {
			// 内部方法自带methodId，可以这样取
			methodId = mc.getRelativeId();
		}
		// 以下两种情况可能造成methodId为空
		// 1：指定了服务提供者的IP，但该IP并未提供对应的服务
		// 2：当提供端和消费端注册表同步失败时
		if (methodId == null) {
			throw new NoSuchMethodException(String.format( //
					"Method id of '%s' is null from %s", mc.getKey(), remoteAddr));
		}

		long exeNo = ref.getMethodExeNo(methodId);
		RpcCall co = new RpcCall(mc.getProtocol(), RpcCall.TYPE_REQUEST, methodId, exeNo, args);
		co.setTraceId(MDC.get("traceId"));
		co.setAttachments(rpcContext.getAttachments());

		// RpcInvoking实现了Future接口，其get方法会阻塞，直到收到应答
		RpcInvoking ri = new RpcInvoking(rpcContext, remoteAddr, mc, co);
		// RpcChannel的onResponse会移除此对象
		rpcContext.putInvoking(ri);

		// 只要是异步的调用形式，都不需要阻塞当前线程，直接返回Future对象
		switch (mc.getType()) {
		case RpcMethodConfig.TYPE_ASYNC_CALL:
		case RpcMethodConfig.TYPE_ASYNC_NOTICE:
		case RpcMethodConfig.TYPE_SYNC_BROADCAST:
		case RpcMethodConfig.TYPE_ASYNC_BROADCAST:
		case RpcMethodConfig.TYPE_BROADCAST_NOTICE:
			rc.write(co); // rc.write不会被阻塞，也不会受网络影响
			return ri; // 注意：这里返回的是ri这个Future对象，而不是真实的结果
		}

		try {
			rc.write(co);
			// hold方法会加锁，在await前判断调用结果是否已经返回
			ri.hold(mc.getTimeout());
		} finally {
			// 出现异常要及时移除，因为不一定收得到响应
			rpcContext.removeInvoking(ri);
		}
		// 如果远程方法有抛出异常，在本地将此异常抛出
		if (co.getThrowable() != null) {
			throw co.getThrowable();
		}
		// 返回远程方法的执行结果
		return co.getResult();
	}

	/**
	 * 此方法只针对框架内部约定的方法有效
	 * 
	 * @param methodId
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public Object invoke(int myMethodId, Object[] args) throws Throwable {
		RpcChannel rc = this.getChannel();
		RpcMethodConfig mc = RpcContext.getMyMethodConfig(myMethodId);
		return invoke(rc, mc, args);
	}

	public Object invoke(RpcMethodConfig mc, Object[] args) throws Throwable {
		// 从可能存在的多个逻辑通道中获取一个
		RpcChannel rc = this.getChannel();
		if (rc == null) {
			throw new IllegalStateException(String.format("Can not get channel of %s", this));
		}
		// 通过这个逻辑通道发送RPC调用请求
		return this.invoke(rc, mc, args);
	}

	public Object invoke(RpcChannel rc, int myMethodId, Object[] args) throws Throwable {
		RpcMethodConfig mc = RpcContext.getMyMethodConfig(myMethodId);
		return invoke(rc, mc, args);
	}

	/**
	 * 向注册中心提交注册表，获得远程更新注册表
	 * 
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	public void popRegistry() throws Throwable {
		log.info("Pop registry to {}", remoteAddr);
		RpcChannel rc = this.getChannel();
		RpcMethodConfig mc = RpcContext.getMyMethodConfig(RpcMethodConfig.ID_POP_REGISTRY);
		mc.setTimeout(RpcConfig.POP_REG_TIMEOUT);
		Map<String, RpcRegistry> rm = (Map<String, RpcRegistry>) invoke(rc, mc,
				new Object[] { this.rpcContext.getMyRegistry() });
		for (String k : rm.keySet()) {
			RpcRegistry rr = (RpcRegistry) rm.get(k);
			this.rpcCore.updateRegistry(rr);
		}
		this.status.set(STATUS_READY);
	}

	public void removeChannel(RpcChannel rc) {
		this.channelMap.remove(rc.getKey());
	}

	public RpcCore getRpcCore() {
		return this.rpcCore;
	}

	public String getRemoteAddr() {
		return this.remoteAddr;
	}

	public String toString() {
		return this.remoteAddr;
	}

	public void setLastBeat(long lastBeat) {
		// log.info("Set last beat of link {}", this);
		this.lastBeat.set(lastBeat);
	}

	public boolean before(long d) {
		return lastBeat.get() < d;
	}

	public boolean isReady() {
		return status.get() == STATUS_READY;
	}

	public void setStatus(int status) {
		this.status.set(status);
	}
}
