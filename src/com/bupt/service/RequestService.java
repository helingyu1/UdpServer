package com.bupt.service;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;

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
    public void send_to_socket(IoSession session, AcessPoint ap) {

        byte[] tel_buf = new byte[37];    // ���͸��ֻ�����Ӧ����
        byte[] newbuf = RequestService.String2Byte(ap.getRecv());    //���͸�wifi����������

        tel_buf[0] = (byte) MSG_ERROR_STATUS;
        //����tel_buf�е�macid
        for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
            tel_buf[i] = (byte) Integer.parseInt(ap.getRecv()[i], 16);
        }
        
        // step1:��record���в�ѯwifi_ipv4,wifi_ipv4_port
        Record record = DBDaoImpl.getInfoFromRecord(ap);
        if (!record.isRecorded()) {
            logger.debug("read database error");
            // ��������ڼ�¼�����ֻ����ʹ�����Ӧ��Ϣ
            tel_buf[PARA_OFFSET] = (byte) NO_SOCKET_ADDR;
            if (!send(session, tel_buf)) {
                logger.warn("send to mobile error");
            }
            return;
        }
        logger.info("Succeed!Find wifi ip and port for mac_id:"+ap.getWifi_id());
        // step2:���ݲ��ip �˿ںţ����䷢����Ϣ�������Ƿ�����
        // ���ֻ�ip�Ͷ˿ں�д��newbuf
        String mobileIpString = ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress();

        String[] mobileIpStringArray = mobileIpString.split("\\.");
        logger.debug("telephone ip is:"+Arrays.toString(mobileIpStringArray));
        byte[] mobileIp = new byte[4];
        for (int i = 0; i < mobileIp.length; i++) {
            mobileIp[i] = (byte) Integer.parseInt(mobileIpStringArray[i]);
        }
        logger.info("send_to_socket() mobileIp="+Arrays.toString(mobileIp));
        for (int i = 8; i < 12; i++) {
            newbuf[i] = mobileIp[i - 8];
        }
        newbuf[13] = (byte) ((InetSocketAddress) session.getRemoteAddress()).getPort();
        newbuf[12] = (byte) (((InetSocketAddress) session.getRemoteAddress()).getPort() >> 8);
        
        if (newbuf[18] == 0x5f && newbuf[19] == 0x3f) {
            logger.info(newbuf);
            logger.info("this is what we send to socket\r\n");
        }

        // step3:��wifi����ת����Ϣ
        logger.debug("newbuf is:"+Arrays.toString(newbuf));
        if(send(session, newbuf, new InetSocketAddress(Helper.longToIp(record.getWifi_ipv4()), record.getWifi_ipv4_port()))){
        	logger.debug("Succeed!Send to wifi socket finished!");
        }else{
        	logger.warn("Failure!Send to wifi socket failed!");
        }
        

        // step4:���ֻ�������ȷ��Ӧ��Ϣ
        tel_buf[PARA_OFFSET] = NO_ERROR;
        if (!send(session, tel_buf)) {
            logger.warn("send to mobile error\n");
        }
        logger.debug("Succeed!");
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
		String[] rtn = new String[20];
		for (int i = 0; i < rtn.length; i++)
			rtn[i] = "00";
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
		rtn[6] = year.substring(2, 4);// ��ݵ��ֽ�
		rtn[7] = year.substring(0, 2);// ��ݸ��ֽ�
		rtn[8] = month;
		rtn[9] = day;
		rtn[10] = hour;
		rtn[11] = min;
		rtn[12] = sec;

//		rtn[6] = (char) Integer.parseInt(year.substring(2, 4), 16);// ��ݵ��ֽ�
//		rtn[7] = (char) Integer.parseInt(year.substring(0, 2), 16);// ��ݸ��ֽ�
//		rtn[8] = (char) (Integer.parseInt(month, 16));
//		rtn[9] = (char) (Integer.parseInt(day, 16));
//		rtn[10] = (char) (Integer.parseInt(hour, 16));
//		rtn[11] = (char) (Integer.parseInt(min, 16));
//		rtn[12] = (char) Integer.parseInt(sec, 16);
//		System.out.println(Arrays.toString(Helper.char2StringArray(rtn)));
//		WriteFuture writeFuture = send(session, rtn);
//		if (writeFuture.isWritten()) {
//			logger.debug("heartbeat reply  success!");
//			
//		}
		// ���յ�����Ϣ recv_buf[18]==0x5f && recv_buf[19]==0x3f :heartbeat reply
		// success!
	}

	/**
	 * ���������ⲿ�豸��������Ϣ
	 * @param session	���ⲿ�豸�ĻỰ	
	 * @param ap	�ⲿ�豸ʵ����
	 */
	public void outside_send_to_socket(IoSession session,AcessPoint ap) {
		String[] tel_buf = new String[37];//���ֻ����ص���Ϣ
		char[] newbuf = new char[37];//��wifi���͵���Ϣ�����Ȳ��ԣ�
		tel_buf[0] = MSG_ERROR_STATUS+"";
		tel_buf[PARA_OFFSET] = NO_ERROR+"";
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
			tel_buf[PARA_OFFSET] = NO_SOCKET_ADDR+"";
		}
		
		
		// step2:��comsocket�����ѯ�Ƿ���ڴ�comid�ļ�¼
		boolean hasPermission = DBDaoImpl.hasItemInComsocket(mac_id, com_id);
		if(!hasPermission){
			//������Ȩ��Ϣ
			tel_buf[PARA_OFFSET] = NO_PERMISSION+"";
		}
		
		// ���step1��step2 ��һһ�������ڣ������ֻ�������Ϣ
		if(!(NO_ERROR+"").equals(tel_buf[PARA_OFFSET])){
//			send(session, tel_buf);
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
	public boolean send(IoSession session, byte[] data) {
		IoBuffer buffer = IoBuffer.wrap(data);
		WriteFuture future = session.write(buffer);
		future.awaitUninterruptibly(100);
		return future.isWritten();
	}
	public boolean send(IoSession session,byte[] data,InetSocketAddress addr){
		IoBuffer buffer = IoBuffer.wrap(data);
		WriteFuture future = session.write(buffer,addr);
		future.awaitUninterruptibly(100);
		return future.isWritten();
	}
	
	public static byte[] String2Byte(String[] s) {
        byte[] res = new byte[s.length];
        for (int i = 0; i < s.length; i++) {
            res[i] = (byte) Integer.parseInt(s[i], 16);
        }
        return res;
    }

	public static void main(String[] args) {
		String a = "e";
		byte b = (byte) Integer.parseInt(a, 16);
		System.out.println(b);
		// detect_alive(null);
	}
}
