package com.boarsoft.rpc.bean;

import java.io.Serializable;
import java.util.List;

import org.dom4j.Node;
import org.springframework.util.StringUtils;

import com.boarsoft.rpc.util.XmlConfigUtil;

public class RpcServiceConfig extends RpcFaceConfig implements Serializable {
	private static final long serialVersionUID = -238430885029993755L;

	protected String ref;
	protected boolean enable = true;
	protected String uri;

	@Deprecated
	public RpcServiceConfig() {
		// 仅为Kryo序列化保留
	}

	public RpcServiceConfig(String group, String name, String interfaceName, String version, String ref)
			throws ClassNotFoundException {
		super(group, name, interfaceName, version, null);
		this.ref = ref;
	}

	@SuppressWarnings("unchecked")
	public RpcServiceConfig(Node sn) throws Exception {
		super(sn);
		this.ref = XmlConfigUtil.getStringAttr(sn, "@ref", this.ref);
		if (StringUtils.isEmpty(this.ref)) {
			throw new IllegalStateException("Service.ref can not be blank.");
		}
		this.enable = XmlConfigUtil.getBooleanAttr(sn, "@enable", this.enable);
		this.uri = XmlConfigUtil.getStringAttr(sn, "@uri", null);
		//
		List<Node> ml = sn.selectNodes("method");
		for (Node m : ml) {
			String name = XmlConfigUtil.getStringAttr(m, "@name", null);
			String ms = this.getMethodSign(m, name);
			RpcMethodConfig mc = this.methodConfigMap.get(ms);
			if (mc == null) {
				throw new IllegalStateException(String.format("Method %s/%s does not exists", sign, ms));
			}
			mc.setType(XmlConfigUtil.getStringAttr(m, "@type", this.type));
			mc.setTimeout(XmlConfigUtil.getIntegerAttr(m, "@timeout", this.timeout));
			mc.setCallback(XmlConfigUtil.getStringAttr(m, "@callback", null));
			mc.setProtocol(XmlConfigUtil.getIntegerAttr(m, "@protocol", 0));
			mc.setAutoMock(XmlConfigUtil.getBooleanAttr(m, "@autoMock", false));
			mc.setUri(XmlConfigUtil.getStringAttr(m, "@uri", "/".concat(m.getName())));
		}
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getRef() {
		return this.ref;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
}
