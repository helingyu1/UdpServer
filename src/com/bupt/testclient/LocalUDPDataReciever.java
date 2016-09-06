package com.bupt.testclient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

import com.bupt.utils.Helper;

public class LocalUDPDataReciever {

	private static final String TAG = LocalUDPDataReciever.class
			.getSimpleName();
	private static LocalUDPDataReciever instance = null;
	private Thread thread = null;

	public static LocalUDPDataReciever getInstance() {
		if (instance == null)
			instance = new LocalUDPDataReciever();
		return instance;
	}

	public void startup() {
		this.thread = new Thread(new Runnable() {
			public void run() {
				try {
//					Log.d(LocalUDPDataReciever.TAG, "����UDP�˿������У��˿�="
//							+ ConfigEntity.localUDPPort + "...");

					// ��ʼ����
					LocalUDPDataReciever.this.udpListeningImpl();
				} catch (Exception eee) {
//					Log.w(LocalUDPDataReciever.TAG, "����UDP����ֹͣ��(socket���ر���?),"
//							+ eee.getMessage(), eee);
				}
			}
		});
		this.thread.start();
	}

	private void udpListeningImpl() throws Exception {
		while (true) {
			byte[] data = new byte[1024];
			// �������ݱ��İ�
			DatagramPacket packet = new DatagramPacket(data, data.length);

			DatagramSocket localUDPSocket = LocalUDPSocketProvider
					.getInstance().getLocalUDPSocket();
			if ((localUDPSocket == null) || (localUDPSocket.isClosed()))
				continue;

			// ����ֱ���յ�����
			localUDPSocket.receive(packet);

			// ��������˷�����������
//			String pFromServer = new String(packet.getData(), 0,
//					packet.getLength(), "UTF-8");
//			System.out.println("��NOTE��>>>>>> �յ�����˵���Ϣ��"
//					+ pFromServer);
			System.out.println("��NOTE��>>>>>> �յ�����˵���Ϣ��"+Arrays.toString(Helper.char2StringArray(Helper.getChars(packet.getData()))));
			char[] aa = {0x63,0xe0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0x63};
			byte[] b = Helper.getBytes(aa);
			boolean ok = UDPUtils.send(b, b.length);
			if (ok){
				System.out.println("��Ӧ��Ϣ���ͳ�.");
			}
			else
				System.out.println("��������˵���Ϣû�гɹ�����������");
		}
	}

}
