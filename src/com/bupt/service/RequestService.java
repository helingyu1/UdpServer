package com.bupt.service;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import com.bupt.entity.AcessPoint;
import com.bupt.entity.Record;

/**
 * ��������ͬ�����service��
 * 
 * @author helingyu
 * 
 */
public class RequestService {
	private DaoService service = new DaoService();

	// ����ƫ����
	private static final int MAC_OFFSET = 14;
	private static final int ACTION_OFFSET = 36;// outside��������
	private static final int PARA_OFFSET = 20;

	// ��־
	private static final int NO_SOCKET_ADDR = 4;
	private static final int NO_ERROR = 0;

	/**
	 * д�����ݿ�(���յ��İ��н�����wifi_id,wifi_ipv4,wifi_ipv4_port) д���ű� record ��
	 * heartnumber
	 */
	public void store_to_database(IoSession session, AcessPoint ap) {

		Record record = new Record();
		record.setWifi_id(ap.getWifi_id());
		record.setWifi_ipv4(Integer.parseInt(ap.getIp()));
		record.setWifi_ipv4_port(ap.getPort());
		// step1:дheartnumber��
		service.writeToHeartNumber(ap.getWifi_id());
		// step2:дrecord��
		service.writeToRecord(record);
		// step3:������Ӧ�����ظ�ap
		WriteFuture future = session.write("aaaa");

	}

	/**
	 * ��������(�����ݿ�����ȡ��wifi_ipv4,wifi_ipv4_port��䵽���ݰ���)
	 */
	public void send_to_socket(AcessPoint ap) {

		// step1:��record���в�ѯwifi_ipv4,wifi_ipv4_port

		// step2:���ݲ��ip �˿ںţ����䷢����Ϣ�������Ƿ�����

		// step3:���ֻ�������Ӧ��Ϣ

	}

	/**
	 * �����ֻ�(���ݰ���������ֱ�ӷ����ֻ�)
	 */
	public void send_to_mobile(AcessPoint ap) {
		// �յ���Ϣ��ֱ��ԭ�ⲻ��ԭ·������
	}

	/**
	 * ��Ȿ�������Ƿ�����
	 */
	public void detect_alive(AcessPoint ap) {
		// �յ��������ʱ����Ϣ������
		// ���յ�����Ϣ recv_buf[18]==0x5f && recv_buf[19]==0x3f :heartbeat reply
		// success!
	}

	/**
	 * 
	 */
	public void outside_send_to_socket(AcessPoint ap) {
		StringBuffer sb = new StringBuffer();
		for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
			sb.append(ap.getRecv()[i]);
		}
		String mac_id = new String(sb);
		System.out.println("mac_id is:" + mac_id);
		// ��sent_to_socket���
	}
}
