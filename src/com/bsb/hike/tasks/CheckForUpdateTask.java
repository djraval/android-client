package com.bsb.hike.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.utils.Utils;

public class CheckForUpdateTask extends AsyncTask<Void, Void, Boolean>
	{
		public static final String STAGING_URL = "http://staging.get.hike.in/updates/android";
		public static final String PRODUCTION_URL = "http://get.hike.in/updates/android";
		
		public static String UPDATE_CHECK_URL = PRODUCTION_URL;
		/*
		 * Response JSON: {"latest":<string>, "critical":<string>, "url":<url>}
		 */
		private static final String LATEST = "latest";
		private static final String CRITICAL = "critical";
		private static final String URL = "url";

		private HikeService hikeService;

		public CheckForUpdateTask(HikeService hikeService)
		{
			this.hikeService = hikeService;
		}

		@Override
		protected Boolean doInBackground(Void... params) 
		{
			try 
			{
				URL url = new URL(UPDATE_CHECK_URL);
				HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
				httpURLConnection.setConnectTimeout(0);
				BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
				StringBuilder responseSB = new StringBuilder();
				String line = "";
				while((line = br.readLine()) != null)
				{
					responseSB.append(line);
				}
				br.close();

				String result = responseSB.toString();

				Log.d(getClass().getSimpleName(), "Response is: " + result);
				if(TextUtils.isEmpty(result))
				{
					return false;
				}
				else
				{
					try 
					{
						Editor editor = hikeService.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();

						JSONObject response = new JSONObject(result);

						/*
						 *  Here we check the source where this app was downloaded from if it was download from the play store,
						 *  the source won't be null and we store the update url as blank so that the user is redirected to 
						 *  the play store.
						 */
						String updateUrl = !TextUtils.isEmpty(hikeService.getPackageManager().getInstallerPackageName(hikeService.getPackageName())) ? 
								"" : response.optString(URL, "");
						editor.putString(HikeConstants.Extras.UPDATE_URL, updateUrl);

						String criticalVersion = response.optString(CRITICAL);
						if(!TextUtils.isEmpty(criticalVersion))
						{
							if(Utils.isUpdateRequired(criticalVersion, hikeService))
							{
								Log.d(getClass().getSimpleName(), "Critical update");
								editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.CRITICAL_UPDATE);
								editor.putString(HikeConstants.Extras.LATEST_VERSION, criticalVersion);
								editor.commit();

								HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_AVAILABLE, HikeConstants.CRITICAL_UPDATE);
								return true;
							}
						}

						String latestVersion = response.optString(LATEST);
						if(!TextUtils.isEmpty(latestVersion))
						{
							if(Utils.isUpdateRequired(latestVersion, hikeService))
							{
								Log.d(getClass().getSimpleName(), "Normal update");
								editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.NORMAL_UPDATE);
								editor.putString(HikeConstants.Extras.LATEST_VERSION, latestVersion);
								editor.commit();

								HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_AVAILABLE, HikeConstants.NORMAL_UPDATE);
								return true;
							}
						}
					} 
					catch (JSONException e) 
					{
						Log.d(getClass().getSimpleName(), "Invalid JSON", e);
					}
				}
			} 
			catch (MalformedURLException e) 
			{
				Log.e(getClass().getSimpleName(), "Invalid URL", e);
			}
			catch (SocketTimeoutException e) 
			{
				Log.e(getClass().getSimpleName(), "SocketTimeoutException", e);
			}
			catch (IOException e) 
			{
				Log.e(getClass().getSimpleName(), "IO Exception", e);
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) 
		{
			Log.d(getClass().getSimpleName(), "Was update successful? " +  result);
			hikeService.scheduleNextUpdateCheck();
		}
	}