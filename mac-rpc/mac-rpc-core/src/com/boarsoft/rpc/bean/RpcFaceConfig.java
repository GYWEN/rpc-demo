package com.boarsoft.rpc.bean;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.ArrayUtils;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.boarsoft.common.Util;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.util.XmlConfigUtil;

public abstract class RpcFaceConfig implements Serializable {
	private static final Logger log = LoggerFactory.getLogger(RpcFaceConfig.class);

	private static final long serialVersionUID = 3975079833022797904L;
	protected String id;
	protected String group = "group";
	protected String name = "name";
	protected String interfaceName;
	protected String version;
	// protected Class<?> interfaceClazz;
	protected Integer timeout = 30000;
	protected String type = "SC";
	/** 接口签名（group/name/interface/version） */
	protected String sign;
	/** k: 方法KEY, v: RpcMethodConfig */
	protected Map<String, RpcMethodConfig> methodConfigMap = new ConcurrentHashMap<String, RpcMethodConfig>();
	/** 用于模拟实际调用的bean，为null是表示使用通用模拟器 */
	protected String mocker;
	/** 是否在异常时自动调用mocker */
	protected boolean autoMock = false;

	/** 冗余，不传输，目的是方便直接根据Method对象获取RpcMethodConfig */
	protected transient final Map<Method, RpcMethodConfig> methodConfigs = //
			new ConcurrentHashMap<Method, RpcMethodConfig>();

	public RpcFaceConfig() {
		// 仅为Kryo序列化保留
	}

	@Override
	public String toString() {
		return sign;
	}

	public RpcFaceConfig(String group, String name, String interfaceName, String version, String id)
			throws ClassNotFoundException {
		this.group = group;
		this.name = name;
		this.interfaceName = interfaceName;
		this.version = version;
		this.id = id;
		this.init();
	}

	public RpcFaceConfig(Node fn) throws Exception {
		this.id = XmlConfigUtil.getStringAttr(fn, "@id", this.id);
		this.group = XmlConfigUtil.getStringAttr(fn, "@group", this.group);
		this.name = XmlConfigUtil.getStringAttr(fn, "@name", this.name);
		this.interfaceName = XmlConfigUtil.getStringAttr(fn, "@interface", this.interfaceName);
		if (StringUtils.isEmpty(this.interfaceName)) {
			throw new RuntimeException("Interface name can not be blank.");
		}
		this.version = XmlConfigUtil.getStringAttr(fn, "@version", this.version);
		this.timeout = XmlConfigUtil.getIntegerAttr(fn, "@timeout", this.timeout);
		this.type = XmlConfigUtil.getStringAttr(fn, "@type", this.type);
		// 模拟器（bean）ID
		this.mocker = XmlConfigUtil.getStringAttr(fn, "@mocker", this.mocker);
		this.autoMock = XmlConfigUtil.getBooleanAttr(fn, "@autoMock", this.autoMock);
		this.init();
	}

	@SuppressWarnings("unchecked")
	protected String getMethodSign(Node node, String name) {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("(");
		List<Node> al = node.selectNodes("arg");
		if (al.size() > 0) {
			for (int i=0; i<al.size(); i++) {
				Node a = al.get(i);
				String pt = XmlConfigUtil.getStringAttr(a, "@type", null);
				sb.append(pt).append(",");
//				String nm = XmlConfigUtil.getStringAttr(a, "@name", null);
//				if (Util.strIsEmpty(nm)) {
//					continue;
//				}
//				mc.nameArgument(i, nm);
			}
			sb.setLength(sb.length() - 1);
		}
		sb.append(")");
		return sb.toString();
	}

	public Class<?> getInterfaceClazz() throws ClassNotFoundException {
		return Class.forName(this.interfaceName);
	}

	protected void init() throws ClassNotFoundException {
		String[] va = this.version.split("\\.");
		va = (String[]) ArrayUtils.subarray(va, 0, //
				Math.min(RpcConfig.SERVICE_VERSION_MATCH, va.length));
		String ver = Util.array2str(va, ".");
		this.sign = new StringBuilder().append(this.group).append("/")//
				.append(name).append("/").append(this.interfaceName)//
				.append("/").append(ver).toString();
		log.debug("Init RpcFaceConfig {}", this.sign);
		try {
			Class<?> interfaceClazz = this.getInterfaceClazz();
			Method[] mA = interfaceClazz.getMethods();
			int j = mA.length;
			for (int i = 0; i < j; i++) {
				Method m = mA[i];
				RpcMethodConfig rm = new RpcMethodConfig(this, m);
				// rm.setProtocol(protocol);
				log.debug("Put method {} of face {}", m, this);
				// 根据方法签名缓存RpcMethodConfig
				this.methodConfigMap.put(rm.getSign(), rm);
				// 冗余，根据方法对象缓存RpcMethodConfig
				this.methodConfigs.put(m, rm);
			}
		} catch (ClassNotFoundException e) {
			log.warn("Ignore {} because {} is not found", this.sign, this.interfaceName);
		}
	}

	public RpcMethodConfig getMethodConfig(Method method) {
		// String ms = ReflectUtil.getMethodSign(method);
		// String mk = new
		// StringBuilder(this.sign).append("/").append(ms).toString();
		// return this.methodConfigMap.get(ms);
		// 根据方法返回RpcMethodConfig，而不需要使用反射来获取方法签名
		return this.methodConfigs.get(method);
	}

	public boolean match(RpcFaceConfig fc) {
		if (fc == null) {
			return false;
		}
		return this.sign.equalsIgnoreCase(fc.getSign());
	}

	public String getGroup() {
		return this.group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getInterfaceName() {
		return this.interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Integer getTimeout() {
		return this.timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public Map<String, RpcMethodConfig> getMethodConfigMap() {
		return this.methodConfigMap;
	}

	public void setMethodConfigMap(Map<String, RpcMethodConfig> methodConfigMap) {
		this.methodConfigMap = methodConfigMap;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMocker() {
		return mocker;
	}

	public void setMocker(String mocker) {
		this.mocker = mocker;
	}

	public boolean isAutoMock() {
		return autoMock;
	}

	public void setAutoMock(boolean autoMock) {
		this.autoMock = autoMock;
	}
}
