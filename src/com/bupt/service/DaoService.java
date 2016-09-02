package com.bupt.service;

import com.bupt.dao.DBDaoImpl;
import com.bupt.entity.Record;

public class DaoService {

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
}
