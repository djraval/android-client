package com.bsb.hike.voip;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class VoIPUtils {

	public static enum ConnectionClass {
		TwoG,
		ThreeG,
		FourG,
		WiFi,
		Unknown
	}
	
    public static boolean isWifiConnected(Context context) {
    	ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    	if (mWifi.isConnected()) {
    		return true;
    	}    	
    	else
    		return false;
    }	
	
    public static String getLocalIpAddress(Context c) {
    	
    	if (isWifiConnected(c)) {
    		WifiManager wifiMgr = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
    		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
    		int ipAddress = wifiInfo.getIpAddress();
    		
    	    // Convert little-endian to big-endianif needed
    	    if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
    	        ipAddress = Integer.reverseBytes(ipAddress);
    	    }

    	    byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

    	    String ipAddressString;
    	    try {
    	        ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
    	    } catch (UnknownHostException ex) {
    	        Logger.e(VoIPConstants.TAG, "Unable to get host address.");
    	        ipAddressString = null;
    	    }

    	    return ipAddressString;    		
    	} else {
	        try {
	            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	                NetworkInterface intf = en.nextElement();
	                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                    InetAddress inetAddress = enumIpAddr.nextElement();
	                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
	                        return inetAddress.getHostAddress();
	                    }
	                }
	            }
	        } catch (NullPointerException ex) {
	            ex.printStackTrace();
	        } catch (SocketException ex) {
	            ex.printStackTrace();
	        }
        return null;
    	}
    }	

    /**
     * Used to communicate between two VoIP clients, via the server
     * @param msisdn The client to which the message is being sent.
     * @param type Message type (v0, v1 etc). 
     * @param subtype Message sub-type. This usually decides what the recipient does with 
     * the message.
     * @throws JSONException
     */
    public static void sendMessage(String msisdn, String type, String subtype) throws JSONException {
    	
		JSONObject data = new JSONObject();
		data.put(HikeConstants.MESSAGE_ID, new Random().nextInt(10000));
		data.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000); 

		JSONObject message = new JSONObject();
		message.put(HikeConstants.TO, msisdn);
		message.put(HikeConstants.TYPE, type);
		message.put(HikeConstants.SUB_TYPE, subtype);
		message.put(HikeConstants.DATA, data);
		
		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, message);
    }

    /**
     * Add a VoIP related message to the chat thread.
     * @param context
     * @param clientPartner
     * @param messageType
     * @param duration
     */
    public static void addMessageToChatThread(Context context, VoIPClient clientPartner, String messageType, int duration) {
		
    	Logger.d(VoIPConstants.TAG, "Adding message to chat thread. Message: " + messageType + ", Duration: " + duration);
    	HikeConversationsDatabase mConversationDb = HikeConversationsDatabase.getInstance();
    	Conversation mConversation = mConversationDb.getConversation(clientPartner.getPhoneNumber(), HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, Utils.isGroupConversation(clientPartner.getPhoneNumber()));	// TODO: possible crash here
    	/*
    	 * Caused by: java.lang.IllegalArgumentException: the bind value at index 1 is null
	at android.database.sqlite.SQLiteProgram.bindString(SQLiteProgram.java:164)
	at android.database.sqlite.SQLiteProgram.bindAllArgsAsStrings(SQLiteProgram.java:200)
	at android.database.sqlite.SQLiteDirectCursorDriver.query(SQLiteDirectCursorDriver.java:47)
	at android.database.sqlite.SQLiteDatabase.rawQueryWithFactory(SQLiteDatabase.java:1316)
	at android.database.sqlite.SQLiteDatabase.queryWithFactory(SQLiteDatabase.java:1163)
	at android.database.sqlite.SQLiteDatabase.query(SQLiteDatabase.java:1034)
	at android.database.sqlite.SQLiteDatabase.query(SQLiteDatabase.java:1202)
	at com.bsb.hike.db.HikeConversationsDatabase.getConversation(HikeConversationsDatabase.java:2079)
	at com.bsb.hike.voip.VoIPUtils.addMessageToChatThread(VoIPUtils.java:130)
    	 */
    	long timestamp = System.currentTimeMillis() / 1000;

		JSONObject jsonObject = new JSONObject();
		JSONObject data = new JSONObject();
		
		if (duration == 0 && messageType == HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY) {
			if (clientPartner.isInitiator())
				messageType = HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING;
			else
				messageType = HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING;
		}

		try
		{
			Logger.d(VoIPConstants.TAG, "Adding message of type: " + messageType + " to chat thread.");
			data.put(HikeConstants.MESSAGE_ID, Long.toString(timestamp));
			data.put(HikeConstants.VOIP_CALL_DURATION, duration);
			data.put(HikeConstants.VOIP_CALL_INITIATOR, !clientPartner.isInitiator());

			jsonObject.put(HikeConstants.DATA, data);
			jsonObject.put(HikeConstants.TYPE, messageType);
			jsonObject.put(HikeConstants.TO, clientPartner.getPhoneNumber());
//			jsonObject.put(HikeConstants.FROM, prefs.getString(HikeMessengerApp.MSISDN_SETTING, ""));
			
			ConvMessage convMessage = new ConvMessage(jsonObject, mConversation, context, true);
			mConversationDb.addConversationMessages(convMessage);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		}
		catch (JSONException e)
		{
			Logger.w(VoIPConstants.TAG, "addMessageToChatThread() JSONException: " + e.toString());
		}    	
    }

	public static void sendMissedCallNotificationToPartner(VoIPClient clientPartner) {

		try {
			JSONObject socketData = new JSONObject();
			socketData.put("time", System.currentTimeMillis());
			
			JSONObject data = new JSONObject();
			data.put(HikeConstants.MESSAGE_ID, new Random().nextInt(10000));
			data.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000); 
			data.put(HikeConstants.METADATA, socketData);

			JSONObject message = new JSONObject();
			message.put(HikeConstants.TO, clientPartner.getPhoneNumber());
			message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_VOIP_1);
			message.put(HikeConstants.SUB_TYPE, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING);
			message.put(HikeConstants.DATA, data);
			
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, message);
			Logger.d(VoIPConstants.TAG, "Sent missed call notifer to partner.");
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}

	/**
	 * Tells you how you are connected to the Internet. 
	 * 2G / 3G / WiFi etc.
	 * @param context
	 * @return ConnectionClass 2G / 3G / 4G / WiFi
	 */
	public static ConnectionClass getConnectionClass(Context context) {
		ConnectionClass connection = null;
		
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi != null && mWifi.isConnected()) {
		    connection = ConnectionClass.WiFi;
		} else {
		    TelephonyManager mTelephonyManager = (TelephonyManager)
		            context.getSystemService(Context.TELEPHONY_SERVICE);
		    int networkType = mTelephonyManager.getNetworkType();
		    switch (networkType) {
		        case TelephonyManager.NETWORK_TYPE_GPRS:
		        case TelephonyManager.NETWORK_TYPE_EDGE:
		        case TelephonyManager.NETWORK_TYPE_CDMA:
		        case TelephonyManager.NETWORK_TYPE_1xRTT:
		        case TelephonyManager.NETWORK_TYPE_IDEN:
		            connection = ConnectionClass.TwoG;
		            break;
		        case TelephonyManager.NETWORK_TYPE_UMTS:
		        case TelephonyManager.NETWORK_TYPE_EVDO_0:
		        case TelephonyManager.NETWORK_TYPE_EVDO_A:
		        case TelephonyManager.NETWORK_TYPE_HSDPA:
		        case TelephonyManager.NETWORK_TYPE_HSUPA:
		        case TelephonyManager.NETWORK_TYPE_HSPA:
		        case TelephonyManager.NETWORK_TYPE_EVDO_B:
		        case TelephonyManager.NETWORK_TYPE_EHRPD:
		        case TelephonyManager.NETWORK_TYPE_HSPAP:
		            connection = ConnectionClass.ThreeG;
		            break;
		        case TelephonyManager.NETWORK_TYPE_LTE:
		            connection = ConnectionClass.FourG;
		            break;
		        default:
		            connection = ConnectionClass.Unknown;
		            break;
		    }
		}
//		Logger.w(VoIPConstants.TAG, "Our connection class: " + connection.name());
		return connection;
	}
}
