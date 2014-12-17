package com.bsb.hike.analytics;

/**
 * @author rajesh
 *
 */
class AnalyticsConstants 
{	
	public static long MAX_FILE_SIZE = 10 * 1024;
	
	public static int HOUR_OF_DAY_TO_SEND = 2;
	
	public static boolean IS_ANALYTICS_ENABLED = true;
	
	public static final int MAX_EVENTS_IN_MEMORY = 10;
	
	public static final String HTTP_UPLOAD_URL = "http://staging.im.hike.in/v1/logs/analytics"; 
	
	public static final String EVENT_FILE_DIR = "/Analytics";
	
	public static final String NEW_LINE = "\n";
		
	public static final String EVENT_FILE_NAME = "eventdata";
	
	public static final String FILE_EXTENSION = ".txt";
	
	public static final String ANALYTICS_TAG = "hikeAnalytics";
	
	public static String TYPE = "t";

	public static String ANALYTICS_EVENT = "le";
	
	public static String EVENT_PRIORITY = "ep";

	public static String DATA = "d";
	
	public static String METADATA = "md";

	public static String UI_EVENT = "uiEvent";
	
	public static String CLICK_EVENT = "click";
				
	public static String SUB_TYPE = "st";
	
	public static String EVENT_TYPE = "et";		
	
	public static String EVENT_KEY = "ek";
	
	public static String TO = "to";	
}
