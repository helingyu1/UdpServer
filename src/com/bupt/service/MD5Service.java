package com.bupt.service;

import java.security.MessageDigest;
import java.util.Arrays;

import org.apache.log4j.Logger;

public class MD5Service {

	Logger logger = Logger.getLogger(MD5Service.class);

	/**
	 * 
	 * @param data
	 *            需要生成md5的数据
	 * @param length
	 *            数据长度
	 * @return
	 */
	public byte[] encode_md5(byte[] data, int length) {
		byte[] result = null;
		byte[] rawData = new byte[length];
		for(int i=0;i<length;i++)
			rawData[i] = data[i];
		try {
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
			mdInst.update(rawData);
			result = mdInst.digest();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void main(String[] args) {
		// System.out.println(Arrays.toString(encode_md5("".getBytes(),1)));
		byte[] a = "".getBytes();
		System.out.println(a.length);
	}
}
