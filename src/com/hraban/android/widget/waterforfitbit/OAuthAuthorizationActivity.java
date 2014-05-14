package com.hraban.android.widget.waterforfitbit;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.fitbit.api.FitbitAPIException;

public class OAuthAuthorizationActivity extends Activity {

	private WebView webView;
	private FitbitAdapter fitbitAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		if (!FitbitAdapter.isConnected(this)) {
			Toast.makeText(getApplicationContext(), R.string.oauth_authorization_failure_no_connection_toast, Toast.LENGTH_SHORT).show();
			finish();
		}
		
		webView = new WebView(this);
		setContentView(webView);
        
		try {
			fitbitAdapter = new FitbitAdapter(getApplicationContext());
		}
		catch (final FitbitAPIException e) {
			Log.e(OAuthAuthorizationActivity.class.getSimpleName(), "Unable to create fitbit adapter", e);
			throw new IllegalStateException("Unable to create fitbit adapter", e);
		}
		
        new StartOAuthAuthorizationTask().execute();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (intent.getAction().equals("android.intent.action.VIEW")) {
			new CompleteOAuthAuthorizationTask().execute(
					intent.getData().getQueryParameter("oauth_token"),
					intent.getData().getQueryParameter("oauth_verifier"));
		}
	}
	
	private class StartOAuthAuthorizationTask extends AsyncTask<Void, Void, String> {
		
		@Override
		protected String doInBackground(Void... params) {
			try {
				return fitbitAdapter.getAuthorizationUrl();
			} catch (FitbitAPIException e) {
				Log.e(getClass().getSimpleName(), "Failed to start oauth authorization", e);
				Toast.makeText(getApplicationContext(), R.string.oauth_authorization_failure_toast, Toast.LENGTH_SHORT).show();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String authUrl) {
			super.onPostExecute(authUrl);
			
			if (authUrl == null) {
				finish();
			}
			else {
				webView.loadUrl(authUrl);
			}
		}
	}

	private class CompleteOAuthAuthorizationTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			try {
				fitbitAdapter.completeAuthorization(params[0], params[1]);
			}
			catch (final FitbitAPIException e) {
				Log.e(getClass().getSimpleName(), "Failed to complete authorization", e);
				return false;
			}
			
			try {
				fitbitAdapter.getAndStoreWaterConsumptionTotal(getApplicationContext());
			}
			catch (final FitbitAPIException e) {
				Log.e(getClass().getSimpleName(), "Failed to get current water log after authorization", e);
				return false;
			}

			try {
				fitbitAdapter.getAndStoreWaterConsumptionGoal(getApplicationContext());
			}
			catch (final FitbitAPIException e) {
				Log.e(getClass().getSimpleName(), "Failed to get current water goal after authorization", e);
				return false;
			}

			return true;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			super.onPostExecute(success);
			if (success) {
				Toast.makeText(getApplicationContext(), R.string.oauth_authorization_success_toast, Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(getApplicationContext(), R.string.oauth_authorization_failure_toast, Toast.LENGTH_SHORT).show();
			}
			finish();
		}
	}
}
