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
//			String toServer = "Hi�����ǿͻ��ˣ��ҵ�ʱ���" + System.currentTimeMillis();
//			String toServer="6D000000000000000000000000001afe34f754e10000000000000000000000000000000000";
			String toServer="6300000000000000000000000000000000000000";//�������Ƿ�����
//			byte[] soServerBytes = toServer.getBytes("UTF-8");
			byte[] aa = {0x63,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
			// ��ʼ����
//			boolean ok = UDPUtils.send(soServerBytes, soServerBytes.length);
			boolean ok = UDPUtils.send(aa, aa.length);
			if (ok){
				System.out.println("��������˵���Ϣ���ͳ�.");
				Thread.sleep(5000);
				UDPUtils.send(aa, aa.length);
				System.out.println("��һ�η���");
			}
			else
				System.out.println("��������˵���Ϣû�гɹ�����������");

			// 3000��������һ��ѭ��
			Thread.sleep(100000);
		}

	}

}
