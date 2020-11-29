package com.boarsoft.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.Util;

public class FileUploadUtil {
	private static final Logger log = LoggerFactory.getLogger(FileUploadUtil.class);

	public static long breakpointUpload(String addr, File f, String targetPath,
			String targetName, long offset) throws Exception {
		return breakpointUpload(addr, f, targetPath, targetName, offset, 1);
	}

	/**
	 * 支持断点续传的文件上传
	 * 
	 * @param addr
	 *            服务端接收地址
	 * @param f
	 * @throws Exception
	 */
	public static long breakpointUpload(String addr, File f, String targetPath,
			String targetName, long offset, int count) throws Exception {
		URL url = new URL(addr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
		conn.setRequestProperty("Content-Length", String.valueOf(f.length()));
		conn.setRequestProperty("connection", "Keep-Alive");
		conn.setRequestProperty("Content-Type", "multipart/form-data");
		conn.setRequestProperty("Target-Path", targetPath);
		conn.setRequestProperty("Target-Name", targetName);
		conn.setRequestProperty("Offset", String.valueOf(offset));
		long len = offset;// 成功上传了多少
		BufferedReader br = null;
		try {
			OutputStream out = conn.getOutputStream();
			RandomAccessFile raf = new RandomAccessFile(f, "r");
			raf.seek(offset);
			int bytes = 0;
			byte[] bufferOut = new byte[1024];
			while ((bytes = raf.read(bufferOut)) != -1) {
				out.write(bufferOut, 0, bytes);
			}
			raf.close();
			out.flush();
			out.close();
			// 读取返回的字节数，表示成功
			br = new BufferedReader(
					new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String line = null;
			while ((line = br.readLine()) != null) {
				len = Util.str2long(line, 0);
				break;
			}
		} catch (Exception e) {
			log.info("retry upload File：" + count + " time");
			if (count <= 3) {
				Thread.sleep(10000);
				len = breakpointUpload(addr, f, targetPath, targetName, len, count + 1);
			}
			log.error("", e);
		} finally {
			StreamUtil.close(br);
		}
		return len;
	}
}
