package com.hraban.android.widget.waterforfitbit;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.widget.RemoteViews;

public class WaterWidgetProvider extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int i = 0; i < appWidgetIds.length; i++) {
			update(context, appWidgetIds[i]);
		}
		
		BackgroundSyncService.schedule(context);
	}
	
	public static void update(final Context context, Integer appWidgetId) {
		final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_main);

		remoteViews.setOnClickPendingIntent(R.id.textview_add_water,
				PendingIntent.getActivity(context, 0, new Intent(context, AddWaterConsumptionActivity.class), 0));

		final Float amountLoggedToday = Float.valueOf(DataPreferences.getAmountLoggedToday(context));
		
		remoteViews.setTextViewText(
				R.id.textview_add_water,
				context.getString(
						FitbitAdapter.getWaterResId(
								R.string.button_add_water_text_imperial,
								R.string.button_add_water_text_metric),
						amountLoggedToday.intValue()));
		
		final float percentageOfGoal = amountLoggedToday / DataPreferences.getGoal(context);
		if (percentageOfGoal > 1.0f) {
			remoteViews.setInt(R.id.textview_add_water, "setBackgroundResource", R.drawable.widget_background_green);
			remoteViews.setTextColor(R.id.textview_add_water, context.getResources().getColor(R.color.fitbit_green));
		}
		else if (percentageOfGoal > 0.666f) {
			remoteViews.setInt(R.id.textview_add_water, "setBackgroundResource", R.drawable.widget_background_orange);
			remoteViews.setTextColor(R.id.textview_add_water, context.getResources().getColor(R.color.fitbit_orange));
		}
		else if (percentageOfGoal > 0.333f) {
			remoteViews.setInt(R.id.textview_add_water, "setBackgroundResource", R.drawable.widget_background_yellow);
			remoteViews.setTextColor(R.id.textview_add_water, context.getResources().getColor(R.color.fitbit_yellow));
		}
		else {
			remoteViews.setInt(R.id.textview_add_water, "setBackgroundResource", R.drawable.widget_background_blue);
			remoteViews.setTextColor(R.id.textview_add_water, context.getResources().getColor(R.color.fitbit_blue));
		}
		
		if (appWidgetId == null) {
			AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, WaterWidgetProvider.class), remoteViews);
		}
		else {
			AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews);
		}
	}

	public static boolean isBatteryCritical(final Context context) {
		return getBatteryPercentage(context) < 0.15f;
	}

	public static boolean shouldSaveOnBattery(final Context context) {
		return getBatteryPercentage(context) < 0.333f;
	}
	
	private static float getBatteryPercentage(final Context context) {
		final Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		float percentage = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) / (float)batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		return percentage;
	}
}
