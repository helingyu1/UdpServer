package com.bupt.service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoService;
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
	public void detect_alive(IoSession session,AcessPoint ap) {
		byte[] rtn = new byte[20];
		for(int i=0;i<rtn.length;i++)
			rtn[i] = 0x00;
		// �յ��������ʱ����Ϣ������
		// ��ȡ��ǰʱ����Ϣ
		Calendar cal = Calendar.getInstance();
		String year = Helper.fill(Integer.toHexString(cal.get(Calendar.YEAR)),4,'0');
		String month = Helper.fill(Integer.toHexString(cal.get(Calendar.MONTH)+1),2,'0');
		String day = Helper.fill(Integer.toHexString(cal.get(Calendar.DATE)),2,'0');
		String hour = Helper.fill(Integer.toHexString(cal.get(Calendar.HOUR_OF_DAY)),2,'0');
		String min = Helper.fill(Integer.toHexString(cal.get(Calendar.MINUTE)),2,'0');
		String sec = Helper.fill(Integer.toHexString(cal.get(Calendar.SECOND)),2,'0');
		
		rtn[6] = (byte)Integer.parseInt(year.substring(2, 4),16);//��ݵ��ֽ�
		rtn[7] = (byte)Integer.parseInt(year.substring(0,2),16);//��ݸ��ֽ�
		rtn[8] = (byte)(Integer.parseInt(month,16));
		rtn[9] = (byte)(Integer.parseInt(day,16));
		rtn[10] = (byte)(Integer.parseInt(hour,16));
		rtn[11] = (byte)(Integer.parseInt(min,16));
		rtn[12] = (byte)Integer.parseInt(sec,16);
		IoBuffer buffer = IoBuffer.wrap(rtn);
		
		WriteFuture future = session.write(rtn);
//		SocketAddress addr = new InetSocketAddress("127.0,0,1", ((InetSocketAddress)session.getRemoteAddress()).getPort());
//		
//		WriteFuture future = session.write(rtn, addr);
		System.out.println(((InetSocketAddress)session.getRemoteAddress()).getPort());
		future.awaitUninterruptibly();
		//�ж���Ϣ�Ƿ������
		if(future.isWritten()){
			System.out.println("���ͳɹ�������");
			ReadFuture rf = session.read();
			//�ȴ���Ϣ��Ӧ
			rf.awaitUninterruptibly();
			//�Ƿ���Ӧ�ɹ�
			if(rf.isDone()){
				System.out.println("���ճɹ�������");
				Object message = rf.getMessage();
				IoBuffer buf = (IoBuffer)message;
				System.out.println(buf.array());
				System.out.println(Arrays.toString(buffer.array()));
			}
		}else{
			System.out.println("ʧ���ˣ�");
		}
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
		String a = "e";
		byte b = (byte)Integer.parseInt(a, 16);
		System.out.println(b);
		//detect_alive(null);
	}
}
