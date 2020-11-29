package com.boarsoft.soagov.health;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.Util;
import com.boarsoft.common.util.StreamUtil;
import com.boarsoft.config.core.Configable;

public class HttpHealthChkImpl implements HealthChecker, Runnable, Configable {
	private static final Logger log = LoggerFactory.getLogger(HttpHealthChkImpl.class);

	/** { Key：服务编号-服务提供者版本号，Value："服务提供者地址1,服务提供者地址2" } */
	protected Map<String, List<String>> providerMap;

	/** { Key：服务编号-服务提供者版本号，Value：{ Key："服务提供者地址1", Value："40" } } */
	protected Map<String, Map<String, Integer>> healthMap = //
			new ConcurrentHashMap<String, Map<String, Integer>>();

	protected ScheduledExecutorService scheduler;

	protected int initialDelay = 3;

	protected int delay = 3;

	/**
	 * { Key：服务编号-服务提供者版本号，Value："服务提供者地址1,服务提供者地址2" }<br>
	 * 实际上此信息应从注册中心获取，由注册中心负责维护。
	 */
	protected Properties prop = new Properties();

	@PostConstruct
	public void init() {
		this.config();
	}

	@Override
	public void config() {
		try {
			log.info("Load registry.properties ...");
			prop.load(HttpHealthChkImpl.class.getClassLoader()//
					.getResourceAsStream("conf/health.properties"));
		} catch (IOException e) {
			log.error("Error on load registry.properties.", e);
		}
	}

	@Override
	public void init(Map<String, List<String>> providerMap) {
		this.providerMap = providerMap;
		for (String sk : providerMap.keySet()) {
			List<String> al = providerMap.get(sk);
			Map<String, Integer> hm = new ConcurrentHashMap<String, Integer>();
			for (String a : al) {
				hm.put(a, -1);
			}
			healthMap.put(sk, hm);
		}
		scheduler.scheduleWithFixedDelay(this, initialDelay, delay, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		for (String sk : healthMap.keySet()) {
			String url = prop.getProperty(sk);
			if (Util.strIsEmpty(url)) {
				continue;
			}
			List<String> pl = providerMap.get(sk);
			Map<String, Integer> hm = healthMap.get(sk);
			for (String a : hm.keySet()) {
				String rs = this.test(String.format(url, a));
				log.debug("Check web health {}-{} = {}", sk, a, rs);
				if (Util.strIsNotEmpty(rs)) {
					rs = rs.trim();
					int load = Util.str2int(rs, -1);
					if (load >= 0) {
						hm.put(a, load);
						if (!pl.contains(a)) {
							pl.add(a);
						}
						continue;
					}
				}
				pl.remove(a);
			}
		}
	}

	@Override
	public void onFailed(String sn, Exception e) {
	}

	protected String test(String url) {
		BufferedReader br = null;
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(30000);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			conn.connect();
			br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			return br.readLine();
		} catch (Exception e) {
			return null;
		} finally {
			StreamUtil.close(br);
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	public Map<String, List<String>> getProviderMap() {
		return providerMap;
	}

	public void setProviderMap(Map<String, List<String>> providerMap) {
		this.providerMap = providerMap;
	}

	public Map<String, Map<String, Integer>> getHealthMap() {
		return healthMap;
	}

	public void setHealthMap(Map<String, Map<String, Integer>> healthMap) {
		this.healthMap = healthMap;
	}

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	public void setScheduler(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}

	public int getInitialDelay() {
		return initialDelay;
	}

	public void setInitialDelay(int initialDelay) {
		this.initialDelay = initialDelay;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}
}