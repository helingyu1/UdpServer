package com.bupt.service;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import com.bupt.dao.DBDaoImpl;
import com.bupt.entity.AcessPoint;
import com.bupt.entity.Global_info;
import com.bupt.entity.Record;
import com.bupt.entity.TSPackHeader;
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
	private static final int NO_PERMISSION = 5;
	private static final int MSG_ERROR_STATUS = 128; // 在wifi_socket_server.h里

	/**
	 * 写入数据库(从收到的包中解析出wifi_id,wifi_ipv4,wifi_ipv4_port) 写两张表 record 和
	 * heartnumber
	 */
	public void store_to_database(IoSession session, AcessPoint ap) {
		// 设置返回给插座的数据
		String[] recv = ap.getRecv();
		byte[] send = new byte[recv.length];
		for (int i = 0; i < send.length; i++) {
			send[i] = (byte) Integer.parseInt(recv[i], 16);
		}
		// 获取当前时间信息
		Calendar cal = Calendar.getInstance();
		String year = Helper.fill(Integer.toHexString(cal.get(Calendar.YEAR)),
				4, '0');
		send[6] = (byte) Integer.parseInt(year.substring(2, 4), 16);// 年份低字节
		send[7] = (byte) Integer.parseInt(year.substring(0, 2), 16);// 年份高字节
		send[8] = (byte) (cal.get(Calendar.MONTH) + 1);
		send[9] = (byte) cal.get(Calendar.DAY_OF_MONTH);
		send[10] = (byte) (cal.get(Calendar.DAY_OF_WEEK) - 1);
		send[11] = (byte) cal.get(Calendar.HOUR_OF_DAY);
		send[12] = (byte) cal.get(Calendar.MINUTE);
		send[13] = (byte) cal.get(Calendar.SECOND);

		Record record = new Record();
		record.setWifi_id(ap.getWifi_id());
		record.setWifi_ipv4(ap.getIp());
		record.setWifi_ipv4_port(ap.getPort());
		// step1:写heartnumber表
		service.writeToHeartNumber(ap.getWifi_id());
		// step2:写record表
		service.writeToRecord(record);
		// step3:生成响应，返回给ap
		if (!send(session, send)) {
			logger.debug("[Branch1]Failure!After saving info to DB,response to wifi failed!");
			return;
		}
		logger.info("[Branch1]Succeed!After saving info to DB,response to wifi succeed!");
	}

	/**
	 * 发往插座(从数据库中提取出wifi_ipv4,wifi_ipv4_port填充到数据包中)
	 */
	public void send_to_socket(IoSession session, AcessPoint ap) {

		byte[] tel_buf = new byte[37]; // 发送给手机的响应内容
		byte[] newbuf = Helper.String2Byte(ap.getRecv()); // 发送给wifi插座的内容

		tel_buf[0] = (byte) MSG_ERROR_STATUS;
		// 设置tel_buf中的macid
		for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
			tel_buf[i] = (byte) Integer.parseInt(ap.getRecv()[i], 16);
		}

		// step1:在record表中查询wifi_ipv4,wifi_ipv4_port
		Record record = DBDaoImpl.getInfoFromRecord(ap);
		if (!record.isRecorded()) {
			logger.info("[Branch4]Cann't find any record in Record!");
			// 如果不存在记录，向手机发送错误响应消息
			tel_buf[PARA_OFFSET] = (byte) NO_SOCKET_ADDR;
			if (!send(session, tel_buf)) {
				logger.warn("send to mobile error");
			}
			return;
		}
		logger.info("[Branch4]Succeed!Find wifi ip and port for mac_id:"
				+ ap.getWifi_id());
		// step2:根据查出ip 端口号，向其发送信息，测试是否在线
		// 将手机ip和端口号写入newbuf
		String mobileIpString = ((InetSocketAddress) session.getRemoteAddress())
				.getAddress().getHostAddress();

		String[] mobileIpStringArray = mobileIpString.split("\\.");
		logger.debug("telephone ip is:" + Arrays.toString(mobileIpStringArray));
		byte[] mobileIp = new byte[4];
		for (int i = 0; i < mobileIp.length; i++) {
			mobileIp[i] = (byte) Integer.parseInt(mobileIpStringArray[i]);
		}
		logger.debug("send_to_socket() mobileIp=" + Arrays.toString(mobileIp));
		for (int i = 8; i < 12; i++) {
			newbuf[i] = mobileIp[i - 8];
		}
		newbuf[13] = (byte) ((InetSocketAddress) session.getRemoteAddress())
				.getPort();
		newbuf[12] = (byte) (((InetSocketAddress) session.getRemoteAddress())
				.getPort() >> 8);

		if (newbuf[18] == 0x5f && newbuf[19] == 0x3f) {
			logger.info("this is what we send to socket");
			logger.info(newbuf);
		}

		// step3:向wifi插座转发信息
		logger.debug("newbuf is:" + Arrays.toString(newbuf));
		if (send(session, newbuf,
				new InetSocketAddress(Helper.longToIp(record.getWifi_ipv4()),
						record.getWifi_ipv4_port()))) {
			logger.info("[Branch4]Succeed!Send to wifi socket finished!");
		} else {
			logger.warn("[Branch4]Failure!Send to wifi socket failed!");
		}

		// step4:向手机发送正确响应信息
		tel_buf[PARA_OFFSET] = NO_ERROR;
		if (!send(session, tel_buf)) {
			logger.warn("send to mobile error\n");
		}
		logger.debug("Succeed!");
	}

	/**
	 * 将插座发来的信息发送手机(数据包不作处理直接发往手机)
	 */
	public void send_to_mobile(IoSession session, AcessPoint ap) {
		// 设置手机的Ip和port
		String[] recv = ap.getRecv();
		byte[] send = new byte[recv.length];
		for (int i = 0; i < recv.length; i++)
			send[i] = (byte) Integer.parseInt(recv[i], 16);
		// 设置ip
		StringBuffer sb = new StringBuffer();
		for (int i = 8; i < 12; i++) {
			sb.append(Integer.parseInt(recv[i], 16));
			if (i != 11)
				sb.append(".");
		}
		String ip = new String(sb);
		// 设置端口号
		int high = Integer.parseInt(recv[12], 16);
		int low = Integer.parseInt(recv[13], 16);
		int port = (high << 8) + low;
		logger.debug("Get info from wifi request data.Tel ip is：" + ip
				+ ",port is：" + port);
		InetSocketAddress addr = new InetSocketAddress(ip, port);
		if (!send(session, send, addr)) {
			logger.warn("Failure!Repost to mobile failed!");
			return;
		}
		logger.info("[Branch5]Succeed!Repost to mobile succeed!");

	}

	/**
	 * 检测本服务器是否在线
	 */
	public void detect_alive(IoSession session, AcessPoint ap) {
		byte[] send = new byte[20];
		for (int i = 0; i < send.length; i++)
			send[i] = 0;
		// 收到请求，添加时间信息，返回
		send[0]= (byte)0xe3;
		// 获取当前时间信息
		Calendar cal = Calendar.getInstance();
		String year = Helper.fill(Integer.toHexString(cal.get(Calendar.YEAR)),
				4, '0');
		send[6] = (byte) Integer.parseInt(year.substring(2, 4), 16);// 年份低字节
		send[7] = (byte) Integer.parseInt(year.substring(0, 2), 16);// 年份高字节
		send[8] = (byte) (cal.get(Calendar.MONTH) + 1);
		send[9] = (byte) cal.get(Calendar.DAY_OF_MONTH);
		send[10] = (byte) (cal.get(Calendar.DAY_OF_WEEK) - 1);
		send[11] = (byte) cal.get(Calendar.HOUR_OF_DAY);
		send[12] = (byte) cal.get(Calendar.MINUTE);
		send[13] = (byte) cal.get(Calendar.SECOND);
		
		System.out.println("99响应数据为："+Arrays.toString(Helper.bytesToHexString(send)));

		// 发送响应信息
		if (!send(session, send)) {
			logger.warn("[Branch2]Failure!Send detect response failed!");
			return;
		}
		logger.info("[Branch2]Succeed!Send detect response succeed!");
	}

	/**
	 * 接收来自外部设备的请求信息
	 * 
	 * @param session
	 *            与外部设备的会话
	 * @param ap
	 *            外部设备实体类
	 */
	public void outside_send_to_socket(IoSession session, AcessPoint ap) {
		// 计算发送给wifi插座的newbuf的长度
		Global_info host = new Global_info();
		host.setCmd((byte) (Integer.parseInt(ap.getRecv()[0], 16) - 100));
		host.setPass("".getBytes());
		int paramsLen = ap.getRecv().length - ACTION_OFFSET;
		int rawLen = paramsLen + 16;
		int cookLen = (((int) (rawLen / 16)) + ((rawLen % 16 == 0) ? 0 : 1)) * 16;
		int resultLen = 20 + cookLen;// 20是TSPackHeader的大小

		byte[] tel_buf = new byte[37];// 向手机返回的信息

		logger.info("the resultlen is:" + resultLen);

		tel_buf[0] = (byte) MSG_ERROR_STATUS;
		tel_buf[PARA_OFFSET] = NO_ERROR;

		StringBuffer sb = new StringBuffer();
		for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
			tel_buf[i] = (byte) Integer.parseInt(ap.getRecv()[i], 16);
			// newbuf[i] = (byte) Integer.parseInt(ap.getRecv()[i], 16);
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
			logger.warn("[Branch3]Cann't find any record in Record!");
			tel_buf[PARA_OFFSET] = NO_SOCKET_ADDR;
		}
		// step2:在comsocket表里查询是否存在此comid的记录
		boolean hasPermission = DBDaoImpl.hasItemInComsocket(mac_id, com_id);
		if (!hasPermission) {
			// 设置为未授权
			tel_buf[PARA_OFFSET] = NO_PERMISSION;
			logger.warn("[Branch3]Cann't find the permission in Comsocket!");
		}
		// 如果step1，step2 任一一步不存在，就向手机返回信息
		if (tel_buf[PARA_OFFSET] != NO_ERROR) {
			if (!send(session, tel_buf)) {
				logger.error("[Branch3]send to mobile error...");
			}
			logger.debug("Cann't find this comid or wifiid");
			return;
		}

		String outsideIp = Helper.longToIp(record.getWifi_ipv4());
		int outsidePort = record.getWifi_ipv4_port();
		logger.debug("outside socket ip：" + outsideIp + ",port:" + outsidePort);

		// 获得加密后的信息（此信息发送给第三方wifi插座）
		byte[] newbuf = getSendData(session, ap);
		// step3:向wifi发信息
		if (send(session, newbuf, new InetSocketAddress(outsideIp, outsidePort))) {
			logger.info("[Branch3]Succeed!Send to [outside wifi socket] finished!");
		} else {
			logger.warn("Failure!Send to [outside wifi socket] failed!");
		}
		if (true) {
			logger.debug("this is what we send to socket");
			logger.debug("newbuf is:" + Arrays.toString(newbuf));
		}
		// step3:向wifi发信息 FOR TEST
//		if (send(session, newbuf)) {
//			logger.debug("test:send 22222222222222222222222222222222222");
//		}

	}
	/**
	 * 接收98消息，查询n个插座是否在线（120s内有心跳）
	 * @param session
	 * @param ap
	 */
	public void detect_wifi_alive(IoSession session,AcessPoint ap){
		int amount = Integer.parseInt(ap.getRecv()[20],16);
		logger.debug("得到的插座数量是："+amount);
		List<byte[]> recvMac = new ArrayList<byte[]>();
		String[] macStrArray = new String[amount];//mac地址数组(String类型)
		int k = 21;//mac地址在recv[]数组中开始的索引位置
		for(int i=0;i<amount;i++){
			byte[] temp = new byte[6];
			StringBuffer sb = new StringBuffer();
			for(int j=0;j<6;j++,k++){
				temp[j] = (byte)(Integer.parseInt(ap.getRecv()[k],16));
				sb.append(ap.getRecv()[k]);
			}
			macStrArray[i] = new String(sb);
//			System.out.println("第"+(i+1)+"个mac String 是："+new String(sb));
			recvMac.add(temp);
		}
		byte[] retArray = new byte[amount];//
		for(int i=0;i<amount;i++){
			retArray[i] = service.detectWifi(macStrArray[i]);
		}
		// 发送报文数据长度=1(命令字)+19(无用19字节)+1(数量)+7*n
		int sendLen = 1 + 19 + 1 + 7*amount;
		byte[] toSend = new byte[sendLen];
		toSend[0]= (byte)0xe2;
		for(int i=1;i<20;i++)
			toSend[i] = 0;
		toSend[20] = (byte)amount;
		for(int i=0;i<amount;i++){
			setMacAndStatus(toSend, 21+i*7, recvMac.get(i), retArray[i]);
		}
		System.out.println("发送的数组是："+Arrays.toString(Helper.bytesToHexString(toSend)));
		if (!send(session, toSend)) {
			logger.warn("[Branch7]send to mobile error");
		}
		logger.info("[Branch7]Success!Detect wifi response successfully!");
	}

	/**
	 * 收到0x6e消息，改为0x02 其他流程跟send_to_socket一致
	 * 
	 * @param session
	 * @param ap
	 */
	public void repost_request(IoSession session, AcessPoint ap) {
		
		
		byte[] newbuf = Helper.String2Byte(ap.getRecv()); // 发送给wifi插座的内容
		
		newbuf[0] = 2; // 替换控制字信息为2（此为设计规定）

		// step1:在record表中查询wifi_ipv4,wifi_ipv4_port
		Record record = DBDaoImpl.getInfoFromRecord(ap);
		if (!record.isRecorded()) {
			logger.warn("[Branch6]read database error");
			return;
		}
		logger.info("[Branch6]Succeed!Find wifi ip and port for mac_id:"
				+ ap.getWifi_id());
		// step2:根据查出ip 端口号，向其发送信息，测试是否在线
		// 将手机ip和端口号写入newbuf
		String mobileIpString = ((InetSocketAddress) session.getRemoteAddress())
				.getAddress().getHostAddress();

		String[] mobileIpStringArray = mobileIpString.split("\\.");
		logger.debug("telephone ip is:" + Arrays.toString(mobileIpStringArray));
		byte[] mobileIp = new byte[4];
		for (int i = 0; i < mobileIp.length; i++) {
			mobileIp[i] = (byte) Integer.parseInt(mobileIpStringArray[i]);
		}
		logger.debug("send_to_socket() mobileIp=" + Arrays.toString(mobileIp));
		for (int i = 8; i < 12; i++) {
			newbuf[i] = mobileIp[i - 8];
		}
		newbuf[13] = (byte) ((InetSocketAddress) session.getRemoteAddress())
				.getPort();
		newbuf[12] = (byte) (((InetSocketAddress) session.getRemoteAddress())
				.getPort() >> 8);

		if (newbuf[18] == 0x5f && newbuf[19] == 0x3f) {
			logger.debug(newbuf);
			logger.debug("this is what we send to socket\r\n");
		}

		// step3:向wifi插座转发信息
		logger.debug("newbuf is:" + Arrays.toString(newbuf));
		if (send(session, newbuf,
				new InetSocketAddress(Helper.longToIp(record.getWifi_ipv4()),
						record.getWifi_ipv4_port()))) {
			logger.info("[Branch6]Succeed!Repost Send to wifi socket finished!");
		} else {
			logger.warn("[Branch6]Failure!Repost Send to wifi socket failed!");
		}
		logger.debug("Succeed!");
	}

	/**
	 * 向当前客户端发送信息
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

	/**
	 * 向会话之外的客户端发送信息
	 * 
	 * @param session
	 * @param data
	 * @param addr
	 * @return
	 */
	public boolean send(IoSession session, byte[] data, InetSocketAddress addr) {
		IoBuffer buffer = IoBuffer.wrap(data);
		WriteFuture future = session.write(buffer, addr);
		future.awaitUninterruptibly(100);
		return future.isWritten();
	}

	private byte[] getSendData(IoSession session, AcessPoint ap) {
		byte cmdId = (byte) (Integer.parseInt(ap.getRecv()[0], 16) - 100);
        TSPackHeader head = new TSPackHeader();
        // 控制类型[0]
        head.setPacketType(cmdId);
        // 加密类型[1]
        head.setEncodeType(TSPackHeader.ENCODE_TYPE_ENCRYPT_AES128);
        // int seed = new Random().nextInt(65535) + 1;
        // head.setSeed((short) seed);
        // 重发类型[2、3]
        head.setRepeat();
        // 时间戳[4-7] 全是0
        // mac[14-19]
        byte[] mac = new byte[TSPackHeader.MAC_LENGTH];
        for (int i = 0; i < TSPackHeader.MAC_LENGTH; i++) {
            mac[i] = (byte) Integer.parseInt(ap.getRecv()[i + MAC_OFFSET], 16);
        }
        head.setMacAddress(mac, 0);// 设置mac地址
        // //手机地址[8-13] 此时还都是0

        // 头信息设置完毕，获取头的md5
        byte[] headBytes = head.getBytes();
        logger.debug("【head is:" + Arrays.toString(Helper.bytesToHexString(headBytes)));
        byte[] md5 = null;
        try {
            md5 = AESUtilForDevice.getMD5(headBytes, 0, headBytes.length);
            logger.debug("【outside】Head MD5 is:" + Arrays.toString(Helper.bytesToHexString(md5)));
        } catch (Exception e) {
            // handle exception
        }
        if (null == md5) {
            return null;
        }
        // cmdLen
        //int cmdLen = 1;
        byte[] rawdata;
        if(ap.getRecv().length==37){
            byte[] cmdData = new byte[1];
            cmdData[0] = (byte) (Integer.parseInt(ap.getRecv()[36], 16));
            int rawLen = md5.length + 1;

            int paddingLen = ((rawLen + 15) / 16) * 16;
            rawdata = new byte[paddingLen];
            for (int i = 0; i < rawdata.length; i++) {
                rawdata[i] = 0;
            }
            System.arraycopy(md5, 0, rawdata, 0, md5.length);
            System.arraycopy(cmdData, 0, rawdata, md5.length, cmdData.length);
        }

        else if(ap.getRecv().length==36){
            int rawLen = md5.length;

            int paddingLen = ((rawLen + 15) / 16) * 16;
            rawdata = new byte[paddingLen];
            for (int i = 0; i < rawdata.length; i++) {
                rawdata[i] = 0;
            }
            System.arraycopy(md5, 0, rawdata, 0, md5.length);
         //   System.arraycopy(cmdData, 0, rawdata, md5.length, cmdData.length);
        }else{
            return null;
        }

//        if (cmdLen > 0) {
//
//            cmdData = new byte[cmdLen];// 除去包头的原始信息
//            cmdData
//            for (int i = 0; i < cmdLen; i++) {
//                cmdData[i] = (byte) (Integer.parseInt(ap.getRecv()[i], 16));
//            }
//        }

        // byte[] cmdData = { cmdId };
        // int cmdLen = 1;


        // 获得aes key
        byte[] aesKey = getAesKey(ap);
        if (null == aesKey)
            return null;

        // 加密
        byte[] encryptData = null;
        try {
            encryptData = AESUtilForDevice.encrypt(aesKey, rawdata, 0,
                    rawdata.length);
        } catch (Exception e) {

        }
        logger.debug("encryptData is:" + Arrays.toString(Helper.bytesToHexString(encryptData)));
        //
        // 手机地址[8-13]
        byte[] phoneIp = new byte[TSPackHeader.PORT_LEN];
        String[] ip = ((InetSocketAddress) session.getRemoteAddress())
                .getAddress().getHostAddress().split("\\.");
        int port = ((InetSocketAddress) session.getRemoteAddress()).getPort();
        for (int i = 0; i < ip.length; i++)
            phoneIp[i] = (byte) Integer.parseInt(ip[i]);
        phoneIp[5] = (byte) port;
        phoneIp[4] = (byte) (port >> 8);
        head.setPort(phoneIp, 0);
        headBytes = head.getBytes();
        byte[] result = new byte[headBytes.length + rawdata.length];
        System.arraycopy(headBytes, 0, result, 0, headBytes.length);
        System.arraycopy(encryptData, 0, result, headBytes.length,
                rawdata.length);
        logger.debug("【outside】result data length is:" + result.length);
        logger.debug("【outside】result data is:" + Arrays.toString(Helper.bytesToHexString(result)));
        return result;
	}

	private byte[] getAesKey(AcessPoint ap) {
		byte[] aesKey = null;
		byte[] pass = new byte[16];// 设备密码16字节
		if (ap.getRecv().length >= 36) { // 有16字节设备密码
			for (int i = PARA_OFFSET; i < PARA_OFFSET + 16; i++) {
				pass[i - PARA_OFFSET] = (byte) Integer.parseInt(
						ap.getRecv()[i], 16);
			}
			if (!Helper.isEmpty(pass, pass.length)) {
				try {
					aesKey = AESUtilForDevice.getMD5(pass, 0, pass.length);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			} else {
				aesKey = new byte[16];
				for (int i = 0; i < aesKey.length; i++)
					aesKey[i] = 0;
			}
		} else { // 无16字节设备密码
			aesKey = new byte[16];
			for (int i = 0; i < aesKey.length; i++)
				aesKey[i] = 0;
		}
		logger.debug("【outside】aesKey is:" + Arrays.toString(aesKey));
		return aesKey;
	}
	
	private void setMacAndStatus(byte[] input,int offset,byte[] mac,byte status){
		System.arraycopy(mac, 0, input, offset, 6);
		input[offset+6] = status;
	}

	public static void main(String[] args) {
	
	}
}
