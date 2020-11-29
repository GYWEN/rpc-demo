package com.boarsoft.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.bean.NameValueBean;

public class HttpClientUtil {
	private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

	/**
	 * 
	 * @param url
	 * @param to
	 * @return
	 */
	public static boolean downloadFile(String url, String to, int timeout) {
		log.info("Downloading {}", url);
		BufferedInputStream br = null;
		BufferedOutputStream bw = null;
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(timeout);
			File tf = new File(to);
			br = new BufferedInputStream(conn.getInputStream());
			bw = new BufferedOutputStream(new FileOutputStream(tf));
			byte[] b = new byte[1024];
			int d;
			while ((d = br.read(b)) != -1) {
				bw.write(b, 0, d);
			}
			log.info("Downloaded: {}", to);
			return true;
		} catch (Exception e) {
			log.error("Error on access {}", url, e);
		} finally {
			StreamUtil.close(bw);
			StreamUtil.close(br);
			conn.disconnect();
		}
		return false;
	}

	public static String sendGet(String url) {
		return sendGet(url, "UTF-8", "UTF-8", null, null, null);
	}

	public static String sendGet(String url, Map<String, String> params) {
		return sendGet(url, "UTF-8", "UTF-8", map2list(params), null, null);
	}

	public static String sendPost(String url, Map<String, String> params) throws IOException {
		String body = HttpClientUtil.pack(params, "UTF-8");
		return sendPost(url, "text/html", "UTF-8", "UTF-8", null, body, null);
	}

	public static String sendGet(String url, List<NameValueBean> params) {
		return sendGet(url, "UTF-8", "UTF-8", params, null, null);
	}

	public static String sendPost(String url, List<NameValueBean> params) throws IOException {
		String body = HttpClientUtil.pack(params, "UTF-8");
		return sendPost(url, "text/html", "UTF-8", "UTF-8", null, body, null);
	}

	// ----------------

	/**
	 * 
	 * @param url
	 * @param encOut
	 * @param encIn
	 * @param params
	 * @param headers
	 * @return
	 */
	public static Map<String, List<String>> sendHead(String url) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(30000);
			conn.setRequestMethod("HEAD");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			return conn.getHeaderFields();
		} catch (Exception e) {
			log.error("Error on access {}", url, e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		return null;
	}

	/**
	 * 发送GET请求的完整方法
	 * 
	 * @param url
	 * @param encOut
	 * @param encIn
	 * @param params
	 * @param headers
	 * @param cookie
	 * @return
	 */
	public static String sendGet(String url, String encOut, String encIn, List<NameValueBean> params,
			Map<String, String> headers, String cookie) {
		StringBuffer sb = new StringBuffer(url);
		try {
			if (params != null && params.size() > 0) {
				sb.append("?");
				boolean b = false;
				for (NameValueBean nv : params) {
					if (b)
						sb.append("&");
					String v = nv.getValue();
					if (nv.isEncode())
						v = URLEncoder.encode(v, encOut);
					sb.append(nv.getName()).append("=").append(v);
					b = true;
				}
			}
		} catch (Exception e) {
			log.error("Error encode param for {}", url, e);
			return null;
		}
		url = sb.toString();
		//
		BufferedReader br = null;
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(30000);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			// 设置cookie
			if (cookie != null)
				conn.setRequestProperty("Cookie", cookie);
			if (headers != null && headers.size() > 0) {
				for (String h : headers.keySet())
					conn.setRequestProperty(h, headers.get(h));
			}
			conn.connect();
			br = new BufferedReader(new InputStreamReader(conn.getInputStream(), encIn));
			sb.setLength(0);
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\r\n");
			}
			return sb.toString();
		} catch (Exception e) {
			log.error("Error on access {}", url, e);
		} finally {
			StreamUtil.close(br);
			if (conn != null) {
				conn.disconnect();
			}
		}
		return null;
	}

	public static String sendPost(String url, String encOut, String encIn, Map<String, String> headers,
			List<NameValueBean> params, String cookie) throws IOException {
		String body = HttpClientUtil.pack(params, encOut);
		return sendPost(url, "text/html", encOut, encIn, headers, body, cookie);
	}

	public static String sendPost(String url, byte[] ba, String encIn) {
		return sendPost(url, 3000, 30000, null, null, ba, encIn);
	}

	public static String sendPost(String url, int connectTimeout, int readTimeout, byte[] ba, String encIn) {
		return sendPost(url, connectTimeout, readTimeout, null, null, ba, encIn);
	}

	/**
	 * POST 二进制
	 * 
	 * @param url
	 * @param connectTimeout
	 * @param readTimeout
	 * @param headers
	 * @param cookie
	 * @param ba
	 * @param encIn
	 * @return
	 */
	public static String sendPost(String url, int connectTimeout, int readTimeout, Map<String, String> headers, String cookie,
			byte[] ba, String encIn) {
		BufferedOutputStream bos = null;
		HttpURLConnection conn = null;
		BufferedReader br = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(30000);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/octet-stream");
			conn.setRequestProperty("Content-Length", String.valueOf(ba.length));

			// 设置cookie
			if (cookie != null) {
				conn.setRequestProperty("Cookie", cookie);
			}
			// 设置header
			if (headers != null && headers.size() > 0) {
				for (String h : headers.keySet())
					conn.setRequestProperty(h, headers.get(h));
			}

			bos = new BufferedOutputStream(conn.getOutputStream());
			// for (int off = 0; off < ba.length; off += 2048) {
			// bos.write(ba, off, Math.min(ba.length - off, 2048));
			// }
			bos.write(ba);
			bos.flush();
			//
			int status = conn.getResponseCode();
			if (status != 200) {
				throw new InternalError(conn.getResponseMessage());
			}
			br = new BufferedReader(new InputStreamReader(conn.getInputStream(), encIn));
			StringBuffer sb = new StringBuffer();
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\r\n");
			}
			return sb.toString();
		} catch (Exception e) {
			log.error("Error on access {}", url, e);
		} finally {
			StreamUtil.close(br);
			StreamUtil.close(bos);
			if (conn != null) {
				conn.disconnect();
			}
		}
		return null;
	}

	public static String pack(List<NameValueBean> params, String enc) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		boolean b = false;
		for (NameValueBean nv : params) {
			if (b)
				sb.append("&");
			String v = nv.getValue();
			if (nv.isEncode())
				v = URLEncoder.encode(v, enc);
			sb.append(nv.getName()).append("=").append(v);
			b = true;
		}
		return sb.toString();
	}

	public static String pack(Map<String, String> params, String enc) throws UnsupportedEncodingException {
		return pack(map2list(params), enc);
	}

	public static void unpack(String s, Map<String, String> map) throws UnsupportedEncodingException {
		String[] a = s.split("&");
		for (String f : a) {
			String[] r = f.split("=");
			map.put(r[0], URLDecoder.decode(r[1], "UTF-8"));
		}
	}

	public static void unpack(String s, List<NameValueBean> list) throws UnsupportedEncodingException {
		String[] a = s.split("&");
		for (String f : a) {
			String[] r = f.split("=");
			list.add(new NameValueBean(r[0], URLDecoder.decode(r[1], "UTF-8")));
		}
	}

	/**
	 * 
	 * @param url
	 * @param encIn
	 * @param xml
	 *            /json
	 * @return
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	public static String sendXmlPost(String url, String encIn, String str, String cookie)
			throws UnsupportedEncodingException, IOException {
		return sendPost(url, "application/xml", encIn, encIn, null, str, cookie);
	}

	public static String sendHtmlPost(String url, String encIn, String str, String cookie)
			throws UnsupportedEncodingException, IOException {
		return sendPost(url, "text/html", encIn, encIn, null, str, cookie);
	}

	public static String sendPost(String url, String encIn, String str, String cookie)
			throws UnsupportedEncodingException, IOException {
		return sendPost(url, "text/html", encIn, encIn, null, str, cookie);
	}

	public static String sendPost(String url, String contentType, String encIn, String str, String cookie)
			throws UnsupportedEncodingException, IOException {
		return sendPost(url, contentType, encIn, encIn, null, str, cookie);
	}

	/**
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static String readStr(InputStream is, String encoding) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(is, encoding));
			String line = null;
			StringBuffer sb = new StringBuffer();
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		} finally {
			StreamUtil.close(br);
		}
	}

	/**
	 * 将Map转成
	 * 
	 * @param m
	 * @return
	 */
	public static List<NameValueBean> map2list(Map<String, String> m) {
		List<NameValueBean> l = new ArrayList<NameValueBean>();
		for (String k : m.keySet())
			l.add(new NameValueBean(k, String.valueOf(m.get(k))));
		return l;
	}

	/**
	 * 
	 * @param url
	 * @param contentType
	 * @param encOut
	 * @param encIn
	 * @param headers
	 * @param body
	 * @param cookie
	 * @return
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	public static String sendPost(String url, String contentType, //
			String encOut, String encIn, Map<String, String> headers, String body, String cookie)
			throws UnsupportedEncodingException, IOException {
		PrintWriter out = null;
		BufferedReader br = null;
		HttpURLConnection conn = null;
		StringBuilder sb = new StringBuilder();
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(30000);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			conn.setRequestProperty("Content-Type", contentType);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 设置cookie
			if (cookie != null) {
				conn.setRequestProperty("Cookie", cookie);
			}
			// headers
			if (headers != null && headers.size() > 0) {
				for (String h : headers.keySet())
					conn.setRequestProperty(h, headers.get(h));
			}
			// body
			out = new PrintWriter(conn.getOutputStream());
			out.print(body);
			out.flush();
			br = new BufferedReader(new InputStreamReader(conn.getInputStream(), encIn));
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
		} finally {
			StreamUtil.close(br);
			StreamUtil.close(out);
			if (conn != null) {
				conn.disconnect();
			}
		}
		return sb.toString();
	}

	public static String sendPost(String url, String contentType, String encOut, String encIn, Map<String, String> headers,
			Map<String, String> params, String cookie) throws IOException {
		String body = HttpClientUtil.pack(params, encOut);
		return sendPost(url, contentType, encOut, encIn, headers, body, cookie);
	}

	public static String sendPost(String url, String contentType, //
			String encOut, String encIn, Map<String, String> headers, String body)
			throws UnsupportedEncodingException, IOException {
		return sendPost(url, contentType, encOut, encIn, headers, body, null);
	}

	public static String sendPost(String url, String contentType, Map<String, String> headers, String body)
			throws UnsupportedEncodingException, IOException {
		return sendPost(url, contentType, "UTF-8", "UTF-8", headers, body, null);
	}

	public static String sendPost(String url, String contentType, Map<String, String> headers, Map<String, String> params)
			throws IOException {
		String body = HttpClientUtil.pack(params, "UTF-8");
		return sendPost(url, contentType, "UTF-8", "UTF-8", headers, body, null);
	}

	public static String sendPost(String url, String contentType, Map<String, String> params)
			throws IOException {
		String body = HttpClientUtil.pack(params, "UTF-8");
		return sendPost(url, contentType, "UTF-8", "UTF-8", null, body, null);
	}
}