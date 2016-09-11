package com.bupt.minaserver;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.bupt.entity.AcessPoint;
import com.bupt.service.RequestService;
import com.bupt.utils.Helper;

/**
 * 该类接收并处理不同类型的消息
 * 发送接收用byte[],转成char[]来操作
 * @author helingyu
 *
 */
public class EchoSeverHandler extends IoHandlerAdapter {
	static int sum = 0;
	
	private final Logger logger = Logger.getLogger(EchoSeverHandler.class);
	// service
	RequestService service = new RequestService();

	// 数据偏移量
	private static final int MAC_OFFSET = 14;
	private static final int ACTION_OFFSET = 36;// outside方法里用
	private static final int PARA_OFFSET = 20;

	// 标志
	private static final int NO_SOCKET_ADDR = 4;
	private static final int NO_ERROR = 0;

	public static final CharsetDecoder decoder = (Charset.forName("UTF-8"))
			.newDecoder();

	/**
	 * MINA的异常回调方法。
	 * <p>
	 * 本类中将在异常发生时，立即close当前会话。
	 * 
	 * @param session
	 *            发生异常的会话
	 * @param cause
	 *            异常内容
	 * @see IoSession#close(boolean)
	 */
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		// logger.error("[IMCORE]exceptionCaught捕获到错了，原因是：" +
		// cause.getMessage(),
		// cause);
		session.close(true);
	}

	/**
	 * MINA框架中收到客户端消息的回调方法。
	 * <p>
	 * 本类将在此方法中实现完整的即时通讯数据交互和处理策略。
	 * <p>
	 * 为了提升并发性能，本方法将运行在独立于MINA的IoProcessor之外的线程池中， 详见
	 * {@link ServerLauncher#initAcceptor()}中的MINA设置代码 。
	 * 
	 * @param session
	 *            收到消息对应的会话引用
	 * @param message
	 *            收到的MINA的原始消息封装对象，本类中是 {@link IoBuffer}对象
	 * @throws Exception
	 *             当有错误发生时将抛出异常
	 * 
	 *             ps:接收来自wifi socket或者mobile的消息
	 */
	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		// *********************************************** 接收数据
		// step1:读取收到的数据
		IoBuffer buffer = (IoBuffer) message;
		String hex = buffer.getHexDump();
		System.out.println("hex:"+hex);
		String[] recv = hex.split(" ");
		
		// 转成字符数组
		logger.debug("服务器接收到的数据："+Arrays.toString(recv));

		// 得到wifi_id
		StringBuffer sb = new StringBuffer();
		for (int i = MAC_OFFSET; i < MAC_OFFSET + 6; i++) {
			sb.append(recv[i]);
		}
		String mac_id = new String(sb);
		// 设置此次请求的发起的实体
		InetSocketAddress addr = (InetSocketAddress) session.getRemoteAddress();
		String ipStr = addr.getAddress().getHostAddress();
		long ip = Helper.ipToLong(ipStr);
		AcessPoint ap = new AcessPoint(ip,
				addr.getPort(), mac_id,recv);

		// step2:解析数据
		int swt = Integer.parseInt(recv[0],16);
		logger.debug("swt:"+swt);
		if (swt == 0) { // 功能1：写插座信息到数据库
			logger.debug("test:进入分支【1】,写插座信息到数据库");
			service.store_to_database(session,ap);
		} else if (swt == 99) { // 功能2：检测服务器是否在线
			logger.debug("test:进入分支【2】,检测服务器是否在线");
			service.detect_alive(session,ap);
		} else if (swt > 100 && swt < 128) { // 功能3：第三方发送控制命令到服务器
			logger.debug("test:进入分支【3】,第三方发送控制命令到服务器");
			service.outside_send_to_socket(session,ap);
		} else if (swt >= 1 && swt < 128) { // 功能4：查看多个插座是否在线
			logger.debug("test:进入分支【4】,非第三方发送请求");
			service.send_to_socket(session,ap);
		} else if (swt >= 128) { // 功能5：数据包不做处理直接发给手机
			logger.debug("test:进入分支【5】，数据包不做处理直接发给手机");
			service.send_to_mobile(session,ap);
		}
	}
	

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		super.sessionCreated(session);
//		System.out.println(((InetSocketAddress)session.getRemoteAddress()).getHostName());
//		session.write("aaaa");
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		// TODO Auto-generated method stub
		super.sessionOpened(session);
	}

	public static void main(String[] args) {

//		String hex = "1111";
//		String[] arr = Helper.hexToStringArray(hex);
//		System.out.println(arr.length);
//		for (int i = 0; i < arr.length; i++)
//			System.out.println(arr[i]);
//		char[] aa = {0x63,0x63,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0x63};
//		String b = new String(aa);
//		System.out.println(b);
		System.out.println(Integer.parseInt("ff", 16));


	}
}
