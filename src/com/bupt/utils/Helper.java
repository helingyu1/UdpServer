package com.bupt.utils;

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

}
