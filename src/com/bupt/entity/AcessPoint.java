package com.bupt.entity;

import java.util.Arrays;

/**
 * ��ʵ���࣬�����������ͨ�ŵĽڵ�
 * @author helingyu
 *
 */

public class AcessPoint {
	private String ip;
	private int port;
	private String wifi_id;
	private String[] recv;
	public AcessPoint(String ip,int port,String wifi_id,String[] recv){
		this.ip = ip;
		this.port = port;
		this.wifi_id = wifi_id;
		this.recv = recv;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String[] getRecv() {
		return recv;
	}
	public void setRecv(String[] recv) {
		this.recv = recv;
	}
	
	public String getWifi_id() {
		return wifi_id;
	}
	public void setWifi_id(String wifi_id) {
		this.wifi_id = wifi_id;
	}
	@Override
	public String toString() {
		return "AcessPoint [ip=" + ip + ", port=" + port + ", wifi_id="
				+ wifi_id + ", recv=" + Arrays.toString(recv) + "]";
	}
	
	

}
