package com.bupt.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import org.apache.log4j.Logger;

import com.bupt.connection.ConnectionPool;
import com.bupt.entity.AcessPoint;
import com.bupt.entity.Record;
import com.bupt.minaserver.MinaServer;
import com.bupt.service.ExceptionService;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;

public class DBDaoImpl {
	
	private static final Logger logger = Logger.getLogger(DBDaoImpl.class);

	private static ConnectionPool connPool;

	static {
		connPool = MinaServer.pool;
		try {
			// 创建数据库连接池
			connPool.createPool();
		} catch (Exception e) {
			e.printStackTrace();
			ExceptionService.handleException(e, logger);
		}
	}

	public static void replaceToRecord(Record record) {
		String sql = "replace into record(wifi_ipv4,wifi_ipv4_port,time,wifi_id) values(?,?,?,?)";
		Connection conn = null;
		PreparedStatement ps=null;
		try {
			conn = (Connection) connPool.getConnection();
			ps = (PreparedStatement) conn.prepareStatement(sql);
			ps.setLong(1, record.getWifi_ipv4());
			ps.setInt(2, record.getWifi_ipv4_port());
			ps.setLong(3, System.currentTimeMillis()/1000);
			ps.setString(4, record.getWifi_id());
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
			ExceptionService.handleException(e, logger);
		}finally{
			if(ps!=null)
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			connPool.returnConnection(conn);
		}
		
	}

	public static Record getInfoFromRecord(AcessPoint ap) {
		Record record = new Record();
		record.setWifi_id(ap.getWifi_id());
		logger.debug("wifi_id="+ap.getWifi_id());
		Connection conn = null;
		PreparedStatement ps =null;
		ResultSet rs = null;
		try {
			String sql = "select wifi_ipv4,wifi_ipv4_port from record where wifi_id='"
					+ ap.getWifi_id()+"'";
			conn = (Connection) connPool.getConnection();
			ps = (PreparedStatement) conn
					.prepareStatement(sql);

			rs = ps.executeQuery(sql);

			if(rs.next())
			{
				
				//如果存在记录
				record.setRecorded(true);
				record.setWifi_ipv4(rs.getLong("wifi_ipv4"));
				record.setWifi_ipv4_port(rs.getInt("wifi_ipv4_port"));
				long wifi_ipv4 = rs.getLong("wifi_ipv4");
				long wifi_ipv4_port = rs.getLong("wifi_ipv4_port");
				System.out.println(wifi_ipv4);
				System.out.println(wifi_ipv4_port);
			}else{
				record.setRecorded(false);
			}

		} catch (SQLException e) {
			e.printStackTrace();
			ExceptionService.handleException(e, logger);
		}finally{
			if(rs!=null)
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if(ps!=null)
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			connPool.returnConnection(conn);
		}
		return record;
	}

	/**
	 * 向heartnumber表中插入新数据
	 * 
	 * @param wifi_id
	 */
	public static void insertToHeartnumber(String wifi_id) {
		String time_day = getCurrentDateString();
		String sql = "insert into heartnumber(wifi_id,time_day,heart_num) values('"
				+ wifi_id + "','" + time_day + "',1)";
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = (Connection) connPool.getConnection();
			ps = (PreparedStatement) conn
					.prepareStatement(sql);
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
			ExceptionService.handleException(e, logger);
		}finally{
			if(ps!=null)
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			connPool.returnConnection(conn);
		}
	}

	/**
	 * 查询该wifi_id是否存在于 heartdevice表中
	 * 
	 * @param wifi_id
	 * @return
	 */
	public static boolean hasItemInHeartdevice(String wifi_id) {
		boolean ret = false;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			conn = (Connection) connPool.getConnection();
			String sql = "select * from heartdevice where wifi_id='" + wifi_id
					+ "'";
			ps = (PreparedStatement) conn
					.prepareStatement(sql);
			rs = ps.executeQuery();
			if (rs.next())
				ret = true;

		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(rs!=null)
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if(ps!=null)
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			connPool.returnConnection(conn);
		}
		return ret;
	}

	/**
	 * 查询 heartnumber表中是否含有该id的记录
	 * 
	 * @param wifi_id
	 * @return
	 */
	public static boolean hasItemInHeartnumber(String wifi_id) {
		boolean ret = false;
		String time_day = getCurrentDateString();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			conn = (Connection) connPool.getConnection();
			String sql = "select * from heartnumber where wifi_id='" + wifi_id
					+ "' and time_day='" + time_day + "'";
			ps = (PreparedStatement) conn
					.prepareStatement(sql);
			rs = ps.executeQuery();
			if (rs.next())
				ret = true;

		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(rs!=null)
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if(ps!=null)
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			connPool.returnConnection(conn);
		}
		return ret;
	}

	/**
	 * 更新 heartnumber 表中心跳记录
	 * 
	 * @param wifi_id
	 */
	public static void updateInHeartnumber(String wifi_id) {
		String time_day = getCurrentDateString();
		String sql = "update heartnumber set heart_num=heart_num+1 where wifi_id='"
				+ wifi_id + "' and time_day='" + time_day + "'";
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = (Connection) connPool.getConnection();
			ps = (PreparedStatement) conn
					.prepareStatement(sql);
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(ps!=null)
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			connPool.returnConnection(conn);
		}
	}
	
	/**
	 * 查询在comsocket中是否存在记录
	 * @param wifi_id
	 * @param com_id
	 * @return
	 */
	public static boolean hasItemInComsocket(String wifi_id,String com_id){
		boolean ret = false;
		String sql = "select com_id from comsocket where wifi_id=? and com_id=?";
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = (Connection) connPool.getConnection();
			ps = (PreparedStatement) conn.prepareStatement(sql);
			ps.setString(1, wifi_id);
			ps.setString(2, com_id);
			rs = (ResultSet)ps.executeQuery();
			if(rs.next())
				ret = true;
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(rs!=null)
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if(ps!=null)
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			connPool.returnConnection(conn);
		}
		return ret;
	}
	public static long getTimeFromRecord(String mac_id){
		long time = 0L;
		String sql = "select time from record where wifi_id=?";
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = (Connection) connPool.getConnection();
			ps = (PreparedStatement) conn.prepareStatement(sql);
			ps.setString(1, mac_id);
			rs = (ResultSet)ps.executeQuery();
			if(rs.next())
				time = rs.getLong("time");
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(rs!=null)
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if(ps!=null)
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			connPool.returnConnection(conn);
		}
		return time;
	}

	public static String getCurrentDateString() {
		Calendar cal = Calendar.getInstance();
		String year = cal.get(Calendar.YEAR) + "";
		String month = cal.get(Calendar.MONTH) + 1 + "";
		String day = cal.get(Calendar.DATE) + "";
		return year + month + day;
	}

	// test
	public static void main(String[] orgs) throws SQLException {
		// Connection conn = (Connection) connPool.getConnection();
		// String sql = "select * from user_info";
		// PreparedStatement ps = (PreparedStatement)
		// conn.prepareStatement(sql);
		// ResultSet rs = ps.executeQuery();
		// System.out.println(rs.wasNull());
		// int i = 0;
		// while (rs.next())
		// i++;
		// System.out.println(i);
		// getInfoFromRecord("getInfoFromRecord");
		// Date date = new Date();
		// updateInHeartnumber("aabbccddeeff");
//		System.out.println(System.currentTimeMillis() / 1000);
//		Record record = new Record();
//		record.setWifi_id("aabbccddeeff");
//		record.setWifi_ipv4(192);
//		record.setWifi_ipv4_port(8080);	
//		replaceToRecord(record);
		System.out.println(Integer.toHexString(2016));
	}

}
