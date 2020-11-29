package com.boarsoft.common.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InetUtil {
	private static final Logger log = LoggerFactory.getLogger(InetUtil.class);

	public static final String LOCALHOST = "127.0.0.1";
	public static final String ANYHOST = "0.0.0.0";
	private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

	private static volatile InetAddress LOCAL_ADDRESS = null;

	/**
	 * 获取网卡0的IP地址、缓存并返回
	 * 
	 * @return
	 */
	public static InetAddress getLocalAddress() {
		if (LOCAL_ADDRESS != null) {
			return LOCAL_ADDRESS;
		}
		LOCAL_ADDRESS = getLocalAddress0();
		return LOCAL_ADDRESS;
	}

	public static InetAddress getLocalAddress0() {
		Enumeration<NetworkInterface> interfaces = null;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (Throwable e) {
			log.warn("Error on getNetworkInterfaces", e);
		}
		if (interfaces == null) {
			return getLocalAddress();
		}
		while (interfaces.hasMoreElements()) {
			try {
				NetworkInterface network = interfaces.nextElement();
				Enumeration<InetAddress> addresses = network.getInetAddresses();
				if (addresses == null) {
					continue;
				}
				while (addresses.hasMoreElements()) {
					try {
						InetAddress address = addresses.nextElement();
						if (isValidAddress(address)) {
							return address;
						}
					} catch (Throwable e) {
						log.warn("Error on get InetAddress", e);
					}
				}
			} catch (Throwable e) {
				log.warn("Error on get NetworkInterface", e);
			}
		}
		return getLocalHost();
	}

	public static String match(Set<String> ips, List<String> likeLt) {
		String ip = null;
		int high = 0, s = 0;
		for (String like: likeLt) {
			int max = like.length();
			if (max < high) {
				continue;
			}
			for (String a : ips) {
				int l = Math.min(a.length(), max);
				if (l <= high) {
					continue;
				}
				for (s = 0; s < l; s++) {
					if (a.charAt(s) != like.charAt(s)) {
						break;
					}
				}
				if (s > high) {
					high = s;
					ip = a;
				}
			}
		}
		return ip;
	}

	public static Map<String, InetAddress> getLocalAddressMap() {
		Map<String, InetAddress> aMap = new HashMap<String, InetAddress>();
		Enumeration<NetworkInterface> interfaces = null;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (Throwable e) {
			log.warn("Error on getNetworkInterfaces", e);
		}
		if (interfaces == null) {
			return aMap;
		}
		while (interfaces.hasMoreElements()) {
			try {
				NetworkInterface network = interfaces.nextElement();
				Enumeration<InetAddress> addresses = network.getInetAddresses();
				if (addresses == null) {
					continue;
				}
				while (addresses.hasMoreElements()) {
					try {
						InetAddress addr = addresses.nextElement();
						if (isValidAddress(addr)) {
							aMap.put(addr.getHostAddress(), addr);
						}
					} catch (Throwable e) {
						log.warn("Error on get InetAddress", e);
					}
				}
			} catch (Throwable e) {
				log.warn("Error on get NetworkInterface", e);
			}
		}
		return aMap;
	}

	/**
	 * 等同于 InetAddress.getLocalHost()，只是不抛异常
	 * 
	 * @return InetAddress，如果获取失败就返回null
	 */
	public static InetAddress getLocalHost() {
		try {
			return InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			log.error("Error on getLocalHost", e);
		}
		return null;
	}

	public static boolean isValidAddress(InetAddress address) {
		if (address == null || address.isLoopbackAddress())
			return false;
		String name = address.getHostAddress();
		return (name != null && !ANYHOST.equals(name) && !LOCALHOST.equals(name) && IP_PATTERN.matcher(name).matches());
	}

	public static String getHostAddress() {
		return getLocalAddress().getHostAddress();
	}
	
	public static String getHostName() {
		return getLocalAddress().getHostName();
	}
}
