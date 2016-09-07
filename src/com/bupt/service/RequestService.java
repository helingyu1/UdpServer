package com.bupt.service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.IoSession;

import com.bupt.dao.DBDaoImpl;
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
	private final Logger logger = Logger.getLogger(RequestService.class);
	private DaoService service = new DaoService();

	// ����ƫ����
	private static final int COMID_OFFSET = 1; // comid��ʼ��ַ��8�ֽڣ�
	private static final int MAC_OFFSET = 14; // mac��ַ��ʼ��ַ(6�ֽ�)
	private static final int ACTION_OFFSET = 36;// outside�������ã�����������ʼ��ַ��1�ֽڣ���0�ǹأ�1�ǿ�
	private static final int PARA_OFFSET = 20; // �豸������ʼ��ַ��16�ֽڣ�Ŀǰ����0����

	// ��־
	private static final int NO_SOCKET_ADDR = 4;
	private static final int NO_ERROR = 0;
	private static final int NO_PERMISSION= 5;
	private static final int MSG_ERROR_STATUS = 128; //��wifi_socket_server.h��

	/**
	 * д�����ݿ�(���յ��İ��н�����wifi_id,wifi_ipv4,wifi_ipv4_port) д���ű� record ��
	 * heartnumber
	 */
	public void store_to_database(IoSession session, AcessPoint ap) {

		Record record = new Record();
		record.setWifi_id(ap.getWifi_id());
		record.setWifi_ipv4(ap.getIp());
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
	public void send_to_socket(IoSession session,AcessPoint ap) {
		
		char[] tel_buf = new char[37];	// ���͸��ֻ�����Ӧ��Ϣ
		char[] newbuf = ap.getRecv();	//���͸���������Ϣ
		
		tel_buf[0] = MSG_ERROR_STATUS;
		// ����tel_buf�е�macid
		for(int i=MAC_OFFSET;i<MAC_OFFSET+6;i++){
			tel_buf[i] = ap.getRecv()[i];
		}
		
		
		

		// step1:��record���в�ѯwifi_ipv4,wifi_ipv4_port
//		Record record = DBDaoImpl.getInfoFromRecord(ap);
//		if(!record.isRecorded()){
//			// ��������ڼ�¼�����ֻ����ʹ�����Ӧ��Ϣ
//			tel_buf[PARA_OFFSET] = NO_SOCKET_ADDR;
//			send(session, tel_buf);
//		}
		logger.debug("������");
		// step2:���ݲ��ip �˿ںţ����䷢����Ϣ�������Ƿ�����
		
		// step3:���ֻ�������ȷ��Ӧ��Ϣ
		tel_buf[PARA_OFFSET] = NO_ERROR;
		send(session,tel_buf);

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
	public void detect_alive(IoSession session, AcessPoint ap) {
		char[] rtn = new char[20];
		for (int i = 0; i < rtn.length; i++)
			rtn[i] = 0x00;
		// �յ��������ʱ����Ϣ������
		// ��ȡ��ǰʱ����Ϣ
		Calendar cal = Calendar.getInstance();
		String year = Helper.fill(Integer.toHexString(cal.get(Calendar.YEAR)),
				4, '0');
		String month = Helper.fill(
				Integer.toHexString(cal.get(Calendar.MONTH) + 1), 2, '0');
		String day = Helper.fill(Integer.toHexString(cal.get(Calendar.DATE)),
				2, '0');
		String hour = Helper.fill(
				Integer.toHexString(cal.get(Calendar.HOUR_OF_DAY)), 2, '0');
		String min = Helper.fill(Integer.toHexString(cal.get(Calendar.MINUTE)),
				2, '0');
		String sec = Helper.fill(Integer.toHexString(cal.get(Calendar.SECOND)),
				2, '0');

		rtn[6] = (char) Integer.parseInt(year.substring(2, 4), 16);// ��ݵ��ֽ�
		rtn[7] = (char) Integer.parseInt(year.substring(0, 2), 16);// ��ݸ��ֽ�
		rtn[8] = (char) (Integer.parseInt(month, 16));
		rtn[9] = (char) (Integer.parseInt(day, 16));
		rtn[10] = (char) (Integer.parseInt(hour, 16));
		rtn[11] = (char) (Integer.parseInt(min, 16));
		rtn[12] = (char) Integer.parseInt(sec, 16);
//		System.out.println(Arrays.toString(Helper.char2StringArray(rtn)));
		WriteFuture writeFuture = send(session, rtn);
		if (writeFuture.isWritten()) {
			logger.debug("heartbeat reply  success!");
			
		}
		// ���յ�����Ϣ recv_buf[18]==0x5f && recv_buf[19]==0x3f :heartbeat reply
		// success!
	}

	/**
	 * ���������ⲿ�豸��������Ϣ
	 * @param session	���ⲿ�豸�ĻỰ	
	 * @param ap	�ⲿ�豸ʵ����
	 */
	public void outside_send_to_socket(IoSession session,AcessPoint ap) {
		char[] tel_buf = new char[37];//���ֻ����ص���Ϣ
		char[] newbuf = new char[37];//��wifi���͵���Ϣ�����Ȳ��ԣ�
		tel_buf[0] = MSG_ERROR_STATUS;
		tel_buf[PARA_OFFSET] = NO_ERROR;
		StringBuffer sb = new StringBuffer();
		// �õ�wifi_id
		for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
			sb.append(ap.getRecv()[i]);
		}
		String mac_id = new String(sb);
		System.out.println("mac_id is:" + mac_id);
		// �õ�com_id
		sb = new StringBuffer();
		for(int i = COMID_OFFSET;i<COMID_OFFSET+8;i++){
			sb.append(ap.getRecv()[i]);
		}
		String com_id = new String(sb);
		System.out.println("com_id is:" + com_id);
		
		
		// ��sent_to_socket���
		// step1:����record���в��Ҽ�¼
		Record record = DBDaoImpl.getInfoFromRecord(ap);
		if(!record.isRecorded()){
			// ����δ�鵽��Ϣ
			tel_buf[PARA_OFFSET] = NO_SOCKET_ADDR;
		}
		
		
		// step2:��comsocket�����ѯ�Ƿ���ڴ�comid�ļ�¼
		boolean hasPermission = DBDaoImpl.hasItemInComsocket(mac_id, com_id);
		if(!hasPermission){
			//������Ȩ��Ϣ
			tel_buf[PARA_OFFSET] = NO_PERMISSION;
		}
		
		// ���step1��step2 ��һһ�������ڣ������ֻ�������Ϣ
		if(tel_buf[PARA_OFFSET]!=NO_ERROR){
			send(session, tel_buf);
			return;
		}
		// step3:��wifi����Ϣ
		
	}

	/**
	 * ��ͻ��˷�����Ϣ
	 * 
	 * @param session
	 * @param data
	 * @return
	 */
	public WriteFuture send(IoSession session, char[] data) {
		byte[] toSend = Helper.getBytes(data);
		IoBuffer buffer = IoBuffer.wrap(toSend);
		WriteFuture future = session.write(buffer);
		future.awaitUninterruptibly(100);
		return future;
	}

	public static void main(String[] args) {
		String a = "e";
		byte b = (byte) Integer.parseInt(a, 16);
		System.out.println(b);
		// detect_alive(null);
	}
}
