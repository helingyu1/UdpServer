package com.bupt.service;

import org.apache.log4j.Logger;

import com.bupt.dao.DBDaoImpl;
import com.bupt.entity.Record;

public class DaoService {
	Logger logger = Logger.getLogger(DaoService.class);

	/**
	 * д��record��
	 */
	public void writeToRecord(Record record) {
		//ʹ��replace into��������£�������ӣ�
		DBDaoImpl.replaceToRecord(record);
	}

	/**
	 * д��heartnumber��
	 */
	public void writeToHeartNumber(String wifi_id) {

		// step1:����heartdevice�в�ѯ�Ƿ���ڸ�id�ļ�¼
		// û�еĻ�����,���������step2
		if (!DBDaoImpl.hasItemInHeartdevice(wifi_id))
			return;

		// step2���ж�heartnumber�����Ƿ��и�id��¼
		// û�еĻ�����3���еĻ�4
		if (!DBDaoImpl.hasItemInHeartnumber(wifi_id)) {
			// step3:����һ���¼�¼(heartnum�ֶ���Ϊ1)
			DBDaoImpl.insertToHeartnumber(wifi_id);
		} else {
			// step4����heartnum�ֶ�ֵ+1
			DBDaoImpl.updateInHeartnumber(wifi_id);
		}
	}
	/**
	 * �жϸ�mac_id��120s���Ƿ�������
	 * @param mac_id
	 * @return
	 */
	public byte detectWifi(String mac_id){
		long currentTime = System.currentTimeMillis()/1000;
		long recordTime = DBDaoImpl.getTimeFromRecord(mac_id);
		long gap = currentTime - recordTime;
		logger.debug(mac_id+"�Ĳ��ʱ���ǣ�"+recordTime);
		logger.debug(mac_id+"������ʱ���ǣ�"+currentTime);
		logger.debug(mac_id+"��ʱ�����ǣ�"+gap);
		return gap<120?(byte)1:0;
	}
}
