package com.boarsoft.rpc.bean;

import java.io.Serializable;

import org.dom4j.Node;

import com.boarsoft.common.Util;
import com.boarsoft.rpc.util.XmlConfigUtil;

public class RpcNodeConfig implements Serializable {
	protected static final long serialVersionUID = -238430885029993755L;
	protected String address;
	protected String ip;
	protected int port;
	protected boolean isMaster;

	public RpcNodeConfig() {
	}

	public RpcNodeConfig(Node n) {
		String addr = XmlConfigUtil.getStringAttr(n, "@address", null);
		this.setAddress(addr);
		this.isMaster = XmlConfigUtil.getBooleanAttr(n, "@master", false);
	}

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		if (Util.strIsEmpty(address)) {
			throw new RuntimeException("Node address can not be blank.");
		}
		try {
			String[] a = address.split(":");
			this.ip = a[0];
			this.port = Integer.parseInt(a[1]);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Invalid address ".concat(address));
		}
		this.address = address;
	}

	public boolean isMaster() {
		return this.isMaster;
	}

	public void setMaster(boolean isMaster) {
		this.isMaster = isMaster;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}
}
