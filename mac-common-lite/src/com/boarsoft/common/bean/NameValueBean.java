package com.boarsoft.common.bean;

public class NameValueBean {
	private String name;
	private String value;
	private boolean encode = true;

	public NameValueBean(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public NameValueBean(String name, String value, boolean encode) {
		this.name = name;
		this.value = value;
		this.encode = encode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isEncode() {
		return encode;
	}

	public void setEncode(boolean encode) {
		this.encode = encode;
	}
}