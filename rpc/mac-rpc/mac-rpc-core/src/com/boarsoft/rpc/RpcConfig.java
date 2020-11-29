package com.boarsoft.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.Util;
import com.boarsoft.common.bean.InetConfig;

public class RpcConfig {
	private static final Logger log = LoggerFactory.getLogger(RpcConfig.class);
	/** */
	private static Properties prop;
	/** */
	public static String VERSION = "1.0.0";
	/** 心跳超时 */
	public static int HEARTBEAT_PERIOD = 3000;
	/** 连接超时 */
	public static int CONNECT_TIMEOUT = 5000;
	/** 停机最大时延，单位秒 */
	public static int SHUTDOWN_TIMEOUT = 10;
	/** 一个缓冲区放几个数据包 */
	public static int BUFFER_RATE = 8;

	/** type + methodId + methodExeNo */
	public static int PACKAGE_HD0_SIZE = (Integer.SIZE + Integer.SIZE + Long.SIZE) / 8;
	/** pageIndex + contentLength */
	public static int PACKAGE_HD1_SIZE = (Integer.SIZE + Integer.SIZE) / 8;
	/** */
	public static int PACKAGE_HEAD_SIZE = PACKAGE_HD0_SIZE + PACKAGE_HD1_SIZE;
	/** totalLength + pageCount + protocol */
	public static int PACKAGE_BODY0_SIZE = (Integer.SIZE + Integer.SIZE + Integer.SIZE) / 8;
	/** */
	public static int PACKAGE_0_SIZE = PACKAGE_HEAD_SIZE + PACKAGE_BODY0_SIZE;
	/** */
	public static int PACKAGE_BODY_MAX_SIZE = 4072;
	/** */
	public static int PACKAGE_MAX_SIZE = PACKAGE_HEAD_SIZE + PACKAGE_BODY_MAX_SIZE;
	/** */
	public static int BUFFER_SIZE = RpcConfig.PACKAGE_MAX_SIZE * RpcConfig.BUFFER_RATE;

	public static int QUEUE_BUFFERS = 512;

	public static int LINK_CHANNELS = 1;
	/** RpcWriter一次从发送队列获取并合并发送的最大缓冲区数量 */
	public static int WRITER_BUFFERS = 8;
	/** 此参数决定RpcReader用于读取的缓冲区大小 */
	public static int READER_BUFFERS = 1;
	/** RpcReader在读不到数据时，休眠多久，单位毫秒 */
	public static int READER_SLEEP = 100;
	/** 最低要求匹配的版本位数 */
	public static int SERVICE_VERSION_MATCH = 2;

	/** 提交注册表的超时设置 */
	public static int POP_REG_TIMEOUT = 30000;
	/** 推送注册表的超时设置 */
	public static int PUSH_REG_TIMEOUT = 60000;
	/** 离线通知超时设置 */
	public static int NODE_OFF_TIMEOUT = 10000;
	/** 停机通知方法超时设置 */
	public static int SHUTTING_DOWN_TIMEOUT = 10000;
	/** 绑定连接方法起超时设置 */
	public static int BIND_LINK_TIMEOUT = 30000;
	/** socket通道写超时 */
	public static int WRITE_TIMEOUT = 1000;
	/** socket通道读超时 */
	public static int READ_TIMEOUT = 1000;

	public static synchronized void init(InputStream is) {
		if (prop == null) {
			prop = new Properties();
		}
		try {
			prop.load(is);
			if (Util.strIsEmpty(InetConfig.LOCAL_IP)) {
				InetConfig.init(is);
			}
			if (InetConfig.LOCAL_PORT <= 0) {
				RpcConfig.setAddr(prop.getProperty("rpc.ip"), prop.getProperty("rpc.port"));
			}
			VERSION = prop.getProperty("rpc.version", VERSION);
			HEARTBEAT_PERIOD = getInt("rpc.heartbeat.period", HEARTBEAT_PERIOD);
			CONNECT_TIMEOUT = getInt("rpc.connect.timeout", CONNECT_TIMEOUT);
			SHUTDOWN_TIMEOUT = getInt("rpc.shutdown.timeout", SHUTDOWN_TIMEOUT);
			//
			PACKAGE_BODY_MAX_SIZE = getInt("rpc.package.body.size", PACKAGE_BODY_MAX_SIZE);
			PACKAGE_MAX_SIZE = PACKAGE_HEAD_SIZE + PACKAGE_BODY_MAX_SIZE;
			//
			BUFFER_RATE = getInt("rpc.buffer.rate", BUFFER_RATE);
			BUFFER_SIZE = PACKAGE_MAX_SIZE * BUFFER_RATE;

			QUEUE_BUFFERS = getInt("rpc.queue.buffers", QUEUE_BUFFERS);
			LINK_CHANNELS = getInt("rpc.link.channels", LINK_CHANNELS);
			READER_BUFFERS = getInt("rpc.reader.buffers", READER_BUFFERS);
			WRITER_BUFFERS = getInt("rpc.writer.buffers", WRITER_BUFFERS);
			READER_SLEEP = getInt("rpc.reader.sleep", READER_SLEEP);

			WRITE_TIMEOUT = getInt("rpc.write.timeout", WRITE_TIMEOUT);
			READ_TIMEOUT = getInt("rpc.read.timeout", READ_TIMEOUT);

			SERVICE_VERSION_MATCH = getInt("rpc.svc.ver.match", SERVICE_VERSION_MATCH);

			POP_REG_TIMEOUT = getInt("rpc.reg.pop.timeout", POP_REG_TIMEOUT);
			PUSH_REG_TIMEOUT = getInt("rpc.reg.push.timeout", PUSH_REG_TIMEOUT);
			NODE_OFF_TIMEOUT = getInt("rpc.node.off.timeout", NODE_OFF_TIMEOUT);
			SHUTTING_DOWN_TIMEOUT = getInt("rpc.shutting.down.timeout", SHUTTING_DOWN_TIMEOUT);
			BIND_LINK_TIMEOUT = getInt("rpc.bind.link.timeout", BIND_LINK_TIMEOUT);
		} catch (IOException e) {
			log.error("Error on load system config.", e);
			throw new RuntimeException(e);
		}
	}

	public static int getInt(String key, int value) {
		return Util.str2int(prop.getProperty(key), value);
	}

	public static void setAddr(String ip, String port) throws UnknownHostException, SocketException {
		InetConfig.setAddr(ip, port);
	}

	public static void setIp(String ip) throws UnknownHostException, SocketException {
		InetConfig.setIp(ip);
	}

	public static String getProperty(String key, String value) {
		return prop.getProperty(key, value);
	}

	public static Properties getProperties() {
		return prop;
	}
}
