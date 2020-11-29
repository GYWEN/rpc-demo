package com.boarsoft.common.util;

import java.io.Closeable;

import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamUtil {
	protected static Logger log = LoggerFactory.getLogger(StreamUtil.class);

	public static void close(Closeable p) {
		if (p == null)
			return;
		try {
			p.close();
		} catch (Exception e) {
			log.error("Error on close {}", p.getClass().getName(), e);
		}
	}

	public static void close(ImageInputStream p) {
		if (p == null)
			return;
		try {
			p.close();
		} catch (Exception e) {
			log.error("Error on close {}", p.getClass().getName(), e);
		}
	}
}