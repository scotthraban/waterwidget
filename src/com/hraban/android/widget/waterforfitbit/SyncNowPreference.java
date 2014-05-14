package com.hraban.android.widget.waterforfitbit;

import com.fitbit.api.FitbitAPIException;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

public class SyncNowPreference extends Preference {

	@Override
	protected void onClick() {
		super.onClick();

		new SyncNowTask().execute();
	}
	
	private class SyncNowTask extends AsyncTask<Void, Void, Boolean> {
		
		@Override
		protected Boolean doInBackground(Void... params) {

			final FitbitAdapter fitbitAdapter;
			try {
				fitbitAdapter = new FitbitAdapter(getContext());
			}
			catch (final FitbitAPIException e) {
				Log.e(getClass().getSimpleName(), "Unable to create fitbit adapter", e);
				return false;
			}
			
			boolean failure = false;
			
			try {
				fitbitAdapter.sendWaterConsumptionLog(getContext());
			}
			catch (final FitbitAPIException e) {
				failure = true;
				Log.e(getClass().getSimpleName(), "Unable to send water consumption log", e);
			}
			
			try {
				fitbitAdapter.getAndStoreWaterConsumptionTotal(getContext());
			}
			catch (final FitbitAPIException e) {
				failure = true;
				Log.e(getClass().getSimpleName(), "Unable to fetch water consumption from fitbit", e);
			}

			try {
				fitbitAdapter.getAndStoreWaterConsumptionGoal(getContext());
			}
			catch (final FitbitAPIException e) {
				failure = true;
				Log.e(getClass().getSimpleName(), "Unable to fetch water comsumption goal from fitbit", e);
			}

			return !failure;
		}
		
		@Override
		protected void onPostExecute(Boolean success) {
			super.onPostExecute(success);
			if (success) {
				Toast.makeText(getContext(), R.string.sync_now_success_toast, Toast.LENGTH_SHORT).show();
				WaterWidgetProvider.update(getContext(), null);
			}
			else {
				Toast.makeText(getContext(), R.string.sync_now_failure_toast, Toast.LENGTH_SHORT).show();
			}
		}
	}

	public SyncNowPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public SyncNowPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SyncNowPreference(Context context) {
		super(context);
	}
}
