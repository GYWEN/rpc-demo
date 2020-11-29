package com.boarsoft.rpc.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.boarsoft.common.Util;
import com.boarsoft.common.bean.InetConfig;
import com.boarsoft.common.util.RandomUtil;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcInvoking;
import com.boarsoft.rpc.bean.RpcMethod;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcReferenceConfig;
import com.boarsoft.rpc.bean.RpcRegistry;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.bean.RpcStub;
import com.boarsoft.rpc.spy.RpcSvcInput;
import com.boarsoft.rpc.spy.RpcSvcSpy;
import com.boarsoft.soagov.spy.SvcSpy;

@Component("rpcContext")
public class RpcContext implements BeanFactoryAware, Runnable, ApplicationContextAware {
	private final static Logger log = LoggerFactory.getLogger(RpcContext.class);

	/** */
	protected DefaultListableBeanFactory proxyFactory;
	/** */
	protected ConfigurableApplicationContext applicationContext;
	/** k: addr, v: RpcRegistry */
	protected final Map<String, RpcRegistry> registryMap = new ConcurrentHashMap<String, RpcRegistry>();
	/** k: serviceKey, v: node address list */
	protected final Map<String, List<String>> providerMap = new ConcurrentHashMap<String, List<String>>();
	/** */
	protected final Map<String, List<String>> providerMap2 = new ConcurrentHashMap<String, List<String>>();
	/** k: addr, 用于保存引用的调用次数？ */
	protected final Map<String, RpcStub> stubMap = new ConcurrentHashMap<String, RpcStub>();
	/** 服务端方法注册信息 */
	protected final Map<Integer, Method> methodMap = new HashMap<Integer, Method>();
	/** 服务端方法调用器（动态代理） */
	protected final Map<Integer, DynamicInvoker> dynamicInvokerMap = new HashMap<Integer, DynamicInvoker>();
	/** 服务端方法配置 */
	protected static final Map<Integer, RpcMethodConfig> methodConfigMap = new HashMap<Integer, RpcMethodConfig>();
	/** 普通服务方法ID从100起步 */
	protected final AtomicInteger methodIdSequence = new AtomicInteger(100);
	/** 用于在接收到RPC请求后，暂存RpcChannel，以便服务提供者获取 */
	protected static final ThreadLocal<RpcChannel> currChlTL = new ThreadLocal<RpcChannel>();
	/** 用于在发起RPC请求前，指定远程地址或节点分组 */
	protected static final ThreadLocal<Stack<String>> specify2TL = new ThreadLocal<Stack<String>>();
	/** 需要额外传递的键值对 */
	protected static final ThreadLocal<Properties> attachmentTL = new ThreadLocal<Properties>();

	/** */
	protected final ConcurrentMap<String, RpcInvoking> invokingMap = new ConcurrentHashMap<String, RpcInvoking>();
	/** */
	@Autowired(required = false)
	protected RpcSvcSpy svcSpy;

	@SuppressWarnings("unchecked")
	public void init() throws Exception {
		// 创建“我的”注册表
		RpcRegistry myReg = new RpcRegistry();
		registryMap.put(myReg.getKey(), myReg);
		// 注册已内置的，默认引用的服务
		registReference(new RpcReferenceConfig("mac-rpc", "rpcKeeper" //
				, RpcNode.class.getName(), RpcConfig.VERSION, "rpcKeeperRef"), myReg);
		// 注册已内置的，默认暴露的服务
		registService(new RpcServiceConfig("mac-rpc", "rpcKeeper" //
				, RpcNode.class.getName(), RpcConfig.VERSION, "rpcKeeper"), myReg);

		// 解析consume.xml，注册要引用的服务
		ClassLoader cl = RpcConfig.class.getClassLoader();
		log.info("Read {}", cl.getResource("conf/consume.xml"));
		try {
			Document cd = new SAXReader().read(cl.getResourceAsStream("conf/consume.xml"));
			List<Node> csnl = cd.selectNodes("/references/reference");
			for (Node sn : csnl) {
				// 将当前XML配置节点转换为服务引用配置对象并注册到“我的”注册表
				this.registReference(new RpcReferenceConfig(sn), myReg);
			}
		} catch (Exception e) {
			log.error("Error on read consume.xml", e);
			return;
		}

		// 解析provide.xml，注册要暴露的服务
		log.info("Read {}", cl.getResource("conf/provide.xml"));
		try {
			Document pd = new SAXReader().read(RpcConfig.class.getClassLoader().getResourceAsStream("conf/provide.xml"));
			List<Node> psnl = pd.selectNodes("/services/service");
			for (Node sn : psnl) {
				// 将当前XML配置节点转换为服务暴露配置对象并注册到“我的”注册表
				this.registService(new RpcServiceConfig(sn), myReg);
			}
		} catch (Exception e) {
			log.error("Error on read provide.xml", e);
			return;
		}

		// 为所有要暴露的服务创建服务暴露代理
		new JavassistMaker().makeDynamicInvoker(this);
	}

	@Override
	public void run() {
		// 定时执行，清除超时的调用（只针对异步调用，参见RpcLink的invoke方法）
		for (RpcInvoking ri : invokingMap.values()) {
			// 异步类型的调用会返回Future，但应用代码并一定去get它，导致无法移除
			RpcMethodConfig mc = ri.getMethodConfig();
			// 以下类型的调都是返回Future，且不一定会get的
			switch (mc.getType()) {
			case RpcMethodConfig.TYPE_ASYNC_CALL:
			case RpcMethodConfig.TYPE_ASYNC_NOTICE:
			case RpcMethodConfig.TYPE_ASYNC_BROADCAST:
			case RpcMethodConfig.TYPE_BROADCAST_NOTICE:
				log.debug("Check timeout RpcInvoking {}/{}", ri.getKey(), ri.getMethodConfig().getTimeout());
				if (ri.isTimeout()) {
					invokingMap.remove(ri.getKey());
					ri.cancel(true);
					log.warn("Remove timeout RpcInvoking {}", ri.getKey());
				}
				break;
			}
		}
	}

	/**
	 * 用于指定远程服务提供者
	 * 
	 * @param value
	 *            远程节点地址 或 分组（tag）
	 */
	public static void specify2(String value) {
		Stack<String> s = specify2TL.get();
		if (s == null) {
			if (Util.strIsEmpty(value)) {
				return;
			}
			s = new Stack<String>();
			specify2TL.set(s);
		}
		if (Util.strIsEmpty(value)) {
			s.pop();
			return;
		}
		s.push(value);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.proxyFactory = (DefaultListableBeanFactory) beanFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}

	public void registService(RpcServiceConfig sc, RpcRegistry reg) throws Exception {
		log.debug("Regist service {}", sc.getSign());
		// 将当前服务暴露配置放到“我的”注册表
		reg.getServiceMap().put(sc.getSign(), sc);
		// 获取并遍历服务接口所声明的所有服务方法
		Method[] mA = sc.getInterfaceClazz().getMethods();
		for (Method m : mA) {
			Integer id = null;
			// 从服务暴露配置对象中取得某个服务方法的具体配置
			RpcMethodConfig mc = sc.getMethodConfig(m);
			// 支持注解方式为方法指定ID和超时时间（目前仅对本内部方法使用）
			if (m.isAnnotationPresent(RpcMethod.class)) {
				RpcMethod a = (RpcMethod) m.getAnnotation(RpcMethod.class);
				id = a.id();
				mc.setType(a.type());
				mc.setTimeout(a.timeout());
			} else {
				// 如果此方法使用注解标注（普通的服务方法），则为它生成一个ID
				id = this.methodIdSequence.getAndIncrement();
			}
			// 使用注解的情况下，ID是可能出现重复的，需要检查
			log.debug("Registry method {} with id {}", mc, id);
			if ((id == null) || (this.methodMap.containsKey(id))) {
				throw new IllegalStateException("Method id is duplicated or null.");
			}
			// 将生成的ID塞到服务方法配置对象中（此对象已位于注册表）
			mc.setRelativeId(id);
			// 缓存方法ID与方法（method对象）之间的关系，方便后面的使用
			this.methodMap.put(id, m);
			// 缓存方法ID与方法配置，以便通过方法ID能获取到方法的配置
			methodConfigMap.put(id, mc);
		}
	}

	/**
	 * 为指定的服务引用配置注册相应的ReferenceFactory类型的Bean
	 * 
	 * @param rc
	 * @param rr
	 * @throws ClassNotFoundException
	 * @throws SecurityException
	 */
	public void registReference(RpcReferenceConfig rc, RpcRegistry rr) throws SecurityException, ClassNotFoundException {
		log.info("Regist reference with config {}", rc);
		// 如果方法有配置注解，读取注解上的配置
		Method[] mA = rc.getInterfaceClazz().getMethods();
		for (Method m : mA) {
			RpcMethodConfig mc = rc.getMethodConfig(m);
			if (m.isAnnotationPresent(RpcMethod.class)) {
				RpcMethod a = (RpcMethod) m.getAnnotation(RpcMethod.class);
				mc.setType(a.type());
				mc.setTimeout(a.timeout());
			}
		}
		// 创建bean定义
		BeanDefinitionBuilder bdb = BeanDefinitionBuilder.genericBeanDefinition(RpcReferenceFactory.class);
		// 添加属性值或引用注入
		bdb.addPropertyValue("referenceConfig", rc);
		bdb.addPropertyReference("rpcCore", "rpcCore");
		bdb.addPropertyReference("rpcContext", "rpcContext");
		bdb.addPropertyValue("svcSpy", svcSpy);
		// 注册，如果已经注册了同名的bean，移除它
		AbstractBeanDefinition abd = bdb.getRawBeanDefinition();
		// if (proxyFactory.containsBeanDefinition(rc.getId())) {
		// log.warn("Remove existing bean and definition {} before register new
		// one", rc.getId());
		// proxyFactory.destroyBean(rc.getId());
		// proxyFactory.removeBeanDefinition(rc.getId());
		// }
		proxyFactory.registerBeanDefinition(rc.getId(), abd);
		// 将此引用配置放入注册表
		rr.getReferenceMap().put(rc.getId(), rc);
	}

	public RpcRegistry getRegistry(String addr) {
		return this.registryMap.get(addr);
	}

	public RpcRegistry getMyRegistry() {
		return this.registryMap.get(InetConfig.LOCAL_ADDR);
	}

	public synchronized void putProvider(String serviceKey, String addr) {
		log.info("Add enabled provider {}/{}", serviceKey, addr);
		List<String> lt = this.getProvidersByKey(serviceKey);
		// synchronized (this.providerMap) {
		if (lt.contains(addr)) {
			return;
		}
		lt.add(addr);
		// }
	}

	public void putProvider2(String serviceKey, String addr) {
		log.info("Add disabled provider {}/{}", serviceKey, addr);
		List<String> lt = this.getDisabledProviders(serviceKey);
		synchronized (lt) {
			if (lt.contains(addr)) {
				return;
			}
			lt.add(addr);
		}
	}

	public String getProvider(String referenceKey) {
		int tag = 0;
		// 如果通过RpcContext.specify2方法指定了具体的服务提供者，则返回该服务提供者
		Stack<String> stack = specify2TL.get();
		if (stack == null || stack.isEmpty()) {
			tag = RpcCore.getTag();
		} else {
			String s = stack.peek(); // peek addr or tag
			if (Util.strIsEmpty(s)) {
				tag = RpcCore.getTag();
			} else if (s.contains(":")) {
				return s;
			} else {
				tag = Integer.parseInt(s);
			}
		}
		return this.getProvider(referenceKey, tag);
	}

	public String getProvider(String referenceKey, int tag) {
		// 根据环境变量tag修改referenceKey以获取匹配的服务提供者
		StringBuilder sb = new StringBuilder(referenceKey);
		sb.append("/").append(tag);
		// 获取当前服务已知的所有服务提供者
		List<String> lt = this.getProvidersByKey(sb.toString());
		// 如果没有同Tag的服务提供者，尝试去调公共的服务提供者（tag==0）
		if (lt.isEmpty() && tag != 0) {
			sb.setLength(0);
			sb.append(referenceKey).append("/0");
			lt = this.getProvidersByKey(sb.toString());
		}
		int s = lt.size();
		if (s == 0) {
			return null;
		}
		// 随机选择一个服务提供者
		if (s == 1) {
			return lt.get(0);
		}
		return lt.get(RandomUtil.random(0, s));
	}

	public List<String> getProvidersByKey(String referenceKey) {
		List<String> lt = this.providerMap.get(referenceKey);
		if (lt == null) {
			synchronized (this.providerMap) {
				lt = this.providerMap.get(referenceKey);
				if (lt == null) {
					lt = new CopyOnWriteArrayList<String>();
					this.providerMap.put(referenceKey, lt);
				}
			}
		}
		return lt;
	}

	public List<String> getProvidersById(String id) {
		RpcRegistry myReg = this.getMyRegistry();
		RpcReferenceConfig rc = myReg.getReferenceMap().get(id);
		return this.getProvidersByKey(rc.getSign());
	}

	public Map<String, RpcRegistry> getRegistryMap() {
		return this.registryMap;
	}

	public RpcStub getStub(String addr) {
		RpcStub r = stubMap.get(addr);
		if (r == null) {
			synchronized (stubMap) {
				r = stubMap.get(addr);
				if (r == null) {
					r = new RpcStub(addr);
					stubMap.put(addr, r);
				}
			}
		}
		return r;
	}

	public void removeStub(String addr) {
		stubMap.remove(addr);
	}

	public static RpcChannel getCurrInChl() {
		return RpcContext.currChlTL.get();
	}

	public void putInvoking(RpcInvoking ri) {
		this.invokingMap.put(ri.getKey(), ri);
	}

	public RpcInvoking removeInvoking(String remoteHost, RpcCall co) {
		return invokingMap.remove(RpcInvoking.makeKey(remoteHost, co));
	}

	public void removeInvoking(RpcInvoking invoking) {
		invokingMap.remove(invoking.getKey());
	}

	public void putRegistry(String remoteHost, RpcRegistry rr) {
		this.registryMap.put(remoteHost, rr);
	}

	/**
	 * 服务端服务方法：调用本地bean的方法，将结果放入RpcCall
	 * 
	 * @param co
	 *            RPC请求对象
	 * @param mc
	 *            被调用的服务方法的配置
	 * @param rc
	 *            收到此RPC请求的逻辑通道（用于向调用方返回执行结果）
	 * @param remoteAddr
	 *            调用方的地址
	 */
	public void invoke(RpcCall co, RpcMethodConfig mc, RpcChannel rc, String remoteAddr) {
		int cr = SvcSpy.CHECK_PASS;
		// 如果有注入服务治理插件，且当前方法不是框架的内部服务方法，则执行服务治理的检查动作
		if (svcSpy != null && mc.getRelativeId() > 99) {
			// 默认的服务治理接口的实现类是RpcSvcSpyImpl
			cr = svcSpy.check(new RpcSvcInput(co, mc, remoteAddr));
		}
		// 根据服务治理接口的返回值，确定一下步的动作
		switch (cr) {
		case SvcSpy.CHECK_PASS:
			// 如果服务治理检查通过，先让进行中的远程调用计数加一
			DynamicInvoker.count.incrementAndGet();
			// 将当前逻辑通道与当前线程绑定，以便必要时，后续代码能获取到
			currChlTL.set(rc);
			// 登记traceId
			attachmentTL.set(co.getAttachments());
			MDC.put("traceId", co.getTraceId());
			try {
				// 获取RPC请求参数中的方法ID
				int methodId = co.getMethodId();
				// 根据方法ID获取容器启动时缓存起来的服务方动态代理（DynamicInvoker）
				DynamicInvoker di = this.dynamicInvokerMap.get(methodId);
				// 检查是否配置了
				Object ro = null;
				// 容器启动时使用Javassist为每一个服务接口都创建了一个动态代理，并按方法ID缓存
				ro = di.invoke(methodId, co.getArguments());
				// 将执行结果放入RpcCall对象，稍后返回
				co.setResult(ro);
				return;
			} catch (Throwable e) {
				log.error("Error on invoke {}", mc, e);
				// 调用异常时，如果有配置服务端模拟器，自动调用模拟器
				if (mc.isAutoMock() && svcSpy != null) {
					// 如果同时有配置svcSpy和mocker，则模拟器生效
					log.error("Error on invoke method {}, try mocker instead", mc, e);
					co.setResult(svcSpy.mock(co, mc));
				} else {
					// 如果出现异常，也将异常放入RpcCall对象
					co.setThrowable(e);
				}
			} finally {
				attachmentTL.remove();
				// 取消traceId
				MDC.remove("traceId");
				// 将当前逻辑通道与当前线程解绑
				currChlTL.remove();
				// 进行中的远程调用计数减一
				DynamicInvoker.count.decrementAndGet();
			}
			break;
		case SvcSpy.CHECK_MOCK:
			// 如果服务治理要求模拟结果，调用服务治理插件提供的模拟器
			svcSpy.mock(co, mc);
			// 模拟结果将被塞入co中返回
			break;
		case SvcSpy.CHECK_OFF:
			// 如果服务被关闭，抛出拒绝执行的异常
			co.setThrowable(new RejectedExecutionException());
			break;
		case SvcSpy.CHECK_SLA:
			// 如果服务TPS达到上限，抛出访问超限的异常
			co.setThrowable(new IllegalAccessException("Access be limited"));
			break;
		case SvcSpy.CHECK_BLACK:
			// 如果被黑名单限制，不做任何回应
			break;
		default:
			// 任何其它未知情况，都不执行
			log.error("Unknow check result: {}", cr);
			break;
		}
	}

	public Map<String, RpcServiceConfig> getLocalServiceConfigMap() {
		return this.getMyRegistry().getServiceMap();
	}

	public ConfigurableApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 返回服务端方法配置（包括框架内约定了methodId的方法）方法配置
	 * 
	 * @param methodId
	 * @return
	 */
	public static RpcMethodConfig getMyMethodConfig(Integer myMethodId) {
		return methodConfigMap.get(myMethodId);
	}

	public static void putMyMethodId(int id, RpcMethodConfig mc) {
		methodConfigMap.put(id, mc);
	}

	public Method getMyMethod(int methodId) {
		return this.methodMap.get(methodId);
	}

	/**
	 * 当节点停机时，该节点作为服务提供者被移除
	 * 
	 * @param addr
	 */
	public void removeProvider(String remoteHost) {
		for (String serviceKey : this.providerMap.keySet()) {
			List<String> lt = this.getProvidersByKey(serviceKey);
			if (lt.remove(remoteHost)) {
				log.info("Remove enabled provider {} on {}", remoteHost, serviceKey);
			}
			List<String> bl = this.getDisabledProviders(serviceKey);
			if (bl.remove(remoteHost)) {
				log.info("Remove disabled provider {} on {}", remoteHost, serviceKey);
			}
		}
	}

	protected List<String> getDisabledProviders(String key) {
		List<String> lt = this.providerMap2.get(key);
		if (lt == null) {
			synchronized (this.providerMap2) {
				lt = this.providerMap2.get(key);
				if (lt == null) {
					lt = new CopyOnWriteArrayList<String>();
					this.providerMap2.put(key, lt);
				}
			}
		}
		return lt;
	}

	/**
	 * 在本地禁止对某个远程节点的所有服务的访问
	 * 
	 * @param remoteHost
	 */
	public void disableProviders(String remoteHost) {
		for (String serviceKey : this.providerMap.keySet()) {
			this.disableProvider(remoteHost, serviceKey);
		}
	}

	/**
	 * 在本地禁止对某个远程节点的某个服务的访问
	 * 
	 * @param remoteHost
	 * @param serviceKey
	 */
	public void disableProvider(String remoteHost, String serviceKey) {
		log.info("Disable provider {} on {}", remoteHost, serviceKey);
		List<String> lt = this.getProvidersByKey(serviceKey);
		if (lt.remove(remoteHost)) {
			List<String> bl = this.getDisabledProviders(serviceKey);
			bl.add(remoteHost);
		}
	}

	/**
	 * 在本地禁止对所有远程节点的某个服务的访问<br>
	 * 
	 * @param serviceKey
	 */
	public void disableService(String serviceKey) {
		List<String> lt = this.providerMap.remove(serviceKey);
		if (lt == null) {
			return;
		}
		this.providerMap2.put(serviceKey, lt);
	}

	/**
	 * 在本地放开对某个远程节点的所有服务的访问
	 * 
	 * @param remoteHost
	 * @param serviceKey
	 */
	public void enableProviders(String remoteHost) {
		for (String serviceKey : this.providerMap2.keySet()) {
			this.enableProvider(remoteHost, serviceKey);
		}
	}

	/**
	 * 在本地放开对某个远程节点的指定服务的访问
	 * 
	 * @param remoteHost
	 * @param serviceKey
	 */
	public void enableProvider(String remoteHost, String serviceKey) {
		log.info("Enable provider {} on {}", remoteHost, serviceKey);
		List<String> bl = (List<String>) this.providerMap2.get(serviceKey);
		if (bl != null && bl.remove(remoteHost)) {
			List<String> lt = this.getProvidersByKey(serviceKey);
			lt.add(remoteHost);
		}
	}

	/**
	 * 在本地放开对所有远程节点的某个服务的访问<br>
	 * 
	 * @param serviceKey
	 */
	public void enableService(String serviceKey) {
		List<String> lt = this.providerMap2.remove(serviceKey);
		if (lt == null) {
			return;
		}
		this.providerMap.put(serviceKey, lt);
	}

	public void removeRegistry(String addr) {
		registryMap.remove(addr);
	}

	public void putDynamicInvoker(Integer id, DynamicInvoker invoker) {
		dynamicInvokerMap.put(id, invoker);
	}

	public boolean containsBean(String name) {
		return applicationContext.containsBean(name);
	}

	public <T> T getBean(String name, Class<T> clazz) {
		return applicationContext.getBean(name, clazz);
	}

	public Map<String, List<String>> getProviderMap() {
		return providerMap;
	}

	public Map<String, List<String>> getProviderMap2() {
		return providerMap2;
	}

	public Map<String, RpcStub> getStubMap() {
		return stubMap;
	}

	/**
	 * 判断是否有正在执行的RPC调用（无论是调用远程方法和被服务方法被远程调用）
	 * 
	 * @return
	 */
	public boolean hasInvoking() {
		// 优雅停机日志：DynamicInvoker.count则是正在被调用的服务方法的计数
		log.info("Invoking out {} / in {}", invokingMap.size(), DynamicInvoker.count.get());
		return !invokingMap.isEmpty() || DynamicInvoker.count.get() > 0;
	}

	public Map<String, RpcInvoking> getInvokingMap() {
		return invokingMap;
	}

	public <T> void putAtt(String key, T value) {
		Properties p = attachmentTL.get();
		if (p == null) {
			p = new Properties();
			attachmentTL.set(p);
		}
		p.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T removeAtt(String key) {
		Properties p = attachmentTL.get();
		if (p == null) {
			return null;
		}
		return (T) p.remove(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAtt(String key, T _default) {
		Properties p = attachmentTL.get();
		if (p == null) {
			return _default;
		}
		return (T) p.getOrDefault(key, _default);
	}

	public void clearAtt() {
		Properties p = attachmentTL.get();
		if (p == null) {
			return;
		}
		p.clear();
	}

	public Properties getAttachments() {
		return attachmentTL.get();
	}

}
