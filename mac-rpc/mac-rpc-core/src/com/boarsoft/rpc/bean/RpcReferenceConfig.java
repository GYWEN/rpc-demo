package com.boarsoft.rpc.bean;

import java.io.Serializable;
import java.util.List;

import org.dom4j.Node;
import org.springframework.util.StringUtils;

import com.boarsoft.rpc.util.XmlConfigUtil;

public class RpcReferenceConfig extends RpcFaceConfig implements Serializable {
	private static final long serialVersionUID = -238430885029993755L;

	@Deprecated
	public RpcReferenceConfig() {
		// 仅为Kryo序列化保留
	}

	public RpcReferenceConfig(String group, String name, String interfaceName, String version, String id)
			throws ClassNotFoundException {
		super(group, name, interfaceName, version, id);
	}

	@SuppressWarnings("unchecked")
	public RpcReferenceConfig(Node rn) throws Exception {
		super(rn);
		// 父类有设置timeout，这里就不用了
		// this.timeout = XmlConfigUtil.getIntegerAttr(rn, "@timeout",
		// this.timeout);
		if (StringUtils.isEmpty(this.id)) {
			throw new RuntimeException("Reference.id can not be blank.");
		}

		List<Node> ml = rn.selectNodes("method");
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
		}
	}
}
