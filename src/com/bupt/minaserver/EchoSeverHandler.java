package com.bupt.minaserver;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.bupt.entity.AcessPoint;
import com.bupt.service.RequestService;
import com.bupt.utils.Helper;

public class EchoSeverHandler extends IoHandlerAdapter {
	// service
	RequestService service = new RequestService();

	// ����ƫ����
	private static final int MAC_OFFSET = 14;
	private static final int ACTION_OFFSET = 36;// outside��������
	private static final int PARA_OFFSET = 20;

	// ��־
	private static final int NO_SOCKET_ADDR = 4;
	private static final int NO_ERROR = 0;

	public static final CharsetDecoder decoder = (Charset.forName("UTF-8"))
			.newDecoder();

	/**
	 * MINA���쳣�ص�������
	 * <p>
	 * �����н����쳣����ʱ������close��ǰ�Ự��
	 * 
	 * @param session
	 *            �����쳣�ĻỰ
	 * @param cause
	 *            �쳣����
	 * @see IoSession#close(boolean)
	 */
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		// logger.error("[IMCORE]exceptionCaught���񵽴��ˣ�ԭ���ǣ�" +
		// cause.getMessage(),
		// cause);
		session.close(true);
	}

	/**
	 * MINA������յ��ͻ�����Ϣ�Ļص�������
	 * <p>
	 * ���ཫ�ڴ˷�����ʵ�������ļ�ʱͨѶ���ݽ����ʹ�����ԡ�
	 * <p>
	 * Ϊ�������������ܣ��������������ڶ�����MINA��IoProcessor֮����̳߳��У� ���
	 * {@link ServerLauncher#initAcceptor()}�е�MINA���ô��� ��
	 * 
	 * @param session
	 *            �յ���Ϣ��Ӧ�ĻỰ����
	 * @param message
	 *            �յ���MINA��ԭʼ��Ϣ��װ���󣬱������� {@link IoBuffer}����
	 * @throws Exception
	 *             ���д�����ʱ���׳��쳣
	 * 
	 *             ps:��������wifi socket����mobile����Ϣ
	 */
	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		// *********************************************** ��������
		// step1:��ȡ�յ�������
		IoBuffer buffer = (IoBuffer) message;
		System.out.println(Arrays.toString(buffer.array()));
//		String str = buffer.getString(decoder);
//		String[] recv = Helper.hexToStringArray(str);
		byte[] recv = buffer.array();
		
		// �õ�wifi_id
		StringBuffer sb = new StringBuffer();
		for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
			sb.append(recv[i]);
		}
		String mac_id = new String(sb);
		InetSocketAddress addr = (InetSocketAddress) session.getRemoteAddress();
		AcessPoint ap = new AcessPoint(addr.getAddress().getHostAddress(),
				addr.getPort(), mac_id,recv);
		System.out.println(ap);

		// step2:��������
//		int swt = Integer.parseInt(recv[0], 16);
		int swt = recv[0];
		if (swt == 0) { // ����1��д������Ϣ�����ݿ�
			System.out.println("test:�����֧��1��");
			service.store_to_database(session,ap);
		} else if (swt == 99) { // ����2�����������Ƿ�����
			System.out.println("test:�����֧��2��");
			service.detect_alive(session,ap);
		} else if (swt > 100 && swt < 128) { // ����3�����������Ϳ������������
			System.out.println("test:�����֧��3��");
			service.outside_send_to_socket(ap);
		} else if (swt >= 1 && swt < 128) { // ����4���鿴��������Ƿ�����
			System.out.println("test:�����֧��4��");
			service.send_to_socket(ap);
		} else if (swt >= 128) { // ����5�����ݰ���������ֱ�ӷ����ֻ�
			System.out.println("test:�����֧��5��");
			service.send_to_mobile(ap);
		}
	}
	

	public static void main(String[] args) {

		String hex = "1111";
		String[] arr = Helper.hexToStringArray(hex);
		System.out.println(arr.length);
		for (int i = 0; i < arr.length; i++)
			System.out.println(arr[i]);

	}
}
