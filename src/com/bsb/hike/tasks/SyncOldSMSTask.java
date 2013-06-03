package com.bsb.hike.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.SMSSyncState;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.State;

public class SyncOldSMSTask extends AsyncTask<Void, Void, SMSSyncState> {

	private static final String COLUMNS[] = new String[] {
			HikeConstants.SMSNative.NUMBER, HikeConstants.SMSNative.MESSAGE,
			HikeConstants.SMSNative.DATE, HikeConstants.SMSNative.READ };

	Context context;
	Map<String, List<ConvMessage>> smsMap;
	Set<String> rejectedNumbers;
	HikeUserDatabase hUDb;
	HikeConversationsDatabase hCDb;

	public SyncOldSMSTask(Context context) {
		this.context = context;
		this.smsMap = new HashMap<String, List<ConvMessage>>();
		this.rejectedNumbers = new HashSet<String>();
		this.hUDb = HikeUserDatabase.getInstance();
		this.hCDb = HikeConversationsDatabase.getInstance();
	}

	@Override
	protected SMSSyncState doInBackground(Void... params) {
		Cursor inboxCursor = null;
		Cursor sentboxCursor = null;

		try {

			inboxCursor = context.getContentResolver().query(
					HikeConstants.SMSNative.INBOX_CONTENT_URI, COLUMNS, null,
					null, null);

			extractCursorData(inboxCursor, true);

			sentboxCursor = context.getContentResolver().query(
					HikeConstants.SMSNative.SENTBOX_CONTENT_URI, COLUMNS, null,
					null, null);

			extractCursorData(sentboxCursor, false);

			boolean conversationsAdded = false;
			for (Entry<String, List<ConvMessage>> smsMapEntry : smsMap
					.entrySet()) {
				String msisdn = smsMapEntry.getKey();
				/*
				 * If a conversation with this msisdn already exists, we do not
				 * add this.
				 */
				if (hCDb.getConversationWithLastMessage(msisdn) != null) {
					Log.d(getClass().getSimpleName(),
							"Already have conversation: " + msisdn);
					continue;
				}
				Log.d(getClass().getSimpleName(), "new conversation: " + msisdn);
				List<ConvMessage> messages = smsMapEntry.getValue();
				Collections.sort(messages, new Comparator<ConvMessage>() {

					@Override
					public int compare(ConvMessage lhs, ConvMessage rhs) {
						return Long.valueOf(lhs.getTimestamp()).compareTo(
								rhs.getTimestamp());
					}
				});

				hCDb.addConversations(messages);
				conversationsAdded = true;
			}

			return conversationsAdded ? SMSSyncState.SUCCESSFUL
					: SMSSyncState.NO_CHANGE;
		} catch (Exception e) {
			/*
			 * Since we are accessing an unsupported api, we might get an
			 * exception while doing this operation. So catching any exception
			 * here.
			 */
			Log.w(getClass().getSimpleName(), "Unable to sync", e);

			return SMSSyncState.UNSUCCESSFUL;
		} finally {
			if (inboxCursor != null) {
				inboxCursor.close();
			}
			if (sentboxCursor != null) {
				sentboxCursor.close();
			}
		}
	}

	@Override
	protected void onPostExecute(SMSSyncState result) {
		if (result != SMSSyncState.SUCCESSFUL) {
			HikeMessengerApp.getPubSub()
					.publish(HikePubSub.SMS_SYNC_FAIL, null);
			return;
		}
		HikeMessengerApp.getPubSub()
				.publish(HikePubSub.SMS_SYNC_COMPLETE, null);
	}

	private void extractCursorData(Cursor cursor, boolean inbox) {
		int numberIdx = cursor.getColumnIndex(HikeConstants.SMSNative.NUMBER);
		int messageIdx = cursor.getColumnIndex(HikeConstants.SMSNative.MESSAGE);
		int dateIdx = cursor.getColumnIndex(HikeConstants.SMSNative.DATE);
		int readIdx = cursor.getColumnIndex(HikeConstants.SMSNative.READ);

		while (cursor.moveToNext()) {
			String number = cursor.getString(numberIdx);
			/*
			 * If this number has not been added to the map, we should check
			 * whether the contact exists first.
			 */
			if (!smsMap.containsKey(number)) {
				number = number.replaceAll(" ", "").replaceAll("-", "");

				if (rejectedNumbers.contains(number)) {
					Log.d(getClass().getSimpleName(), "Already rejected: "
							+ number);
					continue;
				}

				Log.d(getClass().getSimpleName(), "Checking contact: " + number);
				/*
				 * Check if contact exists
				 */
				ContactInfo contactInfo = hUDb
						.getContactInfoFromPhoneNoOrMsisdn(number);
				if (contactInfo == null) {
					Log.d(getClass().getSimpleName(), "Does not exist!!");
					rejectedNumbers.add(number);
					continue;
				}
				/*
				 * Making sure the number is normalised.
				 */
				number = contactInfo.getMsisdn();
				smsMap.put(number, new ArrayList<ConvMessage>());
			}
			String message = cursor.getString(messageIdx);
			long timestamp = cursor.getLong(dateIdx) / 1000;
			boolean seen = cursor.getInt(readIdx) == 1;
			long id = timestamp + message.hashCode() + number.hashCode();

			State state = inbox ? (seen ? State.RECEIVED_READ
					: State.RECEIVED_UNREAD) : State.SENT_CONFIRMED;

			ConvMessage convMessage = new ConvMessage(message, number,
					timestamp, state, -1, id, null, true);

			List<ConvMessage> messageList = smsMap.get(number);
			messageList.add(convMessage);
		}
	}
}