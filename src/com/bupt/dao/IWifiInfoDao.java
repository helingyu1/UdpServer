package com.bupt.dao;

public interface IWifiInfoDao {

	/**
	 * �����ݿ������Ϣwifi��Ϣ
	 */
	public void saveInfoToRecord();
	
	/**
	 * 
	 */
	public void saveHeartInfoToDB();
	
	/**
	 * �����ݿ��в�ѯ��Ϣ
	 * 
	 */	
	public void getInfoFromRecord(String wifi_id);

}
