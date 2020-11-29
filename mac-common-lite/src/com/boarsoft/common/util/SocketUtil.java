package com.boarsoft.common.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.NetworkChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketUtil {
	private static final Logger log = LoggerFactory.getLogger(SocketUtil.class);

	private static Socket getSocket(String ip, int port, String localIp, int localPort) throws IOException {
		Socket socket = null;
		if (localIp != null && localPort > 0) {
			if ("".equals(localIp))
				localIp = "127.0.0.1";
			socket = new Socket();
			socket.bind(new InetSocketAddress(localIp, localPort));
			socket.connect(new InetSocketAddress(ip, port));
		} else {
			socket = new Socket(ip, port);
		}
		return socket;
	}

	/**
	 * 此方法使用println发送字符串，因此会在最后多加一对\n\r
	 * 
	 * @param ip
	 * @param port
	 * @param str
	 * @return
	 * @throws Exception
	 */
	public static String sendStr(String ip, int port, String str) throws Exception {
		return sendStr(ip, port, str, null, 0);
	}

	/**
	 * 此方法使用println发送字符串，因此会在最后多加一对\n\r
	 * 
	 * @param ip
	 * @param port
	 * @param str
	 * @param localIp
	 * @param localPort
	 * @return
	 * @throws Exception
	 */
	public static String sendStr(String ip, int port, String str, String localIp, int localPort) throws Exception {
		Socket socket = null;
		DataInputStream dis = null;
		DataOutputStream dos = null;
		try {
			socket = getSocket(ip, port, localIp, localPort);
			dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			dos = new DataOutputStream(socket.getOutputStream());
			dos.writeUTF(str);
			dos.flush();
			return dis.readUTF();
		} finally {
			StreamUtil.close(dis);
			StreamUtil.close(dos);
			if (socket != null)
				socket.close();
		}
	}

	public static Object sendObject(String ip, int port, Object m) throws Exception {
		return sendObject(ip, port, m, null, 0);
	}

	public static Object sendObject(String ip, int port, Object m, String localIp, int localPort) throws Exception {
		Socket socket = getSocket(ip, port, localIp, localPort);
		ObjectInputStream ois = null;
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(m);
			ois = new ObjectInputStream(socket.getInputStream());
			while (true) {
				Object o = ois.readObject();
				if (o != null) {
					return o;
				}
				Thread.sleep(500);
			}
		} finally {
			StreamUtil.close(ois);
			StreamUtil.close(oos);
			if (socket != null)
				socket.close();
		}
	}

	public static void closeChannel(AsynchronousSocketChannel asc) {
		if (asc == null) {
			return;
		}
		try {
			asc.shutdownInput();
		} catch (IOException e1) {
			// log.error("Error on shutdown input from {}", asc, e1);
		}
		try {
			asc.shutdownOutput();
		} catch (IOException e1) {
			// log.error("Error on shutdown output from {}", asc, e1);
		}
		try {
			asc.close();
		} catch (IOException e) {
			// log.error("Error on close socket channel.", e);
		}
	}

	/**
	 * 安全的关闭SocketChannel
	 * 
	 * @param sc
	 *            AbstractSelectableChannel
	 */
	public static void closeChannel(NetworkChannel sc) {
		if (sc == null) {
			return;
		}
		try {
			if (sc.isOpen()) {
				sc.close();
			}
		} catch (IOException e) {
			log.error("Error on close socket channel.", e);
		}
	}

	public static String getSocketAddressStr(SocketAddress sa) throws IOException {
		InetSocketAddress isa = (InetSocketAddress) sa;
		String ip = isa.getAddress().getHostAddress();
		return new StringBuilder(ip).append(":").append(isa.getPort()).toString();
	}
}