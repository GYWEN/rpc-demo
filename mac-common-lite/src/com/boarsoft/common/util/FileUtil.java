package com.boarsoft.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.FileTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.Util;

public class FileUtil {
	private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

	@SuppressWarnings("restriction")
	public static final String LINE_SEPARATOR = java.security.AccessController
			.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));

	public static FileTime getCreationTime(File f) throws IOException {
		return (FileTime) Files.getAttribute(f.toPath(), //
				"basic:creationTime", LinkOption.NOFOLLOW_LINKS);
	}

	/**
	 * 创建一个多级目录
	 * 
	 * @param path
	 * @return
	 */
	public static boolean makePath(String path) {
		File file = new File(path);
		if (file.isDirectory()) {
			return true;
		}
		if (file.mkdirs()) {
			return true;
		}
		return false;
	}

	public static void writeBytes(File f, byte[] bytes) throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		FileOutputStream fos = null;
		FileChannel fc = null;
		try {
			fos = new FileOutputStream(f);
			// fos.write(bytes);
			fc = fos.getChannel();
			int w = 0;
			while ((w = fc.write(buf)) > 0) {
				log.debug("Write {} bytes to {}", w, f.getAbsolutePath());
			}
		} finally {
			StreamUtil.close(fc);
			StreamUtil.close(fos);
		}
	}

	public static byte[] readBytes(File f) throws IOException {
		return readBytes(f, 0, f.length());
	}

	public static byte[] readBytes(File f, int offset, Long length) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(length.intValue());
		FileInputStream fis = null;
		FileChannel fc = null;
		try {
			fis = new FileInputStream(f);
			fc = fis.getChannel();
			if (offset > 0) {
				fc.position(offset);
			}
			int r = 0;
			while ((r = fc.read(buf)) > 0) {
				log.debug("Read {} bytes from {}", r, f.getAbsolutePath());
			}
			return buf.array();
		} finally {
			StreamUtil.close(fc);
			StreamUtil.close(fis);
		}
	}

	public static String read(File f) throws IOException {
		return FileUtil.read(f, LINE_SEPARATOR, "UTF-8");
	}

	public static String read(File f, String encoding) throws IOException {
		return FileUtil.read(f, LINE_SEPARATOR, encoding);
	}

	/**
	 * 使用指定的编码和字符分隔读取文件
	 * 
	 * @param f
	 *            要读取的文件
	 * @param sp
	 *            文件内容分隔符
	 * @param encoding
	 *            文件编码
	 * @return
	 * @throws IOException
	 */
	public static String read(File f, String sp, String encoding) throws IOException {
		BufferedReader br = null;
		String s = null;
		StringBuffer sb = new StringBuffer();
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(f), encoding));
			boolean b = Util.strIsNotEmpty(sp);
			while ((s = br.readLine()) != null) {
				sb.append(s);
				if (b) {
					sb.append(sp);
				}
			}
			return sb.toString();
		} finally {
			StreamUtil.close(br);
		}
	}

	public static void write(File f, String s) throws FileNotFoundException {
		write(f, s, false);
	}

	public static void write(File f, String s, boolean append) throws FileNotFoundException {
		write(f, s, append, true, "UTF-8");
	}

	/**
	 * 以指定编码写文件
	 * 
	 * @param f
	 *            要写的目标文件
	 * @param s
	 *            要写的内容
	 * @param append
	 *            是否追加
	 * @param autoFlush
	 *            自动FLUSH
	 * @param encoding
	 *            文件编码
	 * @throws FileNotFoundException
	 */
	public static void write(File f, String s, boolean append, boolean autoFlush, String encoding)
			throws FileNotFoundException {
		PrintStream ps = null;
		// PrintWriter pw = null;
		try {
			// pw = new PrintWriter(new FileOutputStream(f, append), autoFlush,
			// encoding);
			// pw.print(s);
			ps = new PrintStream(new FileOutputStream(f, append), autoFlush, encoding);
			ps.print(s);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			StreamUtil.close(ps);
		}
	}

	// 删除文件，可以是单个文件或文件夹
	public static boolean deleteFile(String fileName) {
		File file = new File(fileName);
		if (!file.exists()) {
			return false;
		} else {
			if (file.isFile()) {
				return file.delete();
			}
			return false;
		}
	}

	// 删除文件夹和文件夹下所有的文件
	public static boolean deleteDirectory(String dir) {
		// 如果dir不以文件分隔符结尾，自动添加文件分隔符
		boolean flag = true;
		if (!dir.endsWith(File.separator)) {
			dir = dir + File.separator;
		}
		File dirFile = new File(dir);
		// 如果dir对应的文件不存在，或者不是一个目录，则退出
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			return false;
		} else {
			File[] files = dirFile.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isFile()) {
					flag = FileUtil.deleteFile(files[i].getAbsolutePath());
					if (!flag) {
						return false;
					}
				} else {
					flag = FileUtil.deleteDirectory(files[i].getAbsolutePath());
					if (!flag) {
						return false;
					}
				}
			}
			if (!dirFile.delete()) {
				return false;
			}
		}
		return true;
	}
}