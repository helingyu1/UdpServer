package com.bupt.testclient;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.bupt.utils.Helper;

public class EchoClient {

	/**
	 * @param args
	 * @throws UnsupportedEncodingException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws UnsupportedEncodingException, InterruptedException {
		// TODO Auto-generated method stub
		// ��ʼ������UDP��Socket
		LocalUDPSocketProvider.getInstance().initSocket();
		// ��������UDP���������������õģ�
		LocalUDPDataReciever.getInstance().startup();

		// ѭ���������ݸ������
		while (true) {
			// Ҫ���͵�����
//			String toServer="6300000000000000000000000000000000000000";//�������Ƿ�����
//			byte[] soServerBytes = toServer.getBytes("UTF-8");
//			byte[] aa = {0x63,0x63,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
			
//			byte[] aa = {0x63,0x63,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0x63};
			
			char[] aa = {0x63,0xe0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0x63};
			byte[] b = Helper.getBytes(aa);
//			System.out.println("�ͻ���ת���byte���飺"+Arrays.toString(b));
//			System.out.println("����ת��char���飺"+Arrays.toString(Helper.getChars(b)));
// ��ʼ����
//			boolean ok = UDPUtils.send(soServerBytes, soServerBytes.length);
			boolean ok = UDPUtils.send(b, b.length);
			if (ok){
				System.out.println("��������˵���Ϣ���ͳ�.");
			}
			else
				System.out.println("��������˵���Ϣû�гɹ�����������");

			// 3000��������һ��ѭ��
			Thread.sleep(100000);
		}

	}

}
