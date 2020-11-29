/**
 * 
 */
package com.boarsoft.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipUtil {
	private static final Logger log = LoggerFactory.getLogger(ZipUtil.class);

	/**
	 * 压缩单个文件或者目录
	 * 
	 * @param zipFileName
	 *            to
	 * @param inputFile
	 *            from
	 * @throws Exception
	 */
	public static void zip(String zipFileName, String inputFile) {
		zip(zipFileName, new File(inputFile));
	}

	/**
	 * 传入指定多个文件集合进行压缩
	 * 
	 * @param zipFileName
	 * @param files
	 */
	public static void zip(String zipFileName, List<File> files) {
		ZipOutputStream out = null;
		try {
			out = new ZipOutputStream(new FileOutputStream(zipFileName));
			for (File f : files) {
				zip(out, f, f.getName());
			}
		} catch (Exception e) {
			log.error("", e);
		} finally {
			StreamUtil.close(out);
		}
	}

	/**
	 * 定义压缩文件及目录为zip文件的方法,重写下面的zip方法
	 * 
	 * @param zipFileName
	 * @param inputFile
	 * @throws Exception
	 */
	private static void zip(String zipFileName, File inputFile) {
		ZipOutputStream out = null;
		try {
			out = new ZipOutputStream(new FileOutputStream(zipFileName));
			zip(out, inputFile, inputFile.getName());
		} catch (Exception e) {
			log.error("", e);
		} finally {
			StreamUtil.close(out);
		}
	}

	/**
	 * 定义压缩文件及目录为zip文件的方法
	 * 
	 * @param out
	 * @param f
	 * @param base
	 * @throws Exception
	 */
	private static void zip(ZipOutputStream out, File f, String base) {
		// 判断File是否为目录
		try {
			if (f.isDirectory()) {
				// 获取f目录下所有文件及目录,作为一个File数组返回
				File[] fl = f.listFiles();
				out.putNextEntry(new ZipEntry(base + "/"));
				base = base.length() == 0 ? "" : base + "/";
				for (int i = 0; i < fl.length; i++) {
					zip(out, fl[i], base + fl[i].getName());
				}
			} else {
				out.putNextEntry(new ZipEntry(base));
				FileInputStream in = null;
				try {
					in = new FileInputStream(f);
					int b;
					while ((b = in.read()) != -1) {
						out.write(b);
					}
				} catch (Exception e) {
					log.error("", e);
				} finally {
					StreamUtil.close(in);
				}
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	/**
	 * 定义解压缩zip文件的方法
	 * 
	 * @param zipFileName
	 * @param outputDirectory
	 */
	public static List<File> unzip(String zipFileName, String outputDirectory) {
		ZipInputStream in = null;
		try {
			List<File> files = new ArrayList<File>();
			in = new ZipInputStream(new FileInputStream(zipFileName));
			// 获取ZipInputStream中的ZipEntry条目，一个zip文件中可能包含多个ZipEntry，
			// 当getNextEntry方法的返回值为null，则代表ZipInputStream中没有下一个ZipEntry，
			// 输入流读取完成；
			ZipEntry z = in.getNextEntry();
			while (z != null) {
				// 创建以zip包文件名为目录名的根目录
				File f = new File(outputDirectory);
				f.mkdir();
				if (z.isDirectory()) {
					String name = z.getName();
					name = name.substring(0, name.length() - 1);
					f = new File(outputDirectory + File.separator + name);
					f.mkdir();
				} else {
					f = new File(outputDirectory + File.separator + z.getName());
					f.createNewFile();
					FileOutputStream out = null;
					try {
						out = new FileOutputStream(f);
						int b;
						while ((b = in.read()) != -1) {
							out.write(b);
						}
						files.add(f);
					} catch (Exception e) {
						log.error("", e);
					} finally {
						StreamUtil.close(out);
					}
				}
				// 读取下一个ZipEntry
				z = in.getNextEntry();
			}
			return files;
		} catch (Exception e) {
			log.error("", e);
		} finally {
			StreamUtil.close(in);
		}
		return null;
	}

	/**
	 * 解压zip文件
	 * 
	 * @param f
	 *            zip源文件
	 * @param to
	 *            解压后的目标文件夹
	 * @throws IOException
	 */
	public static void unzip(File f, String to) throws IOException {
		ZipFile zf = null;
		try {
			zf = new ZipFile(f);
			Enumeration<?> entries = zf.entries();
			while (entries.hasMoreElements()) {
				ZipEntry ze = (ZipEntry) entries.nextElement();
				String p = new StringBuilder().append(to)//
						.append(File.separator).append(ze.getName()).toString();
				// 如果是文件夹，就创建个文件夹
				if (ze.isDirectory()) {
					File dir = new File(p);
					dir.mkdirs();
				} else {
					// 如果是文件，就先创建一个文件，然后用io流把内容copy过去
					File tf = new File(p);
					// 保证这个文件的父文件夹必须要存在
					if (!tf.getParentFile().exists()) {
						tf.getParentFile().mkdirs();
					}
					tf.createNewFile();
					// 将压缩文件内容写入到这个文件中
					InputStream is = null;
					try {
						is = zf.getInputStream(ze);
						FileOutputStream fos = null;
						try {
							fos = new FileOutputStream(tf);
							int len;
							byte[] buf = new byte[40960];
							while ((len = is.read(buf)) != -1) {
								fos.write(buf, 0, len);
							}
						} finally {
							StreamUtil.close(fos);
						}
					} finally {
						StreamUtil.close(is);
					}
				}
			}
		} finally {
			StreamUtil.close(zf);
		}
	}
}
