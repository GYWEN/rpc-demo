package com.boarsoft.rpc.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcServiceConfig;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

public class JavassistMaker {
	private static Logger log = LoggerFactory.getLogger(JavassistMaker.class);
	private Map<String, String> typeMap = new HashMap<String, String>();

	public JavassistMaker() {
		this.typeMap.put("byte", "java.lang.Byte");
		this.typeMap.put("boolean", "java.lang.Boolean");
		this.typeMap.put("short", "java.lang.Short");
		this.typeMap.put("int", "java.lang.Integer");
		this.typeMap.put("long", "java.lang.Long");
		this.typeMap.put("float", "java.lang.Float");
		this.typeMap.put("double", "java.lang.Double");
	}

	public void makeDynamicInvoker(RpcContext rpcContext) throws NotFoundException, CannotCompileException,
			InstantiationException, IllegalAccessException, ClassNotFoundException {
		ClassPool clsPool = ClassPool.getDefault();
		// for web container such as tomcat etc
		clsPool.insertClassPath(new ClassClassPath(this.getClass()));
		// clsPool.insertClassPath(new
		// ClassClassPath(ApplicationContext.class.getClass()));

		// clsPool.importPackage("org.springframework.context.ApplicationContext");
		// clsPool.importPackage("com.boarsoft.rpc.core.DynamicInvoker");
		clsPool.importPackage("com.boarsoft.rpc.core.JavassitMaker");

		for (RpcServiceConfig sc : rpcContext.getLocalServiceConfigMap().values()) {
			this.makeDynamicInvoker(rpcContext, sc, clsPool);
		}
	}

	public void makeDynamicInvoker(RpcContext rpcContext, RpcServiceConfig sc) throws NotFoundException, CannotCompileException,
			InstantiationException, IllegalAccessException, ClassNotFoundException {
		ClassPool clsPool = ClassPool.getDefault();
		this.makeDynamicInvoker(rpcContext, sc, clsPool);
	}

	protected void makeDynamicInvoker(RpcContext rpcContext, RpcServiceConfig sc, ClassPool clsPool) throws NotFoundException,
			CannotCompileException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		StringBuilder sb = new StringBuilder();
		sb.append("Proxy$").append(sc.getRef());
		String clsName = sb.toString();
		log.debug("Creating service proxy class {}", clsName);

		sb.setLength(0);
		sb.append(sc.getInterfaceClazz().getPackage().getName()).append(".").append(clsName);
		CtClass ctCls = clsPool.makeClass(sb.toString());
		ctCls.addInterface(clsPool.getCtClass("com.boarsoft.rpc.core.DynamicInvoker"));

		ctCls.addField(CtField.make("private Object serviceBean;\n", ctCls));
		// sb.setLength(0);
		// sb.append("public void setServiceBean(Object serviceBean) {\n");
		// sb.append(" this.serviceBean = serviceBean;\n");
		// sb.append("}");
		// ctCls.addMethod(CtNewMethod.make(sb.toString(), ctCls));

		ctCls.addField(CtField.make(new StringBuilder().append("private ")//
				.append(ApplicationContext.class.getName()).append(" ctx;").toString(), ctCls));
		sb.setLength(0);
		sb.append("public void setApplicationContext(")//
				.append(ApplicationContext.class.getName()).append(" ctx) {\n");
		sb.append("    this.ctx = ctx;\n");
		sb.append("}");
		ctCls.addMethod(CtNewMethod.make(sb.toString(), ctCls));

		sb.setLength(0);
		sb.append("public Object invoke(int methodId, Object[] args) throws Throwable {\n");

		sb.append("    if (serviceBean == null) {\n");
		sb.append("        serviceBean = ctx.getBean(\"").append(sc.getRef()).append("\");\n");
		sb.append("    }\n");

		sb.append("    ").append(sc.getInterfaceName()).append(" ").append(sc.getRef());
		sb.append(" = (").append(sc.getInterfaceName()).append(") serviceBean;\n");
		sb.append("    switch(methodId) {\n");
		List<Integer> idLt = new ArrayList<Integer>();
		Map<String, RpcMethodConfig> methodConfigMap = sc.getMethodConfigMap();
		for (RpcMethodConfig mc : methodConfigMap.values()) {
			Integer id = mc.getRelativeId();
			idLt.add(id);
			Method m = rpcContext.getMyMethod(id);
			sb.append("    case ").append(id).append(":\n");
			String rt = m.getReturnType().getName();
			if ("void".equals(rt)) {
				sb.append("        ");
			} else if (typeMap.containsKey(rt)) {
				sb.append("        return ").append(typeMap.get(rt)).append(".valueOf(");
			} else {
				sb.append("        return ");
			}
			sb.append(sc.getRef()).append(".").append(m.getName()).append("(");
			Class<?> pta[] = m.getParameterTypes();
			for (int i = 0; i < pta.length; i++) {
				String t = pta[i].getName();
				if (typeMap.containsKey(t)) {
					// 如果参数是基本数据类型，其包装类不可能为null，强制转换即可
					sb.append("((").append(typeMap.get(t)).append(") args[")//
							.append(i).append("]).").append(t).append("Value(), ");
				} else {
					sb.append("(");
					int d = t.lastIndexOf("[") + 1;
					if (d > 0) {
						String a = t.substring(0, d).replaceAll("\\[", "[]");
						t = t.substring(d);
						switch (t) {
						case "B":
							sb.append("byte");
							break;
						case "I":
							sb.append("int");
							break;
						case "S":
							sb.append("short");
							break;
						case "J":
							sb.append("long");
							break;
						case "F":
							sb.append("float");
							break;
						case "D":
							sb.append("double");
							break;
						case "Z":
							sb.append("boolean");
							break;
						default:
							// Lxx.xx.xx; 去掉第一个字符L和最后一个字符;
							t = t.substring(1, t.length() - 1);
							sb.append(t);
							break;
						}
						sb.append(a);
					} else {
						sb.append(t);
					}
					sb.append(") args[").append(i).append("], ");
				}
			}
			if (pta.length > 0) {
				sb.setLength(sb.length() - 2);
			}
			if ("void".equals(rt)) {
				sb.append(");\n        return null;\n");
			} else if (typeMap.containsKey(rt)) {
				sb.append("));\n");
			} else {
				sb.append(");\n");
			}
		}
		sb.append("    default:\n");
		sb.append("        throw new IllegalStateException(");
		sb.append("\"Unknown method id \".concat(String.valueOf(methodId)));\n");
		sb.append("    }\n");
		sb.append("}\n");

		log.debug("\n{}", sb.toString());
		// System.out.println(sb.toString());
		try {
			ctCls.addMethod(CtMethod.make(sb.toString(), ctCls));
		} catch (CannotCompileException e) {
			log.error("Can not make invoker for {}", sc, e);
			return;
		}
		DynamicInvoker invoker = (DynamicInvoker) ctCls.toClass().newInstance();
		// invoker.setServiceBean(rpcContext.getBean(sc.getId(),
		// sc.getInterfaceClazz()));
		invoker.setApplicationContext(rpcContext.getApplicationContext());

		for (Integer id : idLt) {
			rpcContext.putDynamicInvoker(id, invoker);
			// log.debug("Cache dynamic invoker of method {}.", id);
		}
	}

	public static byte obj2byte(Object o) {
		return Byte.parseByte(String.valueOf(o));
	}

	public static boolean obj2boolean(Object o) {
		return Boolean.parseBoolean(String.valueOf(o));
	}

	public static short obj2short(Object o) {
		return Short.parseShort(String.valueOf(o));
	}

	public static int obj2int(Object o) {
		return Integer.parseInt(String.valueOf(o));
	}

	public static long obj2long(Object o) {
		return Long.parseLong(String.valueOf(o));
	}

	public static float obj2float(Object o) {
		return Float.parseFloat(String.valueOf(o));
	}

	public static double obj2double(Object o) {
		return Double.parseDouble(String.valueOf(o));
	}
}
