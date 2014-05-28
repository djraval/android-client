package com.bsb.hike.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class FetchFriendsTask extends AsyncTask<Void, Void, Void>
{
	private Context context;

	private FriendsAdapter friendsAdapter;

	private Map<String, ContactInfo> selectedPeople;

	private List<ContactInfo> groupTaskList;

	private List<ContactInfo> friendTaskList;

	private List<ContactInfo> hikeTaskList;

	private List<ContactInfo> smsTaskList;

	private List<ContactInfo> groupsList;

	private List<ContactInfo> friendsList;

	private List<ContactInfo> hikeContactsList;

	private List<ContactInfo> smsContactsList;

	private List<ContactInfo> groupsStealthList;

	private List<ContactInfo> friendsStealthList;

	private List<ContactInfo> hikeStealthContactsList;

	private List<ContactInfo> smsStealthContactsList;

	private List<ContactInfo> filteredGroupsList;

	private List<ContactInfo> filteredFriendsList;

	private List<ContactInfo> filteredSmsContactsList;

	private List<ContactInfo> filteredHikeContactsList;

	private String existingGroupId;

	private boolean fetchGroups = false;

	private boolean creatingOrEditingGroup = false;

	private int stealthMode;

	private Map<String, StatusMessage> lastStatusMessagesMap;

	private boolean fetchSmsContacts;

	boolean checkFavTypeInComparision;

	private boolean nativeSMSOn;

	public FetchFriendsTask(FriendsAdapter friendsAdapter, Context context, List<ContactInfo> friendsList, List<ContactInfo> hikeContactsList, List<ContactInfo> smsContactsList,
			List<ContactInfo> friendsStealthList, List<ContactInfo> hikeStealthContactsList, List<ContactInfo> smsStealthContactsList, List<ContactInfo> filteredFriendsList,
			List<ContactInfo> filteredHikeContactsList, List<ContactInfo> filteredSmsContactsList, boolean fetchSmsContacts, boolean checkFavTypeInComparision)
	{
		this(friendsAdapter, context, friendsList, hikeContactsList, smsContactsList, friendsStealthList, hikeStealthContactsList, smsStealthContactsList, filteredFriendsList,
				filteredHikeContactsList, filteredSmsContactsList, null, null, null, null, false, null, false, fetchSmsContacts, checkFavTypeInComparision);
	}

	public FetchFriendsTask(FriendsAdapter friendsAdapter, Context context, List<ContactInfo> friendsList, List<ContactInfo> hikeContactsList, List<ContactInfo> smsContactsList,
			List<ContactInfo> friendsStealthList, List<ContactInfo> hikeStealthContactsList, List<ContactInfo> smsStealthContactsList, List<ContactInfo> filteredFriendsList,
			List<ContactInfo> filteredHikeContactsList, List<ContactInfo> filteredSmsContactsList, List<ContactInfo> groupsList, List<ContactInfo> groupsStealthList,
			List<ContactInfo> filteredGroupsList, Map<String, ContactInfo> selectedPeople, boolean fetchGroups, String existingGroupId, boolean creatingOrEditingGrou,
			boolean fetchSmsContacts, boolean checkFavTypeInComparision)
	{
		this.friendsAdapter = friendsAdapter;

		this.context = context;

		this.groupsList = groupsList;
		this.friendsList = friendsList;
		this.hikeContactsList = hikeContactsList;
		this.smsContactsList = smsContactsList;

		this.groupsStealthList = groupsStealthList;
		this.friendsStealthList = friendsStealthList;
		this.hikeStealthContactsList = hikeStealthContactsList;
		this.smsStealthContactsList = smsStealthContactsList;

		this.filteredGroupsList = filteredGroupsList;
		this.filteredFriendsList = filteredFriendsList;
		this.filteredHikeContactsList = filteredHikeContactsList;
		this.filteredSmsContactsList = filteredSmsContactsList;

		this.selectedPeople = selectedPeople;

		this.fetchGroups = fetchGroups;
		this.existingGroupId = existingGroupId;

		this.creatingOrEditingGroup = creatingOrEditingGroup;

		this.fetchSmsContacts = fetchSmsContacts;
		this.checkFavTypeInComparision = checkFavTypeInComparision;

		this.stealthMode = HikeSharedPreferenceUtil.getInstance(context).getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		this.nativeSMSOn = Utils.getSendSmsPref(context);
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		long startTime = System.currentTimeMillis();
		String myMsisdn = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, "");

		boolean removeExistingParticipants = !TextUtils.isEmpty(existingGroupId);

		if (fetchGroups)
		{
			groupTaskList = HikeConversationsDatabase.getInstance().getGroupNameAndParticipantsAsContacts(context);
			addToStealthList(groupTaskList, groupsStealthList, true);
		}

		HikeUserDatabase hikeUserDatabase = HikeUserDatabase.getInstance();

		long queryTime = System.currentTimeMillis();
		List<ContactInfo> allContacts = hikeUserDatabase.fetchAllContacts(myMsisdn);
		Map<String, FavoriteType> favTypeMap = hikeUserDatabase.fetchFavoriteTypeMap();
		Set<String> blockSet = hikeUserDatabase.getBlockedMsisdnSet();
		Logger.d("TestQuery", "qeury time: " + (System.currentTimeMillis() - queryTime));

		friendTaskList = new ArrayList<ContactInfo>();
		hikeTaskList = new ArrayList<ContactInfo>();
		smsTaskList = new ArrayList<ContactInfo>();

		long iterationTime = System.currentTimeMillis();
		for (ContactInfo contactInfo : allContacts)
		{
			String msisdn = contactInfo.getMsisdn();
			if (blockSet.contains(msisdn))
			{
				continue;
			}

			FavoriteType favoriteType = favTypeMap.get(msisdn);
			contactInfo.setFavoriteType(favoriteType);

			if (shouldAddToFavorites(favoriteType))
			{
				friendTaskList.add(contactInfo);

				/*
				 * Removing the contacts that have already been added to the list. At the end we will be left with unknown contacts.
				 */
				favTypeMap.remove(msisdn);
			}
			else
			{
				if (contactInfo.isOnhike())
				{
					hikeTaskList.add(contactInfo);
				}
				else if (fetchSmsContacts && shouldShowSmsContact(msisdn))
				{
					smsTaskList.add(contactInfo);
				}
			}
		}

		/*
		 * Adding the unknown favorites.
		 */
		for (Entry<String, FavoriteType> favoriteTypeEntry : favTypeMap.entrySet())
		{
			String msisdn = favoriteTypeEntry.getKey();
			FavoriteType favoriteType = favoriteTypeEntry.getValue();

			if (!shouldAddToFavorites(favoriteType) || !shouldShowSmsContact(msisdn))
			{
				continue;
			}

			ContactInfo contactInfo = new ContactInfo(msisdn, msisdn, null, msisdn);
			contactInfo.setFavoriteType(favoriteType);

			friendTaskList.add(contactInfo);
		}

		Logger.d("TestQuery", "Iteration time: " + (System.currentTimeMillis() - iterationTime));

		long sortTime = System.currentTimeMillis();
		Collections.sort(friendTaskList, checkFavTypeInComparision ? ContactInfo.lastSeenTimeComparator : ContactInfo.lastSeenTimeComparatorWithoutFav);
		Logger.d("TestQuery", "Sorting time: " + (System.currentTimeMillis() - sortTime));

		if (removeExistingParticipants)
		{
			Map<String, GroupParticipant> groupParticipants = HikeConversationsDatabase.getInstance().getGroupParticipants(existingGroupId, true, false);

			removeContactsFromList(friendTaskList, groupParticipants);
			removeContactsFromList(hikeTaskList, groupParticipants);
			if (fetchSmsContacts)
			{
				removeContactsFromList(smsTaskList, groupParticipants);
			}

			for (GroupParticipant groupParticipant : groupParticipants.values())
			{
				ContactInfo contactInfo = groupParticipant.getContactInfo();

				selectedPeople.put(contactInfo.getMsisdn(), contactInfo);
			}
		}
		addToStealthList(friendTaskList, friendsStealthList, false);
		addToStealthList(hikeTaskList, hikeStealthContactsList, false);
		if (fetchSmsContacts)
		{
			addToStealthList(smsTaskList, smsStealthContactsList, false);
		}

		lastStatusMessagesMap = HikeConversationsDatabase.getInstance().getLastStatusMessages(false, HikeConstants.STATUS_TYPE_LIST_TO_FETCH, friendTaskList);

		Logger.d("TestQuery", "total time: " + (System.currentTimeMillis() - startTime));

		return null;
	}

	private boolean shouldAddToFavorites(FavoriteType favoriteType)
	{
		return favoriteType == FavoriteType.REQUEST_RECEIVED || favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_SENT
				|| favoriteType == FavoriteType.REQUEST_SENT_REJECTED;
	}

	private boolean shouldShowSmsContact(String msisdn)
	{
		if (TextUtils.isEmpty(msisdn))
		{
			return false;
		}

		if (!nativeSMSOn)
		{
			return msisdn.startsWith(HikeConstants.INDIA_COUNTRY_CODE);
		}

		return true;
	}

	private void addToStealthList(List<ContactInfo> contactList, List<ContactInfo> stealthList, boolean isGroupTask)
	{
		if (creatingOrEditingGroup)
		{
			return;
		}

		for (Iterator<ContactInfo> iter = contactList.iterator(); iter.hasNext();)
		{
			ContactInfo contactInfo = iter.next();
			/*
			 * if case of group contactInfo.getId() will retrun groupId, which is treated as msisdn for groups.
			 */
			String msisdn = isGroupTask ? contactInfo.getId() : contactInfo.getMsisdn();
			if (HikeMessengerApp.isStealthMsisdn(msisdn))
			{
				stealthList.add(contactInfo);

				/*
				 * If stealth mode is currently off, we should remove these contacts from the list.
				 */
				if (stealthMode != HikeConstants.STEALTH_ON)
				{
					iter.remove();
				}
			}
		}
	}

	private void removeContactsFromList(List<ContactInfo> contactList, Map<String, GroupParticipant> groupParticipants)
	{
		for (Iterator<ContactInfo> iter = contactList.iterator(); iter.hasNext();)
		{
			ContactInfo contactInfo = iter.next();
			if (groupParticipants.containsKey(contactInfo.getMsisdn()))
			{
				iter.remove();
			}
		}
	}

	@Override
	protected void onPostExecute(Void result)
	{
		/*
		 * Clearing all the lists initially to ensure we remove any existing contacts in the list that might be there because of the 'ai' packet.
		 */
		clearAllLists();

		if (fetchGroups)
		{
			groupsList.addAll(groupTaskList);
		}
		friendsAdapter.initiateLastStatusMessagesMap(lastStatusMessagesMap);
		friendsList.addAll(friendTaskList);
		hikeContactsList.addAll(hikeTaskList);
		if (fetchSmsContacts)
		{
			smsContactsList.addAll(smsTaskList);
		}

		if (fetchGroups)
		{
			filteredGroupsList.addAll(groupTaskList);
		}
		filteredFriendsList.addAll(friendTaskList);
		filteredHikeContactsList.addAll(hikeTaskList);
		if (fetchSmsContacts)
		{
			filteredSmsContactsList.addAll(smsTaskList);
		}

		friendsAdapter.setListFetchedOnce(true);

		friendsAdapter.makeCompleteList(true);
	}

	private void clearAllLists()
	{
		if (fetchGroups)
		{
			groupsList.clear();
			filteredGroupsList.clear();
		}

		friendsList.clear();
		hikeContactsList.clear();
		smsContactsList.clear();
		filteredFriendsList.clear();
		filteredHikeContactsList.clear();
		filteredSmsContactsList.clear();
	}
}