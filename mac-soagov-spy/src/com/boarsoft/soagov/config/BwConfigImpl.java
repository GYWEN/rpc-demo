package com.boarsoft.soagov.config;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.boarsoft.common.Util;

public class BwConfigImpl implements BwConfig {
	private static final long serialVersionUID = -2340389770664847619L;

	/** 所有限流维度值的组合，比如：当限流维度为IP+机构时，key=10.16.0.161-scrcu */
	protected String key;
	/** 所有限度维度的KEY-VALUE设置 */
	protected Map<String, String> dimMap = new TreeMap<String, String>();
	/** */
	protected boolean black = true;

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof BwConfigImpl) {
			BwConfig c = (BwConfig) o;
			return key.equals(c.getKey());
		}
		return false;
	}

	@Override
	public String toString() {
		return key;
	}

	public BwConfigImpl() {
	}

	public BwConfigImpl(String key2) {
		this.setKey(key);
	}

	public BwConfigImpl(String key, boolean b) {
		this.setKey(key);
		this.black = b;
	}

	@Override
	public boolean setKey(String key) {
		if (Util.strIsEmpty(key)) {
			throw new IllegalArgumentException("Dimension key is emtpy");
		}
		Map<String, String> m = new HashMap<String, String>();
		String[] a = key.split("&");
		for (String d : a) {
			int i = d.indexOf("=");
			if (i > 0) {
				String k = d.substring(0, i);
				String v = d.substring(i + 1);
				m.put(k, v);
			} else {
				throw new IllegalArgumentException("Dimension key is invalid");
			}
		}
		this.key = key;
		this.dimMap = m;
		return true;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Map<String, String> getDimMap() {
		return dimMap;
	}

	@Override
	public boolean isBlack() {
		return black;
	}

	public void setBlack(boolean black) {
		this.black = black;
	}
}
