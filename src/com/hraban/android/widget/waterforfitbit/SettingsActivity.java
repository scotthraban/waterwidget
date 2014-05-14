package com.hraban.android.widget.waterforfitbit;

import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;

public class SettingsActivity extends FragmentActivity {

	public static final String ADD_CHUNK_SIZE = "add_chunk_size";

	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	private OAuthParameters oAuthParameters;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		oAuthParameters = new OAuthParameters(getApplicationContext());
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
		    appWidgetId = extras.getInt(
		            AppWidgetManager.EXTRA_APPWIDGET_ID, 
		            AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

		if (!oAuthParameters.isAuthorized()) {
			setWidgetResult(RESULT_CANCELED);
	    	startActivity(new Intent(this, OAuthAuthorizationActivity.class));
		}
		else {
			setWidgetResult(RESULT_OK);
		}
	}

	protected void setWidgetResult(final int resultCode) {
		if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
			final Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			setResult(resultCode, resultValue);
		}
	}
		
	@Override
	protected void onResume() {
		super.onResume();
		
	    if (oAuthParameters.isAuthorized()) {
			// TODO: Change so that kicks off initial sync, instead of doing that inside the oauth task,
			// auth can succeed, but sync not, and looks like auth failed.
			setWidgetResult(RESULT_OK);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		BackgroundSyncService.schedule(getApplicationContext());

		WaterWidgetProvider.update(getApplicationContext(), null);
	}

	public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
		private static final String ADD_CHUNK_SIZE_KEY = "add_chunk_size";
		private static final String SYNC_NOW_KEY = "sync_now";

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
		}
		
		@Override
		public void onStart() {
			super.onStart();
			setSummaries();
		}

		@Override
		public void onResume() {
			super.onResume();
			getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		}
		
		@Override
		public void onPause() {
			super.onPause();
			getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		}
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			setSummaries();
		}
		
		private void setSummaries() {
			findPreference(ADD_CHUNK_SIZE_KEY).setSummary(
					getResources().getString(
							FitbitAdapter.getWaterResId(
									R.string.settings_add_chunk_size_summary_imperial,
									R.string.settings_add_chunk_size_summary_metric),
							getPreferenceScreen().getSharedPreferences().getString(ADD_CHUNK_SIZE, "0")));

			final Context context = getActivity().getApplicationContext();
			final long lastSuccessfulSync = BackgroundSyncService.getLastSuccessfulSync(context);
			final long lastAlarm = BackgroundSyncService.getLastAlarm(context);
			
			findPreference(SYNC_NOW_KEY).setSummary(
					getResources().getString(
							R.string.settings_sync_now_summary,
							new LocalDateTime(lastSuccessfulSync).toString(ISODateTimeFormat.dateTimeNoMillis()),
							new LocalDateTime(lastAlarm).toString(ISODateTimeFormat.dateTimeNoMillis())));
		}
	}
}
