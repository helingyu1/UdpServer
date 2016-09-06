package com.bupt.minaserver;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.mina.core.session.ExpiringSessionRecycler;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;

public class MinaServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// ** Acceptor����
		NioDatagramAcceptor acceptor = new NioDatagramAcceptor();
		// ���д���������ĳ���������������10��
		acceptor.getFilterChain().addLast("threadPool",
				new ExecutorFilter(Executors.newCachedThreadPool()));
		// ����MINA2��IoHandlerʵ����
		acceptor.setHandler(new EchoSeverHandler());
		// ���ûỰ��ʱʱ�䣨��λ�����룩����������Ĭ����10�룬�밴������
		acceptor.setSessionRecycler(new ExpiringSessionRecycler(60 * 1000));

		// ** UDPͨ������
		DatagramSessionConfig dcfg = acceptor.getSessionConfig();
		dcfg.setReuseAddress(true);
		// �������뻺�����Ĵ�С��ѹ�����Ա�����������2048�����ܷ�������
		dcfg.setReceiveBufferSize(1024);
		// ��������������Ĵ�С��ѹ�����Ա�����������2048�����ܷ�������
		dcfg.setSendBufferSize(1024);
		
		dcfg.setUseReadOperation(true);

		// ** UDP����˿�ʼ����
		acceptor.bind(new InetSocketAddress(9999));
		System.out.println("��ʼ����");
	}

}
