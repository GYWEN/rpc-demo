package com.boarsoft.common.bean;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.Util;
import com.boarsoft.common.util.InetUtil;

public class InetConfig {
	private static Logger log = LoggerFactory.getLogger(InetConfig.class);
	private static Properties prop;

	public static String LOCAL_IP;
	public static int LOCAL_PORT;
	public static String LOCAL_ADDR;

	static {
		if (prop == null) {
			InputStream is = InetConfig.class.getClassLoader().getResourceAsStream("conf/config.properties");
			if (is == null) {
				log.warn("Can not read conf/config.properties");
			} else {
				log.info("Init InetConfig with default config.properties");
				InetConfig.init(is);
			}
		}
	}

	public static synchronized void init(InputStream is) {
		if (prop == null) {
			prop = new Properties();
			try {
				prop.load(is);
				InetConfig.setAddr(prop.getProperty("my.ip"), prop.getProperty("my.port"));
			} catch (IOException e) {
				log.error("Error on load system config.", e);
				throw new RuntimeException(e);
			}
		}
	}

	public static void setAddr(String ip, int port) throws UnknownHostException, SocketException {
		if (Util.strIsEmpty(ip)) {
			if (Util.strIsEmpty(LOCAL_IP)) {
				LOCAL_IP = InetUtil.getLocalAddress().getHostAddress();
				// LOCAL_IP = InetUtil.getLocalAddress().getHostName();
			}
		} else {
			LOCAL_IP = ip;
		}
		// LOCAL_IP = Util.strIsEmpty(ip) ? InetConfig.LOCAL_IP : ip;
		// LOCAL_PORT = Util.str2int(port, InetConfig.LOCAL_PORT);
		// LOCAL_ADDR = String.format("%s:%s", LOCAL_IP, port);
		LOCAL_PORT = port;
		LOCAL_ADDR = String.format("%s:%d", LOCAL_IP, port);
	}

	public static void setAddr(String ip, String port) throws UnknownHostException, SocketException {
		InetConfig.setAddr(ip, Util.str2int(port, InetConfig.LOCAL_PORT));
	}

	public static void setIp(String ip) throws UnknownHostException, SocketException {
		InetConfig.setAddr(ip, InetConfig.LOCAL_PORT);
	}
}
