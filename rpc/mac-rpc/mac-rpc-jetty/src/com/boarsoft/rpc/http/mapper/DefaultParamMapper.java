package com.boarsoft.rpc.http.mapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;

public class DefaultParamMapper implements ParamMapper {

	@Override
	public void map(Object[] args, int index, Class<?> clazz, String name, String value, HttpServletRequest req,
			HttpServletResponse rsp) {
		switch (clazz.getName()) {
		case "javax.servlet.http.HttpServletRequest":
			args[index] = req;
			break;
		case "javax.servlet.http.HttpServletResponse":
			args[index] = rsp;
			break;
		case "javax.servlet.http.HttpSession":
			args[index] = req.getSession();
			break;
		case "byte":
		case "java.lang.Byte":
			args[index] = Byte.parseByte(value);
			break;
		case "boolean":
		case "java.lang.Boolean":
			args[index] = Boolean.parseBoolean(value);
			break;
		case "short":
		case "java.lang.Short":
			args[index] = Short.parseShort(value);
			break;
		case "int":
		case "java.lang.Integer":
			args[index] = Integer.parseInt(value);
			break;
		case "long":
		case "java.lang.Long":
			args[index] = Long.parseLong(value);
			break;
		case "float":
		case "java.lang.Float":
			args[index] = Float.parseFloat(value);
			break;
		case "double":
		case "java.lang.Double":
			args[index] = Double.parseDouble(value);
			break;
		default:
			args[index] = JSON.parseObject(value, clazz);
			break;
		}
	}

}
