package com.boarsoft.rpc.http.mapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ParamMapper {

	/**
	 * 
	 * @param args
	 * @param index
	 * @param class
	 * @param name
	 * @param value
	 * @param req
	 *            http servlet request
	 * @param rsp
	 *            http servlet response
	 */
	void map(Object[] args, int index, Class<?> clazz, String name, String value, HttpServletRequest req,
			HttpServletResponse rsp);

}
