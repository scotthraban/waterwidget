package com.hraban.android.widget.waterforfitbit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.util.Log;

public class OAuthParameters {

	private static final String PREFERENCES = "oauth_preferences";
	private static final String OAUTH_ACCESS_TOKEN = "oauth_access_token";
	private static final String OAUTH_ACCESS_TOKEN_SECRET = "oauth_access_token_secret";
	private static final String OAUTH_LOCAL_USER_ID = "oauth_local_user_id";

	private final Context context;
	
	private Properties clientProperties;

	public OAuthParameters(final Context context) {
		this.context = context;
	}

	public boolean isAuthorized() {
		return getOAuthAccessToken() != null &&
				getOAuthAccessTokenSecret() != null &&
				getOAuthLocalUserId() != null;
	}
	
	public void resetAuthentication() {
		context.getSharedPreferences(PREFERENCES, 0).edit()
			.remove(OAUTH_ACCESS_TOKEN)
			.remove(OAUTH_ACCESS_TOKEN_SECRET)
			.remove(OAUTH_LOCAL_USER_ID)
			.commit();
	}
	
	public String getConsumerKey() {
		return getClientProperties().getProperty("oauth.consumer.key");
	}

	public String getConsumerSecret() {
		return getClientProperties().getProperty("oauth.consumer.secret");
	}

	public String getCallbackUrl() {
		return "com.hraban.android.widget.waterforfitbit://oauth.callback";
	}

	public String getOAuthAccessToken() {
		return context.getSharedPreferences(PREFERENCES, 0).getString(OAUTH_ACCESS_TOKEN, null);
	}
	
	public void setOAuthAccessToken(final String oauthToken) {
		setPreference(OAUTH_ACCESS_TOKEN, oauthToken);
	}

	public String getOAuthAccessTokenSecret() {
		return context.getSharedPreferences(PREFERENCES, 0).getString(OAUTH_ACCESS_TOKEN_SECRET, null);
	}
	
	public void setOAuthAccessTokenSecret(final String oauthTokenSecret) {
		setPreference(OAUTH_ACCESS_TOKEN_SECRET, oauthTokenSecret);
	}
	
	public String getOAuthLocalUserId() {
		return context.getSharedPreferences(PREFERENCES, 0).getString(OAUTH_LOCAL_USER_ID, null);
	}
	
	public void setOAuthLocalUserId(final String oauthLocalUserId) {
		setPreference(OAUTH_LOCAL_USER_ID, oauthLocalUserId);
	}
	

	private Properties getClientProperties() {
		if (clientProperties == null) {
			clientProperties = new Properties();
			try {
				AssetManager assetManager = context.getAssets();
				InputStream inputStream = assetManager.open("fitbit.client.properties");
				clientProperties.load(inputStream);
			} catch (IOException e) {
				Log.e("OAuthParameters", "Failed to open fitbit properties file", e);
			}
		}
		return clientProperties;
	}

	private void setPreference(final String key, final String value) {
		final Editor editor = context.getSharedPreferences(PREFERENCES, 0).edit();
		editor.putString(key, value);
		editor.commit();
	}
}
