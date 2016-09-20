package com.bupt.minaserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.mina.core.session.ExpiringSessionRecycler;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;

import com.bupt.connection.ConnectionPool;

public class MinaServer {

	private static final Logger logger = Logger.getLogger(MinaServer.class);
	private static final String path = "config/Global.properties"; // ȫ�������ļ�

	private static int port = 0; // mina �����������˿ں�

	private static String jdbcDriver = ""; // ���ݿ�����
	private static String dbUrl = ""; // ���� URL
	private static String dbUsername = ""; // ���ݿ��û���
	private static String dbPassword = ""; // ���ݿ��û�����
	private static int maxConnections = 0;
	
	public static ConnectionPool pool;
	static {
		Properties prop = new Properties();
		FileInputStream fis;
		try {
			fis = new FileInputStream(new File(path));
			prop.load(fis);
			// ��ȡ�����ļ�����
			port = Integer.parseInt((String) prop.get("port"));
			jdbcDriver = (String) prop.get("jdbcDriver");
			dbUrl = (String) prop.get("dbUrl");
			dbUsername = (String) prop.get("username");
			dbPassword = (String) prop.get("password");
			maxConnections = Integer.parseInt((String)prop.get("maxConnections"));
			logger.info("Config file read finished...");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		initDB();
		initMina(port);
	}

	/**
	 * ��ʼ�����ݿ�
	 */
	public static void initDB() {
		
		ConnectionPool.jdbcDriver = jdbcDriver;
		ConnectionPool.dbUrl = dbUrl;
		ConnectionPool.dbUsername = dbUsername;
		ConnectionPool.dbPassword = dbPassword;
		ConnectionPool.maxConnections = maxConnections;
		
		pool = new ConnectionPool();
		
		try {
			pool.createPool();
		} catch (Exception e) {
			logger.error("DB connection pool created failed!", e);
		}
		logger.info("DB connection pool created successfully...");

	}

	/**
	 * ��ʼ��mina������
	 * @param port �ӱ��������ļ���ȡ�ļ����˿ں�
	 */
	public static void initMina(int port) {
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
		acceptor.setDefaultLocalAddress(new InetSocketAddress(port));
		try {
			acceptor.bind();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// ** UDP����˿�ʼ����
		logger.info("Mina server begin to listen on port " + port+"...");
	}

}
