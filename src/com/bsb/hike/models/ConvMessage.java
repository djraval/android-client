package com.bsb.hike.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.NetworkManager;
import com.bsb.hike.R;
import com.ocpsoft.pretty.time.PrettyTime;

public class ConvMessage
{

	private long msgID; // this corresponds to msgID stored in sender's DB
	private long mappedMsgId; // this corresponds to msgID stored in receiver's DB

	private Conversation mConversation;

	private String mMessage;

	private String mMsisdn;

	private long mTimestamp;

	private boolean mIsSent;

	private boolean mIsSMS;

	private State mState;

	private boolean mInvite;

	private MessageMetadata metadata;

	public boolean isInvite()
	{
		return mInvite;
	}

	public void setInvite(boolean mIsInvite)
	{
		this.mInvite = mIsInvite;
	}

	/* Adding entries to the beginning of this list is not backwards compatible */
	public static enum State
	{
		SENT_UNCONFIRMED,  /* message sent to server */
		SENT_FAILED, /* message could not be sent, manually retry */
		SENT_CONFIRMED , /* message received by server */
		SENT_DELIVERED, /* message delivered to client device */
		SENT_DELIVERED_READ , /* message viewed by recipient */
		RECEIVED_UNREAD, /* message received, but currently unread */
		RECEIVED_READ, /* message received and read */
		UNKNOWN
	};

	public ConvMessage(String message, String msisdn, long timestamp, State msgState)
	{
		this(message, msisdn, timestamp, msgState, -1, -1);
	}

	public ConvMessage(String message, String msisdn, long timestamp, State msgState,long msgid , long mappedMsgId)
	{
		assert(msisdn != null);
		this.mMsisdn = msisdn;
		this.mMessage = message;
		this.mTimestamp = timestamp;
		this.msgID = msgid;
		this.mappedMsgId = mappedMsgId;
		mIsSent = (msgState == State.SENT_UNCONFIRMED ||
					msgState == State.SENT_CONFIRMED ||
					msgState == State.SENT_DELIVERED ||
					msgState == State.SENT_DELIVERED_READ ||
					msgState == State.SENT_FAILED);
		setState(msgState);
	}
	
	public ConvMessage(JSONObject obj) throws JSONException
	{
		this.mMsisdn = obj.getString(HikeConstants.FROM); /*represents msg is coming from another client*/
		JSONObject data = obj.getJSONObject(HikeConstants.DATA);
		if (data.has(HikeConstants.SMS_MESSAGE))
		{
			this.mMessage = data.getString(HikeConstants.SMS_MESSAGE);
			mIsSMS = true;
		} else
		{
			this.mMessage = data.getString(HikeConstants.HIKE_MESSAGE);
			mIsSMS = false;
		}

		this.mTimestamp = data.getLong(HikeConstants.TIMESTAMP);

		/* prevent us from receiving a message from the future */
		long now = System.currentTimeMillis()/1000;
		this.mTimestamp = (this.mTimestamp > now) ? now : this.mTimestamp;
		/* if we're deserialized an object from json, it's always unread */
		setState(State.RECEIVED_UNREAD);
		msgID = -1;
		String mappedMsgID = data.getString(HikeConstants.MESSAGE_ID);
		try
		{
			this.mappedMsgId=Long.parseLong(mappedMsgID);
		}
		catch(NumberFormatException e)
		{
			Log.e("CONVMESSAGE", "Exception occured while parsing msgId. Exception : "+e);
			this.mappedMsgId = -1;
			throw new JSONException("Problem in JSON while parsing msgID.");
		}

		if (data.has(HikeConstants.METADATA))
		{
			setMetadata(data.getJSONObject(HikeConstants.METADATA));
		}
	}

	public void setMetadata(JSONObject metadata)
	{
		if (metadata != null)
		{
			this.metadata = new MessageMetadata(metadata);
		}
	}

	public void setMetadata(String metadataString) throws JSONException
	{
		if (!TextUtils.isEmpty(metadataString))
		{
			JSONObject metadata = new JSONObject(metadataString);
			setMetadata(metadata);
		}
	}

	public MessageMetadata getMetadata()
	{
		return this.metadata;
	}

	public String getMessage()
	{
		return mMessage;
	}

	public boolean isSent()
	{
		return mIsSent;
	}

	public long getTimestamp()
	{
		return this.mTimestamp;
	}

	public State getState()
	{
		return mState;
	}

	public String getMsisdn()
	{
		return mMsisdn;
	}

	@Override
	public String toString()
	{
		String convId = mConversation == null ? "null" : Long.toString(mConversation.getConvId());
		return "ConvMessage [mConversation=" + convId + ", mMessage=" + mMessage + ", mMsisdn=" + mMsisdn + ", mTimestamp=" + mTimestamp
				+ ", mIsSent=" + mIsSent + ", mState=" + mState + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (mIsSent ? 1231 : 1237);
		result = prime * result + ((mMessage == null) ? 0 : mMessage.hashCode());
		result = prime * result + ((mMsisdn == null) ? 0 : mMsisdn.hashCode());
		result = prime * result + ((mState == null) ? 0 : mState.hashCode());
		result = prime * result + (int) (mTimestamp ^ (mTimestamp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConvMessage other = (ConvMessage) obj;

		if (mIsSent != other.mIsSent)
			return false;
		if (mMessage == null)
		{
			if (other.mMessage != null)
				return false;
		}
		else if (!mMessage.equals(other.mMessage))
			return false;
		if (mMsisdn == null)
		{
			if (other.mMsisdn != null)
				return false;
		}
		else if (!mMsisdn.equals(other.mMsisdn))
			return false;
		if (mState != other.mState)
			return false;
		if (mTimestamp != other.mTimestamp)
			return false;
		return true;
	}

	public JSONObject serialize()
	{
		JSONObject object = new JSONObject();
		JSONObject data = new JSONObject();
		try
		{
			data.put(HikeConstants.HIKE_MESSAGE, mMessage);
			data.put(HikeConstants.TIMESTAMP,mTimestamp);
			data.put(HikeConstants.MESSAGE_ID,msgID);

			object.put(HikeConstants.TO, mMsisdn);
			object.put(HikeConstants.DATA,data);

			object.put(HikeConstants.TYPE, mInvite ? NetworkManager.INVITE : NetworkManager.MESSAGE);
		}
		catch (JSONException e)
		{
			Log.e("ConvMessage", "invalid json message", e);
		}
		return object;
	}

	public void setConversation(Conversation conversation)
	{
		this.mConversation = conversation;
	}

	public Conversation getConversation()
	{
		return mConversation;
	}

	public String getTimestampFormatted(boolean pretty)
	{
		Date date = new Date(mTimestamp * 1000);
		if (pretty)
		{
			PrettyTime p = new PrettyTime();
			return p.format(date);			
		}
		else
		{
			String format = "d MMM ''yy 'AT' h:mm aaa";
			DateFormat df = new SimpleDateFormat(format);
			return df.format(date);
		}
	}

	public void setMsgID(long msgID)
	{
		this.msgID = msgID;
	}
	
	public long getMsgID()
	{
		return msgID;			
	}

	public void setMappedMsgID(long msgID)
	{
		this.mappedMsgId = msgID;
	}
	
	public long getMappedMsgID()
	{
		return mappedMsgId;			
	}

	public static State stateValue(int val)
	{
		return State.values()[val];
	}

	public void setState(State state)
	{
		/* only allow the state to increase */
		if (((mState != null) ? mState.ordinal() : 0) <= state.ordinal())
		{
			mState = state;
		}

		/* We have a bug where a message is flipping from sent to received
		 * add this assert to track down when/where it's happening
		assert(mIsSent == (mState == State.SENT_UNCONFIRMED ||
				mState == State.SENT_CONFIRMED ||
				mState == State.SENT_DELIVERED ||
				mState == State.SENT_DELIVERED_READ ||
				mState == State.SENT_FAILED));
		*/
	}

	public JSONObject serializeDeliveryReportRead()
	{
				JSONObject object = new JSONObject();
				JSONArray ids = new JSONArray();
				try
				{
					ids.put(String.valueOf(mappedMsgId));
					object.put(HikeConstants.DATA, ids);
					object.put(HikeConstants.TYPE, NetworkManager.MESSAGE_READ);
					object.put(HikeConstants.TO, mMsisdn);
				}
				catch (JSONException e)
				{
					Log.e("ConvMessage", "invalid json message", e);
				}
				return object;
	}

	public boolean isSMS()
	{
		return mIsSMS;
	}

	public int getImageState()
	{
		/* received messages have no img */
		if (!isSent())
		{
			return -1;
		}

		/* failed is handled separately, since it's applicable to SMS messages */
		if (mState == State.SENT_FAILED)
		{
			return R.drawable.ic_failed;
		}

		if (isSMS())
		{
			return -1;
		}

		switch(mState)
		{
		case SENT_DELIVERED:
			return R.drawable.ic_delivered;
		case SENT_DELIVERED_READ:
			return R.drawable.ic_read;
		case SENT_CONFIRMED:
			return R.drawable.ic_sent;
		case SENT_UNCONFIRMED:
			return R.drawable.ic_tower2;
		default:
			return R.drawable.ic_blank;
		}
	}
}
