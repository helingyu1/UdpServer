package com.bupt.dao;

public interface IWifiInfoDao {

	/**
	 * �����ݿ������Ϣwifi��Ϣ
	 */
	public void saveWifiInfoToDB();
	
	/**
	 * 
	 */
	public void saveHeartInfoToDB();
	
	/**
	 * �����ݿ��в�ѯ��Ϣ
	 * 
	 */	
	public void getInfoInDB();

}
