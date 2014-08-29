package com.bsb.hike.models;

import java.util.List;

import com.bsb.hike.utils.PairModified;

import android.util.Pair;

public abstract class ProfileItem
{

	public static final int HEADER_ID = -1;

	public static final int EMPTY_ID = -2;

	public static final int REQUEST_ID = -3;
	
	public static final int HEADER_ID_GROUP = -4;
	
	public static final int SHARED_MEDIA = -5;
	
	public static final int SHARED_CONTENT = -6;

	public static final int MEMBERS = -7;
	
	public static final int ADD_MEMBERS = -8;
	
	public static final int GROUP_MEMBER = -9;
	
	public static final int HEADER_ID_PROFILE = -10;
	
	public static final int PHONE_NUMBER = -11;

	private int itemId;
	
	private Object text;

	public ProfileItem(int itemId, Object text)
	{
		this.itemId = itemId;
		if(text != null)
			this.text = text;
	}

	public int getItemId()
	{
		return itemId;
	}
	
	public Object getText()
	{
		return text;
	}

	public static class ProfileStatusItem extends ProfileItem
	{

		private StatusMessage statusMessage;

		public ProfileStatusItem(int itemId)
		{
			super(itemId, null);
		}

		public ProfileStatusItem(StatusMessage statusMessage)
		{
			super(0, null);
			this.statusMessage = statusMessage;
		}

		public StatusMessage getStatusMessage()
		{
			return statusMessage;
		}
	}

	public static class ProfileGroupItem extends ProfileItem
	{

		private List<PairModified<GroupParticipant, String>> groupParticipants;

		public ProfileGroupItem(int itemId, Object text)
		{
			super(itemId, text);
		}

		public ProfileGroupItem(int itemId, List<PairModified<GroupParticipant, String>> groupParticipants)
		{
			super(itemId, null);
			this.groupParticipants = groupParticipants;
		}

		public List<PairModified<GroupParticipant, String>> getGroupParticipants()
		{
			return groupParticipants;
		}
	}

	public static class ProfileContactItem extends ProfileItem
	{
		public static enum contactType
		{ SHOW_CONTACTS_STATUS, NOT_A_FRIEND, UNKNOWN_ON_HIKE, REQUEST_RECEIVED, UNKNOWN_NOT_ON_HIKE }
		
		private int contact;
		
		public ProfileContactItem(int itemId, contactType contact ,Object text)
		{
			super(itemId, text);
			this.contact = contact.ordinal();
		}
		
		public ProfileContactItem(int itemId, Object text)
		{
			// TODO Auto-generated constructor stub
			super(itemId, text);
		}
		
		public int getContactType()
		{
			return contact;
		}
	}
	
	public static class ProfileSharedMedia extends ProfileItem
	{
		public ProfileSharedMedia(int itemId, Object text)
		{
			super(itemId, text);
			// TODO Auto-generated constructor stub
		}
		public ProfileSharedMedia(int itemId, Object text, int sizeOfImage,List<HikeSharedFile> sharedFilesList)
		{
			super(itemId,text);
			this.sharedFilesList = sharedFilesList;
			this.sizeOfImage = sizeOfImage;
		}
		
		public List<HikeSharedFile> getSharedFileList()
		{
			return sharedFilesList;
		}
		public int getSizeofImage()
		{
			return sizeOfImage;
		}
		private List<HikeSharedFile> sharedFilesList;
		private int sizeOfImage;
	}
	
	public static class ProfileSharedContent extends ProfileItem
	{
		public ProfileSharedContent(int itemId, Object text)
		{
			super(itemId, text);
			// TODO Auto-generated constructor stub
		}
		public ProfileSharedContent(int itemId, Object text, int sharedFiles, int sharedPins ,List<HikeSharedFile> sharedFilesList)
		{
			super(itemId,text);
			this.sharedFilesList = sharedFilesList;
			this.sharedFiles = sharedFiles;
			this.sharedPins = sharedPins;
		}
		
		public List<HikeSharedFile> getSharedFileList()
		{
			return sharedFilesList;
		}
		public int getSharedFilesCount()
		{
			return sharedFiles;
		}
		public int getSharedPinsCount()
		{
			return sharedPins;
		}
		private List<HikeSharedFile> sharedFilesList;
		private int sharedFiles;
		private int sharedPins;
	}
	
}