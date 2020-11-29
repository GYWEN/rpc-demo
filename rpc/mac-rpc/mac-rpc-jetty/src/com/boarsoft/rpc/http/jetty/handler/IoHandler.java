package com.boarsoft.rpc.http.jetty.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcMethodConfig;

public interface IoHandler {
	/**
	 * 
	 * @param mc
	 * @param co
	 * @param target
	 * @param req
	 * @param rsp
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	RpcCall read(RpcMethodConfig mc, RpcCall co, String target, HttpServletRequest req, HttpServletResponse rsp)
			throws IOException, ClassNotFoundException;

	/**
	 * 
	 * @param mc
	 * @param co
	 * @param target
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	void write(RpcMethodConfig mc, RpcCall co, String target, HttpServletRequest req, HttpServletResponse rsp)
			throws IOException;

}
