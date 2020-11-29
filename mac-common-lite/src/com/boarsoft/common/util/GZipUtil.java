package com.boarsoft.common.util;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GZipUtil {
	protected static final Logger log = LoggerFactory.getLogger(GZipUtil.class);

	/**
	 * 解压GZIP文件
	 * 
	 * @param fn
	 * @param to
	 * @throws Exception
	 */
	public static boolean ungzip(String fn, String to) throws Exception {
		GZIPInputStream gzi = null;
		BufferedOutputStream bos = null;
		int b;
		byte[] d = new byte[1024];
		try {
			gzi = new GZIPInputStream(new FileInputStream(fn));
			bos = new BufferedOutputStream(new FileOutputStream(to));
			while ((b = gzi.read(d)) > 0) {
				bos.write(d, 0, b);
			}
		} catch (Exception e) {
			log.error("", e);
		} finally {
			StreamUtil.close(gzi);
			StreamUtil.close(bos);
		}
		return true;
	}

	/**
	 * 解压ZIP文件
	 * 
	 * @author Mac_J
	 * 
	 */
	public static boolean unzip(String fn, String to) throws Exception {
		ZipInputStream zip = null;
		BufferedOutputStream bos = null;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		int b;
		byte[] d = new byte[1024];
		try {
			fis = new FileInputStream(fn);
			zip = new ZipInputStream(fis);
			fos = new FileOutputStream(to);
			bos = new BufferedOutputStream(fos);
			while ((b = zip.read(d)) > 0) {
				bos.write(d, 0, b);
			}
		} catch (Exception e) {
			log.error("", e);
		} finally {
			StreamUtil.close(fis);
			StreamUtil.close(zip);
			StreamUtil.close(fos);
			StreamUtil.close(bos);
		}
		return true;
	}
}