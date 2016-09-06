package com.bupt.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class Helper {

	/**
	 * ��16�����ַ��������������ַ�һ�飨��һ�ֽڴ��� �磬����6611�����[66,11]
	 * 
	 * @param hex
	 *            �����ַ���
	 * @return
	 */
	public static String[] hexToStringArray(String hex) {
		String[] arr = new String[hex.length() / 2];
		for (int i = 0, j = 0; i < hex.length() - 1; i = i + 2, j++) {
			String temp = hex.substring(i, i + 2);
			arr[j] = temp;
		}
		return arr;
	}

	/**
	 * 16���Ʋ���λ��
	 * 
	 * @param input
	 * @param size
	 * @param symbol
	 * @return
	 */
	public static String fill(String input, int size, char symbol) {

		while (input.length() < size) {
			input = symbol + input;
		}
		return input;
	}

	public static byte[] getBytes(char[] chars) {
		Charset cs = Charset.forName("UTF-8");
		CharBuffer cb = CharBuffer.allocate(chars.length);
		cb.put(chars);
		cb.flip();
		ByteBuffer bb = cs.encode(cb);

		return bb.array();
	}

	public static char[] getChars(byte[] bytes) {
		Charset cs = Charset.forName("UTF-8");
		ByteBuffer bb = ByteBuffer.allocate(bytes.length);
		bb.put(bytes);
		bb.flip();
		CharBuffer cb = cs.decode(bb);

		return cb.array();
	}
	public static String[] char2StringArray(char[] ch){
		String[] rtn = new String[ch.length];
		for(int i=0;i<rtn.length;i++)
			rtn[i] = fill(Integer.toHexString((int)ch[i]),2,'0');
		return rtn;
	}

	public static void main(String[] args) {
		System.out.println(fill("abc", 4, '0'));
	}

}
