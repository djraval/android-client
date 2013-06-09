package com.bsb.hike.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Utils;

public class StatusMessage {

	public static enum StatusMessageType {
		TEXT, IMAGE, TEXT_IMAGE, PROFILE_PIC, FRIEND_REQUEST, FRIEND_REQUEST_ACCEPTED, NO_STATUS, USER_ACCEPTED_FRIEND_REQUEST
	}

	private long id;
	private String mappedId;
	private String msisdn;
	private String name;
	private String text;
	private StatusMessageType statusMessageType;
	private long timeStamp;
	private int moodId;
	private int timeOfDay;

	public StatusMessage(JSONObject statusMessageJson) {
		this.msisdn = statusMessageJson.optString(HikeConstants.FROM);

		this.timeStamp = statusMessageJson.optLong(HikeConstants.TIMESTAMP);
		/* prevent us from receiving a message from the future */
		long now = System.currentTimeMillis() / 1000;
		this.timeStamp = (this.timeStamp > now) ? now : this.timeStamp;

		JSONObject data = statusMessageJson.optJSONObject(HikeConstants.DATA);

		this.mappedId = data.optString(HikeConstants.STATUS_ID);

		if (data.optBoolean(HikeConstants.PROFILE)) {
			this.statusMessageType = StatusMessageType.PROFILE_PIC;
			this.text = "";
		} else if (data.has(HikeConstants.STATUS_MESSAGE)) {
			this.statusMessageType = StatusMessageType.TEXT;
			this.text = data.optString(HikeConstants.STATUS_MESSAGE);
		}
		this.moodId = data.optInt(HikeConstants.MOOD) - 1;
		this.timeOfDay = data.optInt(HikeConstants.TIME_OF_DAY);
	}

	public StatusMessage(long id, String mappedId, String msisdn, String name,
			String text, StatusMessageType statusMessageType, long timeStamp) {
		this(id, mappedId, msisdn, name, text, statusMessageType, timeStamp,
				-1, 0);
	}

	public StatusMessage(long id, String mappedId, String msisdn, String name,
			String text, StatusMessageType statusMessageType, long timeStamp,
			int moodId, int timeOfDay) {
		this.id = id;
		this.mappedId = mappedId;
		this.msisdn = msisdn;
		this.name = name;
		this.text = text;
		this.statusMessageType = statusMessageType;
		this.timeStamp = timeStamp;
		this.moodId = moodId;
		this.timeOfDay = timeOfDay;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public String getMappedId() {
		return mappedId;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getNotNullName() {
		return TextUtils.isEmpty(name) ? msisdn : name;
	}

	public String getText() {
		return text;
	}

	public StatusMessageType getStatusMessageType() {
		return statusMessageType;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public boolean hasMood() {
		return (moodId > -1 && moodId < Utils.getMoodsResource().length);
	}

	public int getMoodId() {
		return moodId;
	}

	public int getTimeOfDay() {
		return timeOfDay;
	}

	public String getTimestampFormatted(boolean pretty) {
		Date date = new Date(timeStamp * 1000);
		if (pretty) {
			PrettyTime p = new PrettyTime();
			return p.format(date);
		} else {
			String format = "d MMM ''yy 'AT' h:mm aaa";
			DateFormat df = new SimpleDateFormat(format);
			return df.format(date);
		}
	}
}
