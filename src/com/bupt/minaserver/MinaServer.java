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
	private static final String path = "config/Global.properties"; // 全局配置文件

	private static int port = 0; // mina 服务器监听端口号

	private static String jdbcDriver = ""; // 数据库驱动
	private static String dbUrl = ""; // 数据 URL
	private static String dbUsername = ""; // 数据库用户名
	private static String dbPassword = ""; // 数据库用户密码
	private static int maxConnections = 0;
	
	public static ConnectionPool pool;
	static {
		Properties prop = new Properties();
		FileInputStream fis;
		try {
			fis = new FileInputStream(new File(path));
			prop.load(fis);
			// 读取配置文件属性
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
	 * 初始化数据库
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
	 * 初始化mina服务器
	 * @param port 从本地配置文件读取的监听端口号
	 */
	public static void initMina(int port) {
		// ** Acceptor设置
		NioDatagramAcceptor acceptor = new NioDatagramAcceptor();
		// 此行代码能让你的程序整体性能提升10倍
		acceptor.getFilterChain().addLast("threadPool",
				new ExecutorFilter(Executors.newCachedThreadPool()));
		// 设置MINA2的IoHandler实现类
		acceptor.setHandler(new EchoSeverHandler());
		// 设置会话超时时间（单位：毫秒），不设置则默认是10秒，请按需设置
		acceptor.setSessionRecycler(new ExpiringSessionRecycler(60 * 1000));

		// ** UDP通信配置
		DatagramSessionConfig dcfg = acceptor.getSessionConfig();
		dcfg.setReuseAddress(true);
		// 设置输入缓冲区的大小，压力测试表明：调整到2048后性能反而降低
		dcfg.setReceiveBufferSize(1024);
		// 设置输出缓冲区的大小，压力测试表明：调整到2048后性能反而降低
		dcfg.setSendBufferSize(1024);

		dcfg.setUseReadOperation(true);
		acceptor.setDefaultLocalAddress(new InetSocketAddress(port));
		try {
			acceptor.bind();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// ** UDP服务端开始侦听
		logger.info("Mina server begin to listen on port " + port+"...");
	}

}
