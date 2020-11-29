package com.boarsoft.rpc.core;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.PriorityOrdered;

import com.boarsoft.common.Util;
import com.boarsoft.common.bean.InetConfig;
import com.boarsoft.common.util.InetUtil;
import com.boarsoft.common.util.RandomUtil;
import com.boarsoft.common.util.SocketUtil;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.bean.RpcInvoking;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcNodeConfig;
import com.boarsoft.rpc.bean.RpcReferenceConfig;
import com.boarsoft.rpc.bean.RpcRegistry;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.bean.RpcStub;
import com.boarsoft.rpc.listener.RpcListener;

//@Component("rpcCore")
//@Scope("singleton")
public class RpcCore implements Runnable, PriorityOrdered {
	private static final Logger log = LoggerFactory.getLogger(RpcCore.class);

	public static final short STATUS_STOPPED = 0;
	public static final short STATUS_STARTING = 1;
	public static final short STATUS_RUNNING = 2;
	public static final short STATUS_STOPPING = 3;

	public static RpcCore instance;
	@Autowired
	protected RpcContext rpcContext;

	/** */
	protected List<RpcListener> listeners = new LinkedList<RpcListener>();

	/** */
	// protected ThreadPoolTaskExecutor threadPool;
	protected ExecutorService threadPool;// = Executors.newCachedThreadPool();
	// protected ExecutorService threadPool = new ThreadPoolExecutor(0,
	// RpcConfig.THREADPOOL_MAX_SIZE,
	// RpcConfig.THREADPOOL_KEEP_ALIVE, TimeUnit.SECONDS, new
	// SynchronousQueue<Runnable>(), this.daemonThreadFactory);
	/** */
	protected ScheduledExecutorService scheduler;
	/** */
	// protected ExecutorService ioThreadPool =
	// Executors.newFixedThreadPool(RpcConfig.IO_THREADS);
	protected ExecutorService ioThreadPool;
	/** node tag，为0表示公共tag，可以被其它tag的服务消费者调用 */
	protected static int tag = 0;

	/** */
	protected AsynchronousChannelGroup asyncChannelGroup;
	/** */
	protected AsynchronousServerSocketChannel asyncServerSocketChannel;
	/** */
	protected static final ConcurrentMap<String, RpcLink> linkMap = new ConcurrentHashMap<String, RpcLink>();
	/** 用于缓存临时的RpcChannel */
	protected static final Map<String, RpcChannel> tmpInChlMap = new ConcurrentHashMap<String, RpcChannel>();
	/** */
	protected static String masterHost;
	/** */
	protected static final List<String> masterList = new ArrayList<String>();
	/** */
	protected static boolean isMaster = false;
	/** */
	protected final RpcConnAcceptor connAcceptor = new RpcConnAcceptor(this);
	/** */
	public final static AtomicInteger status = new AtomicInteger(STATUS_STOPPED);

	public RpcCore() throws IOException {
		instance = this;
		status.set(STATUS_STARTING);
	}

	// 不能用PreDestroy，此方法必须在容器关闭前执行
	@SuppressWarnings("unchecked")
	// @PreDestroy
	public void shutdown() {
		// 广播停机通知
		log.warn("Shutdown RPC core in {}s", RpcConfig.SHUTDOWN_TIMEOUT);
		// 修改节点状态为“停机中”
		status.set(STATUS_STOPPING);
		// 停机时通知服务注册插件
		for (RpcListener listener : listeners) {
			listener.onDeregister(rpcContext.getMyRegistry());
		}
		// 关闭服务端通道
		log.warn("Close server socket channel");
		SocketUtil.closeChannel(this.asyncServerSocketChannel);
		// 排除那些与当前节点直接相连的节点，因为这些节点本次已经通知过了，不需要主节点再广播一次
		List<String> exLt = new ArrayList<String>();
		exLt.addAll(linkMap.keySet());
		// 停机时，线程池会自动关闭，但这里是循环发送，不受影响
		// 收到停机通知的远程节点会在指定时间后主动关闭连接，避免半关连接问题
		try {
			RpcMethodConfig mc = RpcContext.getMyMethodConfig(RpcMethodConfig.ID_SHUTTING_DOWN);
			// 注意：不是SHUTDOWN_TIMEOUT
			mc.setTimeout(RpcConfig.SHUTTING_DOWN_TIMEOUT);
			this.broadcast(mc, new Object[] { InetConfig.LOCAL_ADDR, RpcConfig.SHUTDOWN_TIMEOUT, exLt }, null);
		} catch (Exception e) {
			log.error("Error on broadcast shutdown notice", e);
		}
		// 停机倒计时，等待正在执行调用完成
		for (int i = RpcConfig.SHUTDOWN_TIMEOUT; i > 0; i--) {
			try {
				// 由于此时定时线程已停上，需要在此检查超时的调用
				rpcContext.run();
				if (rpcContext.hasInvoking()) {
					log.info("{}", i);
					Thread.sleep(1000L);
				} else {
					break;
				}
			} catch (InterruptedException e) {
			}
		}
		Map<String, RpcInvoking> im = rpcContext.getInvokingMap();
		if (!im.isEmpty()) {
			log.warn("{} RPC invoking be ignored", im.size());
			// for (String k : im.keySet()) {
			// log.warn("Invoking {}/{} be ignored", k,
			// im.get(k).getMethodConfig().getKey());
			// }
		}
		// 发送节点关闭通知，以便远程节点关闭与自己的连接，避免半关连接问题
		try {
			// 异步并行调用通知各节点（AN），以避免因为某个节点发不通导致，在服务方主动关闭连接后才发起
			RpcMethodConfig mc = RpcContext.getMyMethodConfig(RpcMethodConfig.ID_NODE_OFF);
			mc.setTimeout(RpcConfig.NODE_OFF_TIMEOUT);
			Map<String, Object> rm = this.broadcast(mc, //
					new Object[] { InetConfig.LOCAL_ADDR, exLt }, null);
			// 需要集中阻塞等待返回，避免连接过早关闭连接，导致通知未能发出
			for (String k : rm.keySet()) {
				Object o = rm.get(k);
				if (o != null && o instanceof Future) {
					// log.debug("Future.get for NODE_OFF with timeout {}",
					// RpcConfig.NODE_OFF_TIMEOUT);
					((Future<Object>) o).get(RpcConfig.NODE_OFF_TIMEOUT, TimeUnit.MILLISECONDS);
				}
			}
		} catch (Exception e) {
			log.error("Error on broadcast node off notice", e);
		}
		// 设置节点状态为“已关闭”，以取消定时任务
		status.set(STATUS_STOPPED);
		// 关闭与其它节点连接
		for (RpcLink lo : this.getLinkMap().values()) {
			try {
				lo.close("shutdown hook");
			} catch (Exception e) {
				log.warn("Can not close RPC link {}", lo, e);
			}
		}
		try {
			// 关闭异步通道组
			this.asyncChannelGroup.shutdown();
		} catch (Exception e) {
			log.error("Error on shutdown async channel group", e);
		} finally {
			SocketUtil.closeChannel(this.asyncServerSocketChannel);
		}
		log.info("Stopped successfully.");
	}

	public static RpcCore getCurrentInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	@PostConstruct
	public void init() throws Exception {
		// 强制初始化RpcConfig，以便获取IP和端口
		ClassLoader classLoader = RpcConfig.class.getClassLoader();
		RpcConfig.init(classLoader.getResourceAsStream("conf/config.properties"));
		// 加载其它配置文件
		Map<String, InetAddress> aMap = InetUtil.getLocalAddressMap();
		Document nd = new SAXReader().read(classLoader.getResourceAsStream("conf/nodes.xml"));
		List<Node> nl = nd.selectNodes("/nodes/node");
		String myIp = RpcConfig.getProperty("rpc.ip", null);
		String ma = InetUtil.getHostAddress();
		String mh = InetUtil.getHostName();
		for (Node n : nl) {
			RpcNodeConfig c = new RpcNodeConfig(n);
			// 检查nodes.xml中是否有本机的某个IP，有说明应该取这个IP作为节点IP
			String ip = c.getIp();
			if (mh.equals(ip) || ma.equals(ip) || aMap.containsKey(ip) || //
					"127.0.0.1".equals(ip) || "localhost".equals(ip)) {
				myIp = c.getIp();
				if (InetConfig.LOCAL_PORT == c.getPort()) {
					isMaster = c.isMaster();
					continue;
				}
			}
			if (c.isMaster()) {
				masterList.add(c.getAddress());
			}
		}
		if (Util.strIsEmpty(myIp)) {
			myIp = InetUtil.match(aMap.keySet(), masterList);
		}
		RpcConfig.setIp(myIp);
		log.info("Local address is {}", InetConfig.LOCAL_ADDR);

		status.set(STATUS_RUNNING); // 此句必须在rpcContext.init之前
		rpcContext.init();

		asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(this.ioThreadPool);
		try {
			// 一个主机可能有多个IP地址，这里需要指定要在哪个IP地址和端口上开启监听
			asyncServerSocketChannel = AsynchronousServerSocketChannel.open(this.asyncChannelGroup)
					.bind(new InetSocketAddress("0.0.0.0", InetConfig.LOCAL_PORT));
		} catch (BindException e) {
			log.error("Address already in use: {}", InetConfig.LOCAL_ADDR);
			throw e;
		}
		// 使用IO线程池来执行RpcConnAcceptor以处理连接请求
		this.ioThreadPool.execute(this.connAcceptor);

		if (isMaster) {
			this.checkMasters();
		} else {
			this.pickMaster(); // 此动作必须在rpcContext.init以及本地监听开启之后（作为注册中心时）
		}

		// 开始心跳
		this.scheduler.scheduleWithFixedDelay(this, RpcConfig.HEARTBEAT_PERIOD, RpcConfig.HEARTBEAT_PERIOD,
				TimeUnit.MILLISECONDS);
		// 检查超时又没有被移除的方法
		this.scheduler.scheduleWithFixedDelay(rpcContext, 1000, 1000, TimeUnit.MILLISECONDS);

		log.info("RPC context initiated successfully.");
	}

	/**
	 * 从多个master中选一个，但不能选自己
	 */
	protected void pickMaster() {
		log.debug("Select RPC master node from {}", masterList.size());
		if (masterList.isEmpty()) {
			log.warn("Master list is empty");
			return;
		}
		// 取主机名+端口，而不是ip+端口，因为配置的主节点可能是域名或主机名
		String mh = new StringBuilder(InetUtil.getHostName())//
				.append(":").append(InetConfig.LOCAL_PORT).toString();
		// 复制主节点列表，用于筛选
		List<String> lt = new ArrayList<String>();
		lt.addAll(masterList);
		while (!lt.isEmpty()) {
			int i = RandomUtil.random(0, lt.size());
			String h = lt.remove(i);
			if (Util.strIsEmpty(h)) {
				log.warn("Skip invalid master address");
				continue;
			}
			// 如果配置的主节点为本机 IP:PORT 或 主机名:端口 则跳过
			if (h.equals(InetConfig.LOCAL_ADDR) || h.equals(mh)) {
				continue;
			}
			// 如果有不止一个master 去除原来的masterHost，避免又选中它
			if (!lt.isEmpty() && h.equals(masterHost)) {
				continue;
			}
			log.info("Try link to master node {}", h);
			RpcLink lo = null;
			try {
				lo = this.link2(h);
				if (lo != null && lo.isConnected()) {
					masterHost = h;
					linkMap.put(masterHost, lo);
					log.info("My master node is {}", masterHost);
					return;
				}
			} catch (Throwable e) {
				log.warn("Can not build link with {}", h, e);
			}
		}
		log.warn("Can not resolve master node");
	}

	public void newInChannel(AsynchronousSocketChannel asc) {
		try {
			RpcChannel rc = new RpcChannel(this, asc);
			tmpInChlMap.put(rc.getKey(), rc);
		} catch (Throwable e) {
			log.error("Can not create new RpcChannel on {}", asc, e);
		}
	}

	/**
	 * 将当前RpcChannel与remoteHost对应的RpcLink对象绑定
	 * 
	 * @param remoteAddr
	 * @throws TimeoutException
	 * @throws Throwable
	 */
	public void bindChannel(String remoteAddr) throws TimeoutException {
		// 获取当前RpcChannel
		RpcChannel rc = RpcContext.getCurrInChl();
		if (rc == null) {
			return;
		}
		String ck = rc.getKey();
		// 将此RpcChannel与对应remoteHost这一地址的RpcLink关联起来
		log.debug("Bind channel {} to link {}", ck, remoteAddr);
		try {
			RpcLink lo = this.getLink(remoteAddr);
			// 一旦绑定成功，就从临时map中移除
			tmpInChlMap.remove(ck);
			// 让RpcChannel持有RpcLink的引用
			rc.setRpcLink(lo);
			rc.setRemoteAddr(remoteAddr);
			rc.setStatus(RpcChannel.STATUS_NORMAL);
			lo.setStatus(RpcLink.STATUS_BOUND);
			lo.putChannel(rc);
		} catch (Throwable e) {
			log.error("Error on bind channel {} to link {}", ck, remoteAddr, e);
		}
	}

	/**
	 * 返回RpcLink，但并不激活
	 * 
	 * @param addr
	 * @return
	 */
	protected RpcLink getLink(String addr) {
		RpcLink lo = linkMap.get(addr);
		// 获取对应的逻辑对象，如果对方不存在则创建
		if (lo == null) {
			synchronized (linkMap) {
				lo = linkMap.get(addr);
				if (lo == null) {
					// 新创建的逻辑连接并不包含任何逻辑通道
					lo = new RpcLink(this, addr);
					linkMap.put(addr, lo);
				}
			}
		}
		return lo;
	}

	/**
	 * 获取到指定节点的连接
	 * 
	 * @param addr
	 *            节点地址
	 * @return
	 * @throws Throwable
	 */
	public RpcLink link2(String addr) throws Throwable {
		RpcLink lo = this.getLink(addr);
		// 如果获取到的逻辑连接无有效的逻辑连接，则尝试重连连接
		if (lo.isConnected()) {
			return lo;
		}
		// active为true表示要打开连接（开启逻辑通道）
		synchronized (lo) {
			if (!lo.isConnected()) {
				lo.openChannels();
				// 重连后总是提交注册表
				lo.popRegistry();
			}
		}
		return lo;
	}

	public AsynchronousChannelGroup getAsyncChannelGroup() {
		return this.asyncChannelGroup;
	}

	public ConcurrentMap<String, RpcLink> getLinkMap() {
		return linkMap;
	}

	public RpcContext getRpcContext() {
		return this.rpcContext;
	}

	/**
	 * 此方法在主动关闭逻辑连接时，或在IO操作异常时，关闭逻辑连接通道<br>
	 * 如果所有逻辑连接通道都不可用，则关闭逻辑连接
	 * 
	 * @param rc
	 *            出现异常，需要关闭的逻辑连接通道
	 * @param reason
	 *            关闭的原因
	 */
	public void onChannelClose(RpcChannel rc, String reason) {
		String rh = rc.getRemoteAddr();
		log.warn("RpcChannel {} on {} be closed since {}", rc, rh, reason);
		// 需要同步，以避免当一个RpcLink有多个RpcChannel时，重复广播的问题
		synchronized (rc) {
			// rh为空表示此channel还未与remoteHost绑定，直接移队即可
			tmpInChlMap.remove(rc.getKey());
			if (rh == null) {
				log.info("Temp RpcChannel {} be removed since {}", rc, reason);
				return;
			}
			// 获取对应的逻辑连接对象，将逻辑通道从当前逻辑连接中移除
			RpcLink lo = linkMap.get(rh);
			if (lo == null) {
				log.debug("RpcLink {} already be removed", rh);
				linkMap.remove(rh);
			} else {
				log.warn("Remove RpcChannel {} from RpcLink {}", rc, rh);
				lo.removeChannel(rc);
				// 检查当前逻辑连接是否还有可用的逻辑通道（通常都没有了）
				if (lo.isConnected()) {
					return;
				}
				log.warn("Last channel of link {} be closed because {}", rh, reason);
				this.removeLink(rh, true, "all channels be closed");
			}
		}
		// 如果当前节点是主节点，发送广播通知
		if (isMaster) {
			List<String> exLt = new ArrayList<String>();
			exLt.add(rh); // 排除断线的节点
			List<String> lt = new ArrayList<String>();
			// 让被通知的节点排除掉自己的已知的连接，避免重复通知
			lt.add(InetConfig.LOCAL_ADDR);
			lt.addAll(linkMap.keySet());
			try {
				RpcMethodConfig mc = RpcContext.getMyMethodConfig(RpcMethodConfig.ID_NODE_OFF);
				mc.setTimeout(RpcConfig.NODE_OFF_TIMEOUT);
				this.broadcast(mc, new Object[] { rh, lt }, exLt);
			} catch (Throwable e) {
				log.warn("Error on broadcast {} node off", rh, e);
			}
			return;
		}
	}

	@Override
	public void run() {
		// 发送nodeOff后不再发送心跳和测试连接
		if (status.get() == STATUS_STOPPED) {
			return;
		}
		// 每次心跳都到注册中心更新
		for (RpcListener listener : listeners) {
			listener.onRegister(rpcContext.getMyRegistry());
		}
		try {
			// 清理过期的临时逻辑通道
			this.clearTmpInChl();
			// 仅仅依靠nodeOff通知，不足以确保连接都被正常断开
			if (isMaster) {
				long d = System.currentTimeMillis() - RpcConfig.HEARTBEAT_PERIOD * 3;
				// 检查已建立的所有连接，心跳超时则关闭这个连接
				for (final String r : linkMap.keySet()) {
					final RpcLink l = linkMap.get(r);
					// 如果连接还处于建立过程中（还未提交注册表），则可能存在心跳超时情况
					if (l.isReady()) {
						if (l.before(d)) {
							this.removeLink(r, true, "heart beat lost");
						} else {
							log.debug("RpcLink {} is ok", r);
						}
					} else {
						log.info("RpcLink {} is not ready", r);
						if (l.before(d)) {
							log.warn("RpcLink {} is removed because timeout", r);
							this.removeLink(r, true, "connection timeout");
						}
					}
				}
				this.checkMasters();// 尝试与其它主节点建立连接
			} else {
				if (Util.strIsNotEmpty(masterHost)) {
					// 非主节点需要向主节点发送心跳
					if (this.heartbeat(masterHost)) {
						return;
					}
					// 如果不成功，则关闭连接，并重新选一个主节点
					this.removeLink(masterHost, true, "heart beat failed");
				}
				this.pickMaster();
			}
		} catch (Throwable e) {
			// 增加异常处理，以防万一出现异常，导致线程退出
			log.error("Error on heart beat check", e);
		}
	}

	protected void checkMasters() {
		// 尝试与其它主节点建立连接
		for (String addr : masterList) {
			if (InetConfig.LOCAL_ADDR.equals(addr)) {
				continue;
			}
			try {
				this.heartbeat(addr);
			} catch (Throwable e) {
				log.warn("Can not link to another master {}", addr);
			}
		}
	}

	protected void clearTmpInChl() {
		long d = System.currentTimeMillis() - RpcConfig.BIND_LINK_TIMEOUT;
		for (String k : tmpInChlMap.keySet()) {
			RpcChannel rc = tmpInChlMap.get(k);
			// 超时则一定会被移走，绑定一定失败
			if (rc.getCreateTime() < d) {
				tmpInChlMap.remove(k);
				if (rc.getRemoteAddr() == null) {
					rc.close("bind link timeout");
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected boolean heartbeat(String rh) {
		// log.debug("Heart beat to {}", rh);
		try {
			// 获取到目的主节点的连接，没有则尝试重新建立，并提交注册表
			RpcLink lo = this.link2(rh);
			if (lo == null) {
				return false;
			}
			if (!lo.isConnected()) {
				return false;
			}
			// My registry Map
			Map<String, RpcRegistry> mm = rpcContext.getRegistryMap();
			// 将我的注册表集合转换为注册表版本号集合，避免传输整个注册表集合
			Map<String, Date> vm = new HashMap<String, Date>();
			for (String k : mm.keySet()) {
				vm.put(k, mm.get(k).getVersion());
			}
			// 向远程主节点发送所有节点注册表的版本号（最后一次更新时间）
			RpcMethodConfig mc = RpcContext.getMyMethodConfig(RpcMethodConfig.ID_HEART_BEAT);
			// 暂时在这里设置心跳超时时间
			mc.setTimeout(RpcConfig.HEARTBEAT_PERIOD);
			Map<String, RpcRegistry> rm = (Map<String, RpcRegistry>) lo.invoke(//
					mc, new Object[] { InetConfig.LOCAL_ADDR, vm });
			// 收得到返回表示对方还活着，更新心跳时间
			lo.setLastBeat(System.currentTimeMillis());
			// 如果对方没有更新的版本，则会返回空map
			if (rm == null || rm.isEmpty()) {
				return true;
			}
			// 如果远程主节点总是返回比当前节点版本更新的注册表集合，反之返回null
			for (RpcRegistry r : rm.values()) {
				log.info("Get updated registry {} from {}", r.getKey(), lo);
				// 更新本地注册表，但不推送给其它节点，等连接“我”的节点通过心跳来取
				this.updateRegistry(r);
			}
			return true;
		} catch (Throwable e) {
			if (!RpcCore.isRunning()) {
				return false;
			}
			if (isMaster) {
				log.warn("Can not heart beat to {}", rh);
				log.debug("Error on heart beat to {}", rh, e);
			} else {
				log.error("Can not heart beat to {}", rh, e);
			}
		}
		return false;
	}

	public boolean isMaster() {
		return isMaster;
	}

	/**
	 * 从注册表rr提取服务提供者的信息，与服务方法的ID
	 * 
	 * @param rr
	 * @return
	 */
	public boolean updateRegistry(RpcRegistry rr) {
		// 与本地对应的注册表版本进行比对
		String remoteAddr = rr.getKey();
		// 防止意外收到自己的注册表
		if (InetConfig.LOCAL_ADDR.equals(remoteAddr)) {
			return false;
		}
		RpcRegistry mr = this.rpcContext.getRegistry(remoteAddr);
		if (mr == null) {
			// 如果本地还没有，则直接开始解析
			log.info("Get new registry from {}", remoteAddr);
		} else {
			// 如果我的版本早于远程版本则需要更新
			if (mr.getVersion().before(rr.getVersion())) {
				log.info("Update registry {} from {} to {}", remoteAddr, //
						Util.date2str(mr.getVersion(), Util.STDDTMF), //
						Util.date2str(rr.getVersion(), Util.STDDTMF));
			} else {
				// 如果本地有，但版本不晚于收到的版本，则什么都不用做
				// log.info("Registry {} is lastest {}", remoteHost, //
				// Util.date2str(mr.getVersion(), Util.STDDTMF));
				return false;
			}
		}
		// 来自远程的serviceMap
		Map<String, RpcServiceConfig> sm = rr.getServiceMap();
		for (String k : sm.keySet()) {
			log.info(k);
		}
		// 从本地注册中取出referenceMap并遍历
		RpcRegistry lr = this.rpcContext.getMyRegistry();
		Map<String, RpcReferenceConfig> rm = lr.getReferenceMap();
		// 遍历本地需要引用的接口服务配置，与远程节点（注册表中）提供的服务进行对比
		for (String refId : rm.keySet()) {
			RpcReferenceConfig rc = rm.get(refId);
			// 根据reference的key，到远程serviceMap中匹配service
			RpcServiceConfig sc = sm.get(rc.getSign());
			log.info("Match reference/service with {}, {}: {} = {}", remoteAddr, refId, rc, sc);
			// 如果匹配成功，表示远程节点是当前接口服务的提供者
			if (rc.match(sc)) {
				this.putProvider(rr, remoteAddr, sc);
				this.registMethods(remoteAddr, sc, rc);
			}
		}
		this.rpcContext.putRegistry(remoteAddr, rr);
		// mr.setVersion(new Date());
		for (RpcListener listener : listeners) {
			listener.onUpdateRegistry(rr);
		}
		return true;
	}

	public void putProvider(RpcRegistry rr, String remoteAddr, RpcServiceConfig sc) {
		String pk = new StringBuilder(sc.getSign())//
				.append("/").append(rr.getTag()).toString();
		// 如果服务状态为不可用，则将远程节点放到禁用的服务提供者列表
		if (sc.isEnable()) {
			this.rpcContext.putProvider(pk, remoteAddr);
		} else {
			this.rpcContext.putProvider2(pk, remoteAddr);
		}
	}

	public void registMethods(String remoteAddr, RpcServiceConfig sc, RpcReferenceConfig rc) {
		Map<String, RpcMethodConfig> smm = sc.getMethodConfigMap();
		RpcStub ref = this.rpcContext.getStub(remoteAddr);
		// 遍历 reference中的方法
		Map<String, RpcMethodConfig> rmm = rc.getMethodConfigMap();
		for (String methodKey : rmm.keySet()) {
			// log.debug("Match method {}", methodKey);
			// service中的方法定义是当前方法的远程定义
			RpcMethodConfig smc = (RpcMethodConfig) smm.get(methodKey);
			// reference中的方法定义是当前方法的本地已有的定义
			RpcMethodConfig rmc = (RpcMethodConfig) rmm.get(methodKey);
			if (rmc == null || smc == null) {
				log.warn("Remote host {} does not match {} on method {}", //
						remoteAddr, InetConfig.LOCAL_ADDR, methodKey);
				continue;
			}
			if (smc.getRelativeId() == null) {
				throw new IllegalStateException(new StringBuilder(smc.getKey())//
						.append(" -> relative id is null").toString());
			}
			// log.debug("Put method id {}={} of {}", rmc.getKey(),
			// smc.getRelativeId(), remoteHost);
			// 由于这个服务的提供者可能有多个，所以以消费端配置为准，仅获取methodId
			ref.putMethodId(rmc.getKey(), smc.getRelativeId());
		}
	}

	/**
	 * 根据方法配置，向与当前相连的节点广播
	 * 
	 * @param mc
	 *            方法配置
	 * @param args
	 *            方法参数
	 * @param exLt
	 *            要排除的节点
	 * @return
	 * @throws Throwable
	 */
	public Map<String, Object> broadcast(RpcMethodConfig mc, Object[] args, List<String> exLt) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String rh : linkMap.keySet()) {
			if ((!InetConfig.LOCAL_ADDR.equals(rh)) && ((exLt == null) || (!exLt.contains(rh)))) {
				log.info("Broadcast {} to {}", mc, rh);
				try {
					// 获取并激活连接，因为linkMap中的连接可能并不可用
					RpcLink lo = this.link2(rh);
					Object ro = lo.invoke(mc, args);
					result.put(rh, ro);
				} catch (Throwable e) {
					result.put(rh, e);
					log.warn("Can not broadcast {} to {}", mc, rh, e);
				}
			}
		}
		return result;
	}

	public void removeLink(String addr, boolean close, String reason) {
		// 移除link时先移除registry
		rpcContext.removeRegistry(addr);
		// 将其从服务提供者列表移除
		rpcContext.removeProvider(addr);
		// 移除stub
		rpcContext.removeStub(addr);
		// 从linkMap中移除并关闭
		RpcLink lo = linkMap.remove(addr);
		if (close && lo != null) {
			lo.close(reason);
		}
		for (RpcListener listener : listeners) {
			listener.onRemoveLink(addr, reason);
		}
	}

	public ExecutorService getThreadPool() {
		return threadPool;
	}

	public void setThreadPool(ExecutorService threadPool) {
		this.threadPool = threadPool;
	}

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	public void setScheduler(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}

	public ExecutorService getIoThreadPool() {
		return ioThreadPool;
	}

	public void setIoThreadPool(ExecutorService ioThreadPool) {
		this.ioThreadPool = ioThreadPool;
	}

	public static int getStatus() {
		return status.get();
	}

	public static boolean isRunning() {
		return status.get() == STATUS_RUNNING;
	}

	public static boolean isStopping() {
		return status.get() == STATUS_STOPPING;
	}

	public static boolean isStopped() {
		return status.get() == STATUS_STOPPED;
	}

	/**
	 * 广播推某节点的注册表到所有已连接的节点
	 * 
	 * @param rr
	 */
	public void pushRegistry(final RpcRegistry rr) {
		final RpcCore rpcCore = this;
		threadPool.execute(new Runnable() {
			@Override
			public void run() {
				List<String> exLt = new ArrayList<String>();
				exLt.add(rr.getKey());
				exLt.add(InetConfig.LOCAL_ADDR);

				List<String> lt = new ArrayList<String>();
				lt.add(InetConfig.LOCAL_ADDR);
				lt.addAll(rpcCore.getLinkMap().keySet());
				try {
					log.debug("Invoke PUSH_REGISTRY with {},{}/{}", rr.getKey(), lt, exLt);
					RpcMethodConfig mc = RpcContext.getMyMethodConfig(RpcMethodConfig.ID_PUSH_REGISTRY);
					mc.setTimeout(RpcConfig.PUSH_REG_TIMEOUT);
					rpcCore.broadcast(mc, new Object[] { rr, lt }, exLt);
				} catch (Throwable e) {
					log.error("Error on broadcast PUSH_REGISTRY with {},{}/{}", rr.getKey(), lt, exLt);
				}
			}
		});
	}

	public static boolean hasLink(String remoteHost) {
		if (Util.strIsEmpty(remoteHost)) {
			return false;
		}
		return linkMap.containsKey(remoteHost);
	}

	public void addListener(RpcListener listener) {
		listeners.add(listener);
	}

	public List<RpcListener> getListeners() {
		return listeners;
	}

	public void setListeners(List<RpcListener> listeners) {
		if (listeners != null) {
			this.listeners = listeners;
		}
	}

	public static String getMasterHost() {
		return masterHost;
	}

	public static int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		RpcCore.tag = tag;
	}

	@Override
	public int getOrder() {
		return PriorityOrdered.HIGHEST_PRECEDENCE;
	}

	/**
	 * 提供给应用使用的动态注册服务引用方法
	 * 
	 * @param rc
	 * @throws Throwable
	 */
	public void registReferece(RpcReferenceConfig rc) throws Throwable {
		RpcRegistry rr = rpcContext.getMyRegistry();
		Map<String, RpcReferenceConfig> rcMap = rr.getReferenceMap();
		if (rcMap.containsKey(rc.getId())) {
			return;
		}
		synchronized (rcMap) {
			if (rcMap.containsKey(rc.getId())) {
				return;
			}
			// 注册远程服务引用
			rpcContext.registReference(rc, rr);
			rr.setVersion(new Date());
			// 发送更新后的注册表给注册中心
			RpcLink lo = this.getLink(RpcCore.getMasterHost());
			lo.popRegistry();
		}
	}

	/**
	 * 提供给应用使用的动态注册服务的方法
	 * 
	 * @param sc
	 * @throws Throwable
	 */
	public void registService(RpcServiceConfig sc) throws Throwable {
		RpcRegistry mr = rpcContext.getMyRegistry();
		Map<String, RpcServiceConfig> scMap = mr.getServiceMap();
		if (scMap.containsKey(sc.getSign())) {
			return;
		}
		synchronized (scMap) {
			if (scMap.containsKey(sc.getSign())) {
				return;
			}
			// 注册服务
			rpcContext.registService(sc, mr);
			mr.setVersion(new Date());
			// 创建本地方法调用动态代理
			new JavassistMaker().makeDynamicInvoker(rpcContext, sc);
			// 发送更新后的注册表给注册中心
			RpcLink lo = this.getLink(RpcCore.getMasterHost());
			lo.popRegistry();
		}
	}
}
