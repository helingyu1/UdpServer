package com.bupt.testclient;

import java.io.UnsupportedEncodingException;

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
			String toServer = "Hi�����ǿͻ��ˣ��ҵ�ʱ���" + System.currentTimeMillis();
			byte[] soServerBytes = toServer.getBytes("UTF-8");

			// ��ʼ����
			boolean ok = UDPUtils.send(soServerBytes, soServerBytes.length);
			if (ok)
				System.out.println("��������˵���Ϣ���ͳ�.");
			else
				System.out.println("��������˵���Ϣû�гɹ�����������");

			// 3000��������һ��ѭ��
			Thread.sleep(3000);
		}

	}

}
