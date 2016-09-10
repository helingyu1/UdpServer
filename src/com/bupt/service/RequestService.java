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
 * 用来处理不同请求的service层
 * 
 * @author helingyu
 * 
 */
public class RequestService {
	private final Logger logger = Logger.getLogger(RequestService.class);
	private DaoService service = new DaoService();

	// 数据偏移量
	private static final int COMID_OFFSET = 1; // comid起始地址（8字节）
	private static final int MAC_OFFSET = 14; // mac地址起始地址(6字节)
	private static final int ACTION_OFFSET = 36;// outside方法里用，动作参数起始地址（1字节），0是关，1是开
	private static final int PARA_OFFSET = 20; // 设备密码起始地址，16字节（目前都是0？）

	// 标志
	private static final int NO_SOCKET_ADDR = 4;
	private static final int NO_ERROR = 0;
	private static final int NO_PERMISSION= 5;
	private static final int MSG_ERROR_STATUS = 128; //在wifi_socket_server.h里

	/**
	 * 写入数据库(从收到的包中解析出wifi_id,wifi_ipv4,wifi_ipv4_port) 写两张表 record 和
	 * heartnumber
	 */
	public void store_to_database(IoSession session, AcessPoint ap) {

		Record record = new Record();
		record.setWifi_id(ap.getWifi_id());
		record.setWifi_ipv4(ap.getIp());
		record.setWifi_ipv4_port(ap.getPort());
		// step1:写heartnumber表
		service.writeToHeartNumber(ap.getWifi_id());
		// step2:写record表
		service.writeToRecord(record);
		// step3:生成响应，返回给ap
		WriteFuture future = session.write("aaaa");

	}

	/**
	 * 发往插座(从数据库中提取出wifi_ipv4,wifi_ipv4_port填充到数据包中)
	 */
    public void send_to_socket(IoSession session, AcessPoint ap) {

        byte[] tel_buf = new byte[37];    // 发送给手机的响应内容
        byte[] newbuf = RequestService.String2Byte(ap.getRecv());    //发送给wifi插座的内容

        tel_buf[0] = (byte) MSG_ERROR_STATUS;
        //设置tel_buf中的macid
        for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
            tel_buf[i] = (byte) Integer.parseInt(ap.getRecv()[i], 16);
        }
        
        // step1:在record表中查询wifi_ipv4,wifi_ipv4_port
        Record record = DBDaoImpl.getInfoFromRecord(ap);
        if (!record.isRecorded()) {
            logger.debug("read database error");
            // 如果不存在记录，向手机发送错误响应消息
            tel_buf[PARA_OFFSET] = (byte) NO_SOCKET_ADDR;
            if (!send(session, tel_buf)) {
                logger.warn("send to mobile error");
            }
            return;
        }
        logger.info("Succeed!Find wifi ip and port for mac_id:"+ap.getWifi_id());
        // step2:根据查出ip 端口号，向其发送信息，测试是否在线
        // 将手机ip和端口号写入newbuf
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

        // step3:向wifi插座转发信息
        logger.debug("newbuf is:"+Arrays.toString(newbuf));
        if(send(session, newbuf, new InetSocketAddress(Helper.longToIp(record.getWifi_ipv4()), record.getWifi_ipv4_port()))){
        	logger.debug("Succeed!Send to wifi socket finished!");
        }else{
        	logger.warn("Failure!Send to wifi socket failed!");
        }
        

        // step4:向手机发送正确响应信息
        tel_buf[PARA_OFFSET] = NO_ERROR;
        if (!send(session, tel_buf)) {
            logger.warn("send to mobile error\n");
        }
        logger.debug("Succeed!");
    }
	

	/**
	 * 发送手机(数据包不作处理直接发往手机)
	 */
	public void send_to_mobile(AcessPoint ap) {
		// 收到消息后，直接原封不动原路径返回
	}

	/**
	 * 检测本服务器是否在线
	 */
	public void detect_alive(IoSession session, AcessPoint ap) {
		String[] rtn = new String[20];
		for (int i = 0; i < rtn.length; i++)
			rtn[i] = "00";
		// 收到请求，添加时间信息，返回
		// 获取当前时间信息
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
		rtn[6] = year.substring(2, 4);// 年份低字节
		rtn[7] = year.substring(0, 2);// 年份高字节
		rtn[8] = month;
		rtn[9] = day;
		rtn[10] = hour;
		rtn[11] = min;
		rtn[12] = sec;

//		rtn[6] = (char) Integer.parseInt(year.substring(2, 4), 16);// 年份低字节
//		rtn[7] = (char) Integer.parseInt(year.substring(0, 2), 16);// 年份高字节
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
		// 若收到的消息 recv_buf[18]==0x5f && recv_buf[19]==0x3f :heartbeat reply
		// success!
	}

	/**
	 * 接收来自外部设备的请求信息
	 * @param session	与外部设备的会话	
	 * @param ap	外部设备实体类
	 */
	public void outside_send_to_socket(IoSession session, AcessPoint ap) {
        //计算发送给wifi插座的newbuf的长度
        int paramsLen = ap.getRecv().length - ACTION_OFFSET;
        int rawLen = paramsLen + 16;
        int cookLen = (((int) (rawLen / 16)) + ((rawLen % 16 == 0) ? 0 : 1)) * 16;
        int resultLen= 21+ cookLen;
        byte[] tel_buf = new byte[37];//向手机返回的信息
        byte[] newbuf = new byte[resultLen];//向wifi发送的信息
        logger.info("the resultlen is:");
        logger.info(resultLen);

        tel_buf[0] = (byte) MSG_ERROR_STATUS;
        tel_buf[PARA_OFFSET] = NO_ERROR;

        for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
            tel_buf[i] = (byte) Integer.parseInt(ap.getRecv()[i], 16);
        }
        for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
            newbuf[i] = (byte) Integer.parseInt(ap.getRecv()[i], 16);
        }

        StringBuffer sb = new StringBuffer();
        for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
            sb.append(ap.getRecv()[i]);
        }
        String mac_id = new String(sb);
        System.out.println("mac_id is:" + mac_id);
        // 设置com_id
        sb = new StringBuffer();
        for (int i = COMID_OFFSET; i < COMID_OFFSET + 8; i++) {
            sb.append(ap.getRecv()[i]);
        }
        String com_id = new String(sb);
        System.out.println("com_id is:" + com_id);

        // 跟sent_to_socket差不多
        // step1:先在record表中查找记录
        Record record = DBDaoImpl.getInfoFromRecord(ap);
        boolean isRecorded = record.isRecorded();
        if (!isRecorded) {
            // 设置未查到信息
            logger.warn("read database error\\n");
            tel_buf[PARA_OFFSET] = NO_SOCKET_ADDR;
        }
        // step2:在comsocket表里查询是否存在此comid的记录
        boolean hasPermission = DBDaoImpl.hasItemInComsocket(mac_id, com_id);
        if (!hasPermission) {
            //设置为未授权
            tel_buf[PARA_OFFSET] = NO_PERMISSION;
        }
        // 如果step1，step2 任一一步不存在，就向手机返回信息
        if(!(NO_ERROR+"").equals(tel_buf[PARA_OFFSET])) {
            if (!send(session, tel_buf)) {
                logger.warn("send to mobile error\n");
            }
            return;
        }
        //加密
        //generate_AES_MD5(&host,buf+ACTION_OFFSET,1,newbuf);???

        // step3:向wifi发信息
        String[] mobileIpString = ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress().split(".");
        byte[] mobileIp = new byte[4];
        for (int i = 0; i < mobileIp.length; i++) {
            mobileIp[i] = (byte) Integer.parseInt(mobileIpString[i]);
        }
        logger.info("outside_send_to_socket() mobileIp=");
        logger.info(mobileIp.toString());
        for (int i = 8; i < 12; i++) {
            newbuf[i] = mobileIp[i - 8];
        }
        newbuf[13] = (byte) ((InetSocketAddress) session.getRemoteAddress()).getPort();
        newbuf[12] = (byte) (((InetSocketAddress) session.getRemoteAddress()).getPort() >> 8);

        if(true){
            logger.info(newbuf);
            logger.info("this is what we send to socket\r\n");
        }

        IoFuture connFuture = new NioDatagramConnector().connect(new InetSocketAddress(
                Helper.longToIp(record.getWifi_ipv4()), record.getWifi_ipv4_port()));
        IoSession sessionWifi = connFuture.getSession();
        if (!send(sessionWifi, newbuf)) {
            logger.warn("send to wifi socket error");
        }
        tel_buf[PARA_OFFSET] = NO_ERROR;
        if (!send(session, tel_buf)) {
            logger.warn("send to mobile error\n");
        }

    }

	/**
	 * 向客户端发送信息
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
