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
import com.bupt.entity.Global_info;
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
	private MD5Service md5Service = new MD5Service();
	private AESService aesService = new AESService();

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
			logger.debug("Failure!After saving info to DB,response to wifi failed!");
			return;
		}
		logger.debug("Succeed!After saving info to DB,response to wifi succeed!");
	}

	/**
	 * 发往插座(从数据库中提取出wifi_ipv4,wifi_ipv4_port填充到数据包中)
	 */
	public void send_to_socket(IoSession session, AcessPoint ap) {

		byte[] tel_buf = new byte[37]; // 发送给手机的响应内容
		byte[] newbuf = RequestService.String2Byte(ap.getRecv()); // 发送给wifi插座的内容

		tel_buf[0] = (byte) MSG_ERROR_STATUS;
		// 设置tel_buf中的macid
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
		logger.info("Succeed!Find wifi ip and port for mac_id:"
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
		logger.info("send_to_socket() mobileIp=" + Arrays.toString(mobileIp));
		for (int i = 8; i < 12; i++) {
			newbuf[i] = mobileIp[i - 8];
		}
		newbuf[13] = (byte) ((InetSocketAddress) session.getRemoteAddress())
				.getPort();
		newbuf[12] = (byte) (((InetSocketAddress) session.getRemoteAddress())
				.getPort() >> 8);

		if (newbuf[18] == 0x5f && newbuf[19] == 0x3f) {
			logger.info(newbuf);
			logger.info("this is what we send to socket\r\n");
		}

		// step3:向wifi插座转发信息
		logger.debug("newbuf is:" + Arrays.toString(newbuf));
		if (send(session, newbuf,
				new InetSocketAddress(Helper.longToIp(record.getWifi_ipv4()),
						record.getWifi_ipv4_port()))) {
			logger.debug("Succeed!Send to wifi socket finished!");
		} else {
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
		logger.info("Succeed!Repost to mobile succeed!");

	}

	/**
	 * 检测本服务器是否在线
	 */
	public void detect_alive(IoSession session, AcessPoint ap) {
		byte[] send = new byte[20];
		for (int i = 0; i < send.length; i++)
			send[i] = 0;
		// 收到请求，添加时间信息，返回
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

		// 发送响应信息
		if (!send(session, send)) {
			logger.warn("Failure!Send detect response failed!");
			return;
		}
		logger.info("Succeed!Send detect response succeed!");
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
		byte[] newbuf = new byte[resultLen];// 向wifi发送的信息

		logger.info("the resultlen is:" + resultLen);

		tel_buf[0] = (byte) MSG_ERROR_STATUS;
		tel_buf[PARA_OFFSET] = NO_ERROR;

		StringBuffer sb = new StringBuffer();
		for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
			tel_buf[i] = (byte) Integer.parseInt(ap.getRecv()[i], 16);
			newbuf[i] = (byte) Integer.parseInt(ap.getRecv()[i], 16);
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
			// 设置为未授权
			tel_buf[PARA_OFFSET] = NO_PERMISSION;
		}
		// 如果step1，step2 任一一步不存在，就向手机返回信息
		if (tel_buf[PARA_OFFSET] != NO_ERROR) {
			if (!send(session, tel_buf)) {
				logger.warn("send to mobile error\n");
			}
			return;
		}
		// 加密
		newbuf = generate_AES_MD5(host,
				(byte) Integer.parseInt(ap.getRecv()[ACTION_OFFSET], 16), 1,
				newbuf, session);

		String outsideIp = Helper.longToIp(record.getWifi_ipv4());
		int outsidePort = record.getWifi_ipv4_port();
		logger.debug("outside socket ip：" + outsideIp + ",port:" + outsidePort);

		// step3:向wifi发信息
		if (send(session, newbuf, new InetSocketAddress(outsideIp, outsidePort))) {
			logger.debug("Succeed!Send to [outside wifi socket] finished!");
		} else {
			logger.warn("Failure!Send to [outside wifi socket] failed!");
		}
		// String[] mobileIpString = ((InetSocketAddress) session
		// .getRemoteAddress()).getAddress().getHostAddress().split("\\.");
		// byte[] mobileIp = new byte[4];
		// for (int i = 0; i < mobileIp.length; i++) {
		// mobileIp[i] = (byte) Integer.parseInt(mobileIpString[i]);
		// }
		// logger.info("outside_send_to_socket() mobileIp=");
		// logger.info(Arrays.toString(mobileIp));
		// for (int i = 8; i < 12; i++) {
		// newbuf[i] = mobileIp[i - 8];
		// }
		// newbuf[13] = (byte) ((InetSocketAddress) session.getRemoteAddress())
		// .getPort();
		// newbuf[12] = (byte) (((InetSocketAddress) session.getRemoteAddress())
		// .getPort() >> 8);

		if (true) {
			logger.info("newbuf is:" + Arrays.toString(newbuf));
			logger.info("this is what we send to socket");
		}
		// // step3:向wifi发信息
		// if(send(session,newbuf)){
		// logger.debug("test:send 22222222222222222222222222222222222");
		// }

		// step4:向手机发信息
		tel_buf[PARA_OFFSET] = NO_ERROR;
		if (!send(session, tel_buf)) {
			logger.warn("send to mobile error\n");
		}

	}

	/**
	 * 转发信息
	 * 
	 * @param ap
	 */
	public void repost_request(AcessPoint ap) {

	}

	/**
	 * 
	 * @param host
	 *            cmd+pass
	 * @param params
	 *            参数
	 * @param paramsLen
	 *            1
	 * @param newBuff
	 *            响应数组
	 * @return result 新响应数组
	 */
	public byte[] generate_AES_MD5(Global_info host, byte params,
			int paramsLen, byte[] newBuff, IoSession session) {
		byte[] result = newBuff;
		// 构建发送数据包
		int rawLen = paramsLen + 16;

		byte packetType = host.getCmd();// 控制类型
		byte encodeType = 1;// 加密类型
		byte[] repeat = new byte[2];
		repeat[0] = 1;
		repeat[1] = 0;// 重发编号
		byte[] time = new byte[4];// 时间戳
		// byte[] phoneIpPort = new byte[6];// 手机ip port
		// byte[] socketMac = new byte[6];// 插座mac
		byte[] param = new byte[1];// 参数
		param[0] = params;

		result[0] = packetType;
		result[1] = encodeType;
		result[2] = repeat[0];
		result[3] = repeat[1];
		result[4] = time[0];
		result[5] = time[1];
		result[6] = time[2];
		result[7] = time[3];
		// 设置手机ip
		String[] mobileIpString = ((InetSocketAddress) session
				.getRemoteAddress()).getAddress().getHostAddress().split("\\.");
		byte[] mobileIp = new byte[4];
		for (int i = 0; i < mobileIp.length; i++) {
			mobileIp[i] = (byte) Integer.parseInt(mobileIpString[i]);
		}
		result[8] = mobileIp[0];
		result[9] = mobileIp[1];
		result[10] = mobileIp[2];
		result[11] = mobileIp[3];
		// 设置手机端口
		result[13] = (byte) ((InetSocketAddress) session.getRemoteAddress())
				.getPort();
		result[12] = (byte) (((InetSocketAddress) session.getRemoteAddress())
				.getPort() >> 8);
		// 设置mac地址（已在进方法前设置）
		// result[14] = socketMac[0];
		// result[15] = socketMac[1];
		// result[16] = socketMac[2];
		// result[17] = socketMac[3];
		// result[18] = socketMac[4];
		// result[19] = socketMac[5];

		result[20] = param[0];

		// 构建参数
		// 构建加密密钥
		byte[] pass = new byte[16];
		for (int i = 0; i < 16; i++)
			pass[i] = 0;
		if (host.getPass() != null && host.getPass().length != 0
				&& host.getPass()[0] != 0) {
			pass = md5Service.encode_md5(host.getPass(), host.getPass().length);// 这个就是密钥
			logger.debug("加密密钥是：" + Arrays.toString(pass));
		}
		// 取头的md5
		byte[] rawData = new byte[rawLen];// 这个地方大小是17
		byte[] headerMD5 = md5Service.encode_md5(result, 20);// 这个是需要加密的数据(20是头的长度),md5后是16字节
		System.arraycopy(headerMD5, 0, rawData, 0, 16);
		rawData[16] = params;

		// 进行aes 加密
		byte[] encodedData = aesService.encode_aes128(pass, rawData, 0, 17);
		System.arraycopy(encodedData, 0, result, 20, 17);

		return result;
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

	public static byte[] String2Byte(String[] s) {
		byte[] res = new byte[s.length];
		for (int i = 0; i < s.length; i++) {
			res[i] = (byte) Integer.parseInt(s[i], 16);
		}
		return res;
	}

	public static void main(String[] args) {
		String a = "";
		byte[] b = a.getBytes();
		if (b != null)
			System.out.println(1);
		if (b[0] != 0)
			System.out.println(2);
	}
}
