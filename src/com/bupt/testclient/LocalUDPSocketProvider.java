package com.bupt.testclient;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class LocalUDPSocketProvider {

	private static LocalUDPSocketProvider instance = null;
	private DatagramSocket localUDPSocket = null;

	public static LocalUDPSocketProvider getInstance() {
		if (instance == null)
			instance = new LocalUDPSocketProvider();
		return instance;
	}

	public void initSocket() {
		try {
			System.out.println("????");
			// UDP���ؼ����˿ڣ����Ϊ0����ʾ��ϵͳ���䣬����ʹ��ָ���˿ڣ�
			this.localUDPSocket = new DatagramSocket(12345);
			// ����connect֮��ÿ��sendʱDatagramPacket�Ͳ���Ҫ���Ŀ��������ip��port��
			// * ע�⣺connect����һ��Ҫ��DatagramSocket.receive()����֮ǰ���ã�
			// * ��Ȼ��send���ݽ��ᱻ�����������������ǹٷ�API��bug��Ҳ�����ǵ�
			// * �ù淶��Ӧ����������û���ҵ��ٷ���ȷ��˵��
			this.localUDPSocket.connect(
					InetAddress.getByName("127.0.0.1"),
					7777);
			this.localUDPSocket.setReuseAddress(true);
			System.out.println("new DatagramSocket()�ѳɹ����.");
		} catch (Exception e) {
			System.out.println("localUDPSocket����ʱ����ԭ���ǣ�" + e.getMessage());
		}
	}

	public DatagramSocket getLocalUDPSocket() {
		return this.localUDPSocket;
	}

}
