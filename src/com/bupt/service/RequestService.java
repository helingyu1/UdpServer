package com.bupt.service;

import java.util.Arrays;
import java.util.Calendar;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import com.bupt.entity.AcessPoint;
import com.bupt.entity.Record;
import com.bupt.utils.Helper;

/**
 * ��������ͬ�����service��
 * 
 * @author helingyu
 * 
 */
public class RequestService {
	private DaoService service = new DaoService();

	// ����ƫ����
	private static final int MAC_OFFSET = 14; 	//mac��ַ��ʼ��ַ
	private static final int ACTION_OFFSET = 36;// outside�������ã�����������ʼ��ַ��1�ֽڣ���0�ǹأ�1�ǿ�
	private static final int PARA_OFFSET = 20;	//�豸������ʼ��ַ��16�ֽڣ�Ŀǰ����0����

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
	public static void detect_alive(AcessPoint ap) {
		String[] rtn = new String[20];
		for(int i=0;i<rtn.length;i++)
			rtn[i] = "00";
		// �յ��������ʱ����Ϣ������
		// ��ȡ��ǰʱ����Ϣ
		Calendar cal = Calendar.getInstance();
		String year = Helper.fill(Integer.toHexString(cal.get(Calendar.YEAR)),4,'0');
		String month = Helper.fill(Integer.toHexString(cal.get(Calendar.MONTH)+1),2,'0');
		String day = Helper.fill(Integer.toHexString(cal.get(Calendar.DATE)),2,'0');
		String hour = Helper.fill(Integer.toHexString(cal.get(Calendar.HOUR_OF_DAY)),2,'0');
		String min = Helper.fill(Integer.toHexString(cal.get(Calendar.MINUTE)),2,'0');
		String sec = Helper.fill(Integer.toHexString(cal.get(Calendar.SECOND)),2,'0');
		rtn[6] = year.substring(2, 4);//��ݵ��ֽ�
		rtn[7] = year.substring(0,2);//��ݸ��ֽ�
		rtn[8] = month;
		rtn[9] = day;
		rtn[10] = hour;
		rtn[11] = min;
		rtn[12] = sec;
		System.out.println(Arrays.toString(rtn));
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
	public static void main(String[] args){
		detect_alive(null);
	}
}
