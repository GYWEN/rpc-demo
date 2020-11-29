package com.boarsoft.rpc.demo;

import java.io.Serializable;

public class User implements Serializable {
	private static final long serialVersionUID = -6569043791710513624L;

	protected String name;
	
	public User() {
	}

	public User(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
