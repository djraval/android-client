package com.bsb.hike.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;

public class WelcomeActivity extends Activity
{
	private enum ServerStatus
	{
		MSISDN , MSISDN_NOT_FOUND , ERROR
	}
	public class AccountCreateActivity extends AsyncTask<Void, Void, ServerStatus>
	{

		@Override
		protected ServerStatus doInBackground(Void... arg0)
		{
			AccountUtils.AccountInfo accountInfo = AccountUtils.registerAccount(null,null);
				
			if (accountInfo != null)
			{
				SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
				SharedPreferences.Editor editor = settings.edit();
				String msisdn = accountInfo.msisdn;
				/* If No MSISDN for given number , go to Sms Fallback Screen*/
				if(TextUtils.isEmpty(msisdn))
				{
					editor.putBoolean(HikeMessengerApp.PHONE_NUMBER_VALIDATION, true);
					editor.commit();
					return ServerStatus.MSISDN_NOT_FOUND;
				}
				String token = accountInfo.token;
				String uid = accountInfo.uid;
				AccountUtils.setToken(token);
				editor.putString(HikeMessengerApp.TOKEN_SETTING, token);
				editor.putString(HikeMessengerApp.MSISDN_SETTING, msisdn);
				editor.putString(HikeMessengerApp.UID_SETTING, uid);
				editor.commit();
				return ServerStatus.MSISDN;
			}

			/* set the async task to null so the UI doesn't think we're still looking for the MSISDN */
			//mTask = null;

			return ServerStatus.ERROR;
		}

		@Override
		protected void onPostExecute(ServerStatus result)
		{

			if (result == ServerStatus.MSISDN)
			{
				startActivity(new Intent(WelcomeActivity.this, AccountCreateSuccess.class));
				finish();
			}
			else if(result == ServerStatus.MSISDN_NOT_FOUND)
			{
				startActivity(new Intent(WelcomeActivity.this, SmsFallback.class));
				finish();
			}
			else
			{

				if (mDialog != null)
				{
					mDialog.dismiss();
				}
				mErrorView.setVisibility(View.VISIBLE);
			}
			mTask = null;
		}
	}

	private ProgressDialog mDialog;

	private Button mAcceptButton;

	private ImageView mIconView;

	private AccountCreateActivity mTask;

	private View mErrorView;

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return mTask;
	}

	@Override
	public void onCreate(Bundle savedState)
	{
		super.onCreate(savedState);
		setContentView(R.layout.welcomescreen);
		mAcceptButton = (Button) findViewById(R.id.accept_tc);
		mIconView = (ImageView) findViewById(R.id.ic_edit_message);
		mErrorView = findViewById(R.id.error_text);

		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof AccountCreateActivity)
		{
			mTask = (AccountCreateActivity) retained;
			mDialog = ProgressDialog.show(this, null, getText(R.string.determining_phone_number));
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
		mTask=null;
	}

	public void onClick(View v)
	{
		if (v == mAcceptButton)
		{
			mDialog = ProgressDialog.show(this, null, getText(R.string.determining_phone_number));
			mTask = new AccountCreateActivity();
			mTask.execute();
		}
	}
}
