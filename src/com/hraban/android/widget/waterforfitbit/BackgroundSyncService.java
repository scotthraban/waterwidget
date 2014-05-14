package com.hraban.android.widget.waterforfitbit;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fitbit.api.FitbitAPIException;

public class BackgroundSyncService extends IntentService {

	private static final String PREFERENCES = "background_sync_preferences";
	private static final String LAST_SUCCESSFUL_SYNC = "last_successful_sync";
	private static final String LAST_ALARM = "last_alarm";

	public static long getLastSuccessfulSync(final Context context) {
		return context.getSharedPreferences(PREFERENCES, 0).getLong(LAST_SUCCESSFUL_SYNC, System.currentTimeMillis());
	}
	
	public static long getLastAlarm(final Context context) {
		return context.getSharedPreferences(PREFERENCES, 0).getLong(LAST_ALARM, 0l);
	}

	public static void schedule(final Context context) {
		final long lastSuccessfulSync = getLastSuccessfulSync(context);

		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		final PendingIntent pendingIntent = PendingIntent.getService(
				context, 0, new Intent(context, BackgroundSyncService.class), 0);

		alarmManager.cancel(pendingIntent);

		// Set the alarm for at least 1 second in the future for the first run
		alarmManager.setInexactRepeating(
				AlarmManager.RTC,
				Math.max(System.currentTimeMillis() + 1000, lastSuccessfulSync + AlarmManager.INTERVAL_FIFTEEN_MINUTES),
				AlarmManager.INTERVAL_FIFTEEN_MINUTES,
				pendingIntent);
	}

	public static void notifySuccessfulSync(final Context context) {
		context.getSharedPreferences(PREFERENCES, 0).edit().putLong(LAST_SUCCESSFUL_SYNC, System.currentTimeMillis()).commit();
	}

	public BackgroundSyncService() {
		super(BackgroundSyncService.class.getSimpleName());
	}

	@Override
	protected void onHandleIntent(final Intent intent) {
		
		final Context context = getApplicationContext();

		context.getSharedPreferences(PREFERENCES, 0).edit().putLong(LAST_ALARM, System.currentTimeMillis()).commit();

		if (!WaterWidgetProvider.isBatteryCritical(context) && FitbitAdapter.isConnected(context)) {

			final long lastSuccessfulSync = context.getSharedPreferences(PREFERENCES, 0).getLong(LAST_SUCCESSFUL_SYNC, 0L);
			if (System.currentTimeMillis() > (lastSuccessfulSync + AlarmManager.INTERVAL_FIFTEEN_MINUTES) &&
					!DataPreferences.isSendQueueEmpty(getApplicationContext())) {
				doBackgroundSync();
			}
			else if (System.currentTimeMillis() > (lastSuccessfulSync + (AlarmManager.INTERVAL_HOUR * 2))) {
				doBackgroundSync();
			}
		}
	}

	protected void doBackgroundSync() {

		try {
			boolean failure = false;

			final FitbitAdapter fitbitAdapter = new FitbitAdapter(getApplicationContext());

			try {
				fitbitAdapter.sendWaterConsumptionLog(getApplicationContext());
			}
			catch (final FitbitAPIException e) {
				failure = true;
				Log.e(getClass().getSimpleName(), "Unable to send water comsumption log", e);
			}
	
			if (!WaterWidgetProvider.shouldSaveOnBattery(getApplicationContext())) {
				try {
					fitbitAdapter.getAndStoreWaterConsumptionTotal(getApplicationContext());
				}
				catch (final FitbitAPIException e) {
					failure = true;
					Log.e(getClass().getSimpleName(), "Unable to fetch water consumption from fitbit", e);
				}
	
				try {
					fitbitAdapter.getAndStoreWaterConsumptionGoal(getApplicationContext());
				}
				catch (final FitbitAPIException e) {
					failure = true;
					Log.e(getClass().getSimpleName(), "Unable to fetch water consumption goal from fitbit", e);
				}
			}

			if (!failure) {
				WaterWidgetProvider.update(getApplicationContext(), null);
			}
		}
		catch (final FitbitAPIException e) {
			Log.e(getClass().getSimpleName(), "Unable to create fitbit adapter", e);
		}
	}
}
