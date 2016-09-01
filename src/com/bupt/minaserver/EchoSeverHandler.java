package com.bupt.minaserver;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

public class EchoSeverHandler extends IoHandlerAdapter {

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
		// �յ����ֽ�����
		byte[] bytes = buffer.array();
		// step2:��������
		char swt = (char) bytes[0];
		if (swt == 0) { //д���ݿ�
			System.out.println("test:�����֧��1��");
			store_to_database();
		} else if (swt == 99) { // ���������Ƿ�����
			System.out.println("test:�����֧��2��");
			detect_alive();
		} else if (swt > 100 && swt < 128) { // ���������Ϳ������������
			System.out.println("test:�����֧��3��");
			outside_send_to_socket();
		} else if (swt >= 1 && swt < 128) { // �鿴��������Ƿ�����
			System.out.println("test:�����֧��4��");
			send_to_socket();
		} else if (swt >= 128) { // ���ݰ���������ֱ�ӷ����ֻ�
			System.out.println("test:�����֧��5��");
			send_to_mobile();
		}
		// String body = buffer.getString(decoder);
		// ע�⣺���ͻ�ʹ�ò�������MINA�������£����¹ٷ���
		// ���Ķ�ȡ�������������ײ����ּ����ֽڵ�δ֪����
		// message.toString()
		// System.out.println("��NOTE��>>>>>> �յ��ͻ��˵����ݣ�" + body);
		//
		// // *********************************************** �ظ�����
		// String strToClient = "Hello������Server���ҵ�ʱ�����"
		// + System.currentTimeMillis();
		// byte[] res = strToClient.getBytes("UTF-8");
		// // ��֯IoBuffer���ݰ��ķ������������ſ�����ȷ���ÿͻ���UDP�յ�byte����
		// IoBuffer buf = IoBuffer.wrap(res);
		//
		// // ��ͻ���д����
		// WriteFuture future = session.write(buf);
		// // ��100���볬ʱ���ڵȴ�д���
		// future.awaitUninterruptibly(100);
		// // The message has been written successfully
		// if (future.isWritten()) {
		// // send sucess!
		// }
		// // The messsage couldn't be written out completely for some reason.
		// // (e.g. Connection is closed)
		// else {
		// System.out.println("[IMCORE]�ظ����ͻ��˵����ݷ���ʧ�ܣ�");
		// }
	}

	/**
	 * д�����ݿ�(���յ��İ��н�����wifi_id,wifi_ipv4,wifi_ipv4_port)
	 * д���ű� record �� heartnumber
	 */
	public void store_to_database() {
		
		//step1:дheartnumber��
		//step2:дrecord��

	}

	/**
	 * ��������(�����ݿ�����ȡ��wifi_ipv4,wifi_ipv4_port��䵽���ݰ���)
	 */
	public void send_to_socket() {
		
		// step1:��record���в�ѯwifi_ipv4,wifi_ipv4_port
		
		// step2:���ݲ��ip �˿ںţ����䷢����Ϣ�������Ƿ�����
		
		// step3:���ֻ�������Ӧ��Ϣ

	}

	/**
	 * �����ֻ�(���ݰ���������ֱ�ӷ����ֻ�)
	 */
	public void send_to_mobile() {
		// �յ���Ϣ��ֱ��ԭ�ⲻ��ԭ·������
	}

	/**
	 * ��Ȿ�������Ƿ�����
	 */
	public void detect_alive() {
		// �յ��������ʱ����Ϣ������
		// ���յ�����Ϣ recv_buf[18]==0x5f && recv_buf[19]==0x3f :heartbeat reply success!
	}

	/**
	 * 
	 */
	public void outside_send_to_socket() {
		// ��sent_to_socket���
	}
	public static void main(String[] args){
		char a = 0x62;
		if(a==98)
			System.out.println("11");
		
	}
}
