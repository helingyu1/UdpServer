package com.bupt.entity;


public class TSPackHeader {
	// this had been defined in c code
	// typedef struct {
	// uint8_t packetsType; //鎶ユ枃绫诲瀷
	// uint8_t encodeType; // 1-鍔犲瘑锛�0-涓嶅姞瀵�
	// uint16_t repeat; //閲嶅彂缂栧彿
	// uint32_t time; //鏃堕棿鎴�
	// uint8_t phoneIpPort[6];//鎵嬫満IP鍦板潃绔彛鍙�
	// uint8_t socketMac[6]; //鎻掑骇MAC
	// uint8_t params[0]; //鍙傛暟
	// } TSPackHeader; //鍗忚澶达紝涓嶇鏄姹傝繕鏄繑鍥為兘瑕佸寘鍚�
	public static final int SIZE = 20;
	public static final byte ENCODE_TYPE_ENCRYPT_NONE = 0;
	public static final byte ENCODE_TYPE_ENCRYPT_AES128 = 1;
	public static final byte TYPE_START = 0;
	public static final byte PORT_START = 8;
	public static final byte PORT_LEN = 6;
	public static final byte MAC_START = 14;
	public static final byte MAC_LENGTH = 6;

	protected byte[] mData;

	public TSPackHeader() {
		mData = new byte[SIZE];
		for (byte i = 0; i < mData.length; i++) {
			mData[i] = 0;
		}
	}

	public byte[] getBytes() {
		return mData;
	}

	public boolean setData(byte[] data, int len) {
		if (null == data) {
//			Log.e(TAG, "setData: data=NULL");
			return false;
		}

		if ((data.length < SIZE) || (len < SIZE)) {
//			Log.e(TAG, "setData: data invalid, data.length=" + data.length
//					+ ", len=" + len);
			return false;
		}

		System.arraycopy(data, 0, mData, 0, mData.length);
		return true;
	}

	public void setPacketType(byte packetType) {
		mData[0] = packetType;
	}

	public int getPacketType() {
		return 0xFF & mData[0];
	}

	public void setEncodeType(byte encodeType) {
		mData[1] = encodeType;
	}

	public void setSeed(short seed) {
		mData[2] = (byte) seed;
		mData[3] = (byte) (seed >> 8);
	}
	
	public void setRepeat(){
		mData[2] = (byte)1;
		mData[3] = (byte)0;
	}

	public void setTimeStamp(int timeStamp) {
		mData[4] = (byte) timeStamp;
		mData[5] = (byte) (timeStamp >> 8);
		mData[6] = (byte) (timeStamp >> 16);
		mData[7] = (byte) (timeStamp >> 24);
	}

	public void setPort(byte[] port, int offset) {
		if (null == port) {
//			Log.e(TAG, "setPort: port=null");
			return;
		}
		if (port.length < offset + PORT_LEN) {
//			Log.e(TAG, "setPort: data invalid, length=" + port.length
//					+ ", offset=" + offset);
			return;
		}
		System.arraycopy(port, offset, mData, PORT_START, PORT_LEN);
	}

	public void setMacAddress(byte[] mac, int offset) {
		if (null == mac) {
//			Log.e(TAG, "setMacAddress: mac=null");
			return;
		}
		if (mac.length < offset + MAC_LENGTH) {
//			Log.e(TAG, "setMacAddress: data invalid, length=" + mac.length
//					+ ", offset=" + offset);
			return;
		}
		System.arraycopy(mac, offset, mData, MAC_START, MAC_LENGTH);
	}
}
