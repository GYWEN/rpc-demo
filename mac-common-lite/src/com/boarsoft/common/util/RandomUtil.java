package com.boarsoft.common.util;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

public class RandomUtil {
	private static final SecureRandom numberGenerator = new SecureRandom();

	public static int random(int min, int max) {
		// Double d = new Random(System.currentTimeMillis()).nextInt(max - min);
		// return min + d.intValue() % (max - min);
		return min + numberGenerator.nextInt(max - min);
	}

	/**
	 * 产生1~9位随机数字
	 * 
	 * @param len
	 *            要产生的随机数的长度，取值范围：1~9
	 * @return
	 */
	public static int gen09(int len) {
		if (len < 1 || len > 9)
			throw new IllegalArgumentException("len must be 1~9");
		int d = (int) Math.pow(10, len - 1);
		return d + new Random().nextInt(d * 9);
	}

	/**
	 * 产生1~32 小写字母和数字的随机字符串
	 * 
	 * @param len
	 *            要产生的随机字符串的长度，取值范围：a~z0~9。长度为32时等同于UUID
	 * @return
	 */
	public static String genAz09(int len) {
		if (len < 1 || len > 32)
			throw new IllegalArgumentException("len must be 1~32");
		String s = UUID.randomUUID().toString().replaceAll("-", "");
		return s.substring(32 - len);
	}

	/**
	 * UUID.randomUUID().toString().replaceAll("-", "");
	 * 
	 * @return
	 */
	public static String genUUID() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

	/**
	 * 从UUID复制出来的方法，不生成-，所以比genUUID更快
	 * 
	 * @return
	 */
	public static String randomUUID() {
		/* random */
		byte[] randomBytes = new byte[16];
		numberGenerator.nextBytes(randomBytes);
		randomBytes[6] &= 0x0f; /* clear version */
		randomBytes[6] |= 0x40; /* set to version 4 */
		randomBytes[8] &= 0x3f; /* clear variant */
		randomBytes[8] |= 0x80; /* set to IETF variant */

		/* uuid */
		long msb = 0;
		long lsb = 0;
		assert randomBytes.length == 16 : "data must be 16 bytes in length";
		for (int i = 0; i < 8; i++)
			msb = (msb << 8) | (randomBytes[i] & 0xff);
		for (int i = 8; i < 16; i++)
			lsb = (lsb << 8) | (randomBytes[i] & 0xff);

		return toString(msb, lsb);
	}

	private static String toString(long mostSigBits, long leastSigBits) {
		return (digits(mostSigBits >> 32, 8) + digits(mostSigBits >> 16, 4) + digits(mostSigBits, 4)
				+ digits(leastSigBits >> 48, 4) + digits(leastSigBits, 12));
	}

	private static String digits(long val, int digits) {
		long hi = 1L << (digits * 4);
		return Long.toHexString(hi | (val & (hi - 1))).substring(1);
	}

	public static String random(Map<String, Integer> map) {
		int total = 0;
		for (Integer w : map.values()) {
			total += (w == null ? 0 : w);
		}
		int i = RandomUtil.random(0, total);
		for (Entry<String, Integer> z : map.entrySet()) {
			if (z.getValue() >= i) {
				return z.getKey();
			}
			i -= z.getValue();
		}
		return null;
	}
}