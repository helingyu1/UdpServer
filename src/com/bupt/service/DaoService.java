package com.bupt.service;

import org.apache.log4j.Logger;

import com.bupt.dao.DBDaoImpl;
import com.bupt.entity.Record;

public class DaoService {
	Logger logger = Logger.getLogger(DaoService.class);

	/**
	 * 写入record表
	 */
	public void writeToRecord(Record record) {
		//使用replace into（有则更新，无则添加）
		DBDaoImpl.replaceToRecord(record);
	}

	/**
	 * 写入heartnumber表
	 */
	public void writeToHeartNumber(String wifi_id) {

		// step1:现在heartdevice中查询是否存在该id的记录
		// 没有的话返回,如果有跳到step2
		if (!DBDaoImpl.hasItemInHeartdevice(wifi_id))
			return;

		// step2：判断heartnumber表中是否有该id记录
		// 没有的话跳到3，有的话4
		if (!DBDaoImpl.hasItemInHeartnumber(wifi_id)) {
			// step3:插入一条新记录(heartnum字段设为1)
			DBDaoImpl.insertToHeartnumber(wifi_id);
		} else {
			// step4：将heartnum字段值+1
			DBDaoImpl.updateInHeartnumber(wifi_id);
		}
	}
	/**
	 * 判断该mac_id在120s内是否有心跳
	 * @param mac_id
	 * @return
	 */
	public byte detectWifi(String mac_id){
		long currentTime = System.currentTimeMillis()/1000;
		long recordTime = DBDaoImpl.getTimeFromRecord(mac_id);
		long gap = currentTime - recordTime;
		logger.debug(mac_id+"的查出时间是："+recordTime);
		logger.debug(mac_id+"的现在时间是："+currentTime);
		logger.debug(mac_id+"的时间间隔是："+gap);
		return gap<120?(byte)1:0;
	}
}
