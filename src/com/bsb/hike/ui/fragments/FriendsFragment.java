package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.adapters.FriendsAdapter.ViewType;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.ComposeActivity;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.utils.Utils;

public class FriendsFragment extends SherlockListFragment implements Listener,
		OnItemLongClickListener {

	private FriendsAdapter friendsAdapter;

	private String[] pubSubListeners = { HikePubSub.ICON_CHANGED,
			HikePubSub.FAVORITE_TOGGLED, HikePubSub.USER_JOINED,
			HikePubSub.USER_LEFT, HikePubSub.CONTACT_ADDED,
			HikePubSub.REFRESH_FAVORITES, HikePubSub.FRIEND_REQUEST_ACCEPTED,
			HikePubSub.REJECT_FRIEND_REQUEST, HikePubSub.BLOCK_USER,
			HikePubSub.UNBLOCK_USER, HikePubSub.LAST_SEEN_TIME_UPDATED,
			HikePubSub.LAST_SEEN_TIME_BULK_UPDATED,
			HikePubSub.FRIENDS_TAB_QUERY, HikePubSub.FREE_SMS_TOGGLED };

	private SharedPreferences preferences;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View parent = inflater.inflate(R.layout.friends, null);

		ListView friendsList = (ListView) parent
				.findViewById(android.R.id.list);
		friendsList.setEmptyView(parent.findViewById(android.R.id.empty));

		friendsAdapter = new FriendsAdapter(getActivity());
		friendsList.setAdapter(friendsAdapter);

		friendsList.setOnItemLongClickListener(this);
		return parent;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		preferences = getActivity().getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	public void onDestroy() {
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		ContactInfo contactInfo = friendsAdapter.getItem(position);

		if (FriendsAdapter.SECTION_ID.equals(contactInfo.getId())
				|| FriendsAdapter.EMPTY_ID.equals(contactInfo.getId())) {
			return;
		}

		if (FriendsAdapter.EXTRA_ID.equals(contactInfo.getId())) {
			Intent intent;
			if (FriendsAdapter.INVITE_MSISDN.equals(contactInfo.getMsisdn())) {
				intent = new Intent(getActivity(), TellAFriend.class);
			} else {
				intent = new Intent(getActivity(), ComposeActivity.class);
				intent.putExtra(HikeConstants.Extras.CREATE_GROUP, true);
			}
			startActivity(intent);
		} else {
			Intent intent = new Intent(getActivity(), ChatThread.class);
			if (contactInfo.getName() != null) {
				intent.putExtra(HikeConstants.Extras.NAME,
						contactInfo.getName());
			}
			intent.putExtra(HikeConstants.Extras.MSISDN,
					contactInfo.getMsisdn());
			intent.putExtra(HikeConstants.Extras.SHOW_KEYBOARD, true);
			startActivity(intent);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onEventReceived(final String type, final Object object) {
		if (!isAdded()) {
			return;
		}
		if (HikePubSub.ICON_CHANGED.equals(type)) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					friendsAdapter.notifyDataSetChanged();
				}
			});
		} else if (HikePubSub.USER_JOINED.equals(type)
				|| HikePubSub.USER_LEFT.equals(type)) {
			final ContactInfo contactInfo = HikeUserDatabase.getInstance()
					.getContactInfoFromMSISDN((String) object, true);

			if (contactInfo == null) {
				return;
			}
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (HikePubSub.USER_JOINED.equals(type)) {
						friendsAdapter.addToGroup(contactInfo,
								FriendsAdapter.HIKE_INDEX);
					} else if (HikePubSub.USER_LEFT.equals(type)) {
						friendsAdapter.addToGroup(contactInfo,
								FriendsAdapter.SMS_INDEX);
					}
				}
			});
		} else if (HikePubSub.FAVORITE_TOGGLED.equals(type)
				|| HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type)
				|| HikePubSub.REJECT_FRIEND_REQUEST.equals(type)) {
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					FavoriteType favoriteType = favoriteToggle.second;
					ContactInfo contactInfo = favoriteToggle.first;
					contactInfo.setFavoriteType(favoriteType);
					if ((favoriteType == FavoriteType.FRIEND)
							|| (favoriteType == FavoriteType.REQUEST_SENT_REJECTED)
							|| (favoriteType == FavoriteType.REQUEST_SENT)
							|| (favoriteType == FavoriteType.REQUEST_RECEIVED)) {
						friendsAdapter.addToGroup(contactInfo,
								FriendsAdapter.FRIEND_INDEX);
					} else if (favoriteType == FavoriteType.NOT_FRIEND
							|| favoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED) {
						if (contactInfo.isOnhike()) {
							friendsAdapter.addToGroup(contactInfo,
									FriendsAdapter.HIKE_INDEX);
						} else {
							friendsAdapter.addToGroup(contactInfo,
									FriendsAdapter.SMS_INDEX);
						}
					}
				}
			});
		} else if (HikePubSub.CONTACT_ADDED.equals(type)) {
			final ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo == null) {
				return;
			}

			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if ((contactInfo.getFavoriteType() != FavoriteType.FRIEND)
							&& (contactInfo.getFavoriteType() != FavoriteType.REQUEST_SENT)
							&& (contactInfo.getFavoriteType() != FavoriteType.REQUEST_SENT_REJECTED)
							&& (contactInfo.getFavoriteType() != FavoriteType.REQUEST_RECEIVED)) {
						if (contactInfo.isOnhike()) {
							friendsAdapter.addToGroup(contactInfo,
									FriendsAdapter.HIKE_INDEX);
						} else {
							friendsAdapter.addToGroup(contactInfo,
									FriendsAdapter.SMS_INDEX);
						}
					} else {
						friendsAdapter.addToGroup(contactInfo,
								FriendsAdapter.FRIEND_INDEX);
					}
				}
			});
		} else if (HikePubSub.REFRESH_FAVORITES.equals(type)) {
			String myMsisdn = preferences.getString(
					HikeMessengerApp.MSISDN_SETTING, "");

			boolean nativeSMSOn = Utils.getSendSmsPref(getActivity());

			HikeUserDatabase hikeUserDatabase = HikeUserDatabase.getInstance();

			final List<ContactInfo> favoriteList = hikeUserDatabase
					.getContactsOfFavoriteType(FavoriteType.FRIEND,
							HikeConstants.BOTH_VALUE, myMsisdn, nativeSMSOn);
			favoriteList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_SENT, HikeConstants.BOTH_VALUE,
					myMsisdn, nativeSMSOn));
			favoriteList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_SENT_REJECTED,
					HikeConstants.BOTH_VALUE, myMsisdn, nativeSMSOn));
			favoriteList.addAll(hikeUserDatabase.getContactsOfFavoriteType(
					FavoriteType.REQUEST_RECEIVED, HikeConstants.BOTH_VALUE,
					myMsisdn, nativeSMSOn, false));
			Collections.sort(favoriteList, ContactInfo.lastSeenTimeComparator);
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					friendsAdapter.refreshGroupList(favoriteList,
							FriendsAdapter.FRIEND_INDEX);
				}
			});
		} else if (HikePubSub.BLOCK_USER.equals(type)
				|| HikePubSub.UNBLOCK_USER.equals(type)) {
			String msisdn = (String) object;
			final ContactInfo contactInfo = HikeUserDatabase.getInstance()
					.getContactInfoFromMSISDN(msisdn, true);
			final boolean blocked = HikePubSub.BLOCK_USER.equals(type);
			if (contactInfo == null) {
				return;
			}
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (blocked) {
						friendsAdapter.removeContact(contactInfo, true);
					} else {
						if (contactInfo.isOnhike()) {
							friendsAdapter.addToGroup(contactInfo,
									FriendsAdapter.HIKE_INDEX);
						} else {
							friendsAdapter.addToGroup(contactInfo,
									FriendsAdapter.SMS_INDEX);
						}
					}
				}
			});
		} else if (HikePubSub.LAST_SEEN_TIME_UPDATED.equals(type)) {
			final ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo.getFavoriteType() != FavoriteType.FRIEND) {
				return;
			}

			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					friendsAdapter.addToGroup(contactInfo,
							FriendsAdapter.FRIEND_INDEX);
				}

			});
		} else if (HikePubSub.LAST_SEEN_TIME_BULK_UPDATED.equals(type)) {
			List<ContactInfo> friendsList = friendsAdapter.getFriendsList();
			for (int i = 0; i < friendsList.size() - 1; i++) {
				String msisdn = friendsList.get(i).getMsisdn();
				long lastSeenUpdated = 0;
				if (HikeMessengerApp.lastSeenFriendsMap.get(msisdn) != null) {
					lastSeenUpdated = HikeMessengerApp.lastSeenFriendsMap.get(
							msisdn).longValue();
					long lastSeenPrevious = friendsList.get(i)
							.getLastSeenTime();
					if (lastSeenUpdated > lastSeenPrevious) {
						friendsList.get(i).setLastSeenTime(lastSeenUpdated);
					}
				}
			}
			Collections.sort(friendsAdapter.getFriendsList(),
					ContactInfo.lastSeenTimeComparator);
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					friendsAdapter.makeCompleteList(false);
				}
			});

		} else if (HikePubSub.FRIENDS_TAB_QUERY.equals(type)) {
			final String query = (String) object;
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					friendsAdapter.onQueryChanged(query);
				}
			});
		} else if (HikePubSub.FREE_SMS_TOGGLED.equals(type)) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					friendsAdapter.toggleShowSMSContacts(PreferenceManager
							.getDefaultSharedPreferences(getActivity())
							.getBoolean(HikeConstants.FREE_SMS_PREF, true)
							|| Utils.getSendSmsPref(getActivity()));
				}
			});
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		FriendsAdapter.ViewType viewType = FriendsAdapter.ViewType.values()[friendsAdapter
				.getItemViewType(position)];
		if (viewType != ViewType.FRIEND) {
			return false;
		}
		final ContactInfo contactInfo = friendsAdapter.getItem(position);

		ArrayList<String> optionsList = new ArrayList<String>();

		optionsList.add(getString(R.string.remove_from_friends));

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(
				getActivity(), R.layout.alert_item, R.id.item, options);

		builder.setAdapter(dialogAdapter,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String option = options[which];
						if (getString(R.string.remove_from_friends).equals(
								option)) {
							FavoriteType favoriteType;
							if (contactInfo.getFavoriteType() == FavoriteType.FRIEND) {
								favoriteType = FavoriteType.REQUEST_RECEIVED_REJECTED;
							} else {
								favoriteType = FavoriteType.NOT_FRIEND;
							}
							Pair<ContactInfo, FavoriteType> favoriteRemoved = new Pair<ContactInfo, FavoriteType>(
									contactInfo, favoriteType);
							HikeMessengerApp.getPubSub().publish(
									HikePubSub.FAVORITE_TOGGLED,
									favoriteRemoved);
						}
					}
				});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(
				getResources()
						.getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
	}
}