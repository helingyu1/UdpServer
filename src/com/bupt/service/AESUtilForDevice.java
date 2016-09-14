package com.bupt.service;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author carlos carlosk@163.com
 * @version 鍒涘缓鏃堕棿锛�012-5-17 涓婂崍9:48:35 绫昏鏄�
 */

public class AESUtilForDevice {
	public static final String TAG = "AESUtils";
	// 鍒濆鍖栧悜閲�
	private final static byte[] IV = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05,
			0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };

	public static String encrypt(String seed, String clearText) {
		// Log.d(TAG, "鍔犲瘑鍓嶇殑seed=" + seed + ",鍐呭涓�" + clearText);
		byte[] result = null;
		try {
			byte[] rawkey = getRawKey(seed.getBytes());
			byte[] data = clearText.getBytes();
			result = encrypt(rawkey, data, 0, data.length);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String content = toHex(result);
		// Log.d(TAG, "鍔犲瘑鍚庣殑鍐呭涓�" + content);
		return content;

	}

	public static String decrypt(String seed, String encrypted) {
		// Log.d(TAG, "瑙ｅ瘑鍓嶇殑seed=" + seed + ",鍐呭涓�" + encrypted);
		byte[] rawKey;
		try {
			rawKey = getRawKey(seed.getBytes());
			byte[] enc = toByte(encrypted);
			byte[] result = decrypt(rawKey, enc);
			String coentn = new String(result);
			// Log.d(TAG, "瑙ｅ瘑鍚庣殑鍐呭涓�" + coentn);
			return coentn;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public static byte[] getRawKey(byte[] seed) throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(128, new SecureRandom(seed));
		return kgen.generateKey().getEncoded();
	}

	public static byte[] encrypt(byte[] key, byte[] clear, int inputOffset,
			int inputLen) throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(IV));
		return cipher.doFinal(clear, inputOffset, inputLen);
	}

	public static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(IV));
		return cipher.doFinal(encrypted);
	}

	public static String toHex(String txt) {
		return toHex(txt.getBytes());
	}

	public static String fromHex(String hex) {
		return new String(toByte(hex));
	}

	public static byte[] toByte(String hexString) {
		int len = hexString.length() / 2;
		byte[] result = new byte[len];
		for (int i = 0; i < len; i++)
			result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2),
					16).byteValue();
		return result;
	}

	public static String toHex(byte[] buf) {
		if (buf == null)
			return "";
		StringBuffer result = new StringBuffer(2 * buf.length);
		for (int i = 0; i < buf.length; i++) {
			appendHex(result, buf[i]);
		}
		return result.toString();
	}

	private static void appendHex(StringBuffer sb, byte b) {
		final String HEX = "0123456789ABCDEF";
		sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
	}

	public static byte[] getMD5(byte[] data, int offset, int len)
			throws Exception {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(data, offset, len);
		return md5.digest();// 鍔犲瘑
	}
}