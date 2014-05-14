package com.hraban.android.widget.waterforfitbit;

import org.apache.commons.lang.StringUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.format.Time;

public abstract class DataPreferences {
	
	private static final String PREFERENCES = "data_preferences";
	
	private static final String AMOUNT_SEND_QUEUE = "amount_send_queue";
	private static final String AMOUNT_SEND_QUEUE_DATETIME = "amount_send_queue_datetime";

	private static final String AMOUNT_LOGGED = "amount_logged";
	private static final String AMOUNT_LOGGED_DATE = "amount_logged_date";
	
	private static final String GOAL = "goal";


	public static class Amount {
		private final float amount;
		private final Time datetime;
		
		public Amount(final float amount, final Time datetime) {
			this.amount = amount;
			this.datetime = datetime;
		}

		public float getAmount() {
			return amount;
		}

		public Time getDatetime() {
			return datetime;
		}
	}

	public static boolean isSendQueueEmpty(final Context context) {
		return StringUtils.isEmpty(context.getSharedPreferences(PREFERENCES, 0).getString(AMOUNT_SEND_QUEUE, null));
	}

	public static Amount popAmountFromSendQueue(final Context context) {
		final SharedPreferences dataPreferences = context.getSharedPreferences(PREFERENCES, 0);
		
		if (!dataPreferences.contains(AMOUNT_SEND_QUEUE) || !dataPreferences.contains(AMOUNT_SEND_QUEUE_DATETIME)) {
			return null;
		}
		
		final String[] amountsToLog = dataPreferences.getString(AMOUNT_SEND_QUEUE, null).split(",", 2);
		final String[] amountsToLogDatetime = dataPreferences.getString(AMOUNT_SEND_QUEUE_DATETIME, null).split(",", 2);

		Amount amount = null;
		if (amountsToLog.length > 0 && amountsToLogDatetime.length > 0) {

			final Time datetime = new Time();
			datetime.parse(amountsToLogDatetime[0]);
			datetime.normalize(false);
			
			amount = new Amount(Float.parseFloat(amountsToLog[0]), datetime);
		}
		
		final Editor dataPreferencesEditor = dataPreferences.edit();
		if (amountsToLog.length > 1 && amountsToLogDatetime.length > 1) {
			dataPreferencesEditor.putString(AMOUNT_SEND_QUEUE, amountsToLog[1]);
			dataPreferencesEditor.putString(AMOUNT_SEND_QUEUE_DATETIME, amountsToLogDatetime[1]);
		}
		else {
			dataPreferencesEditor.remove(AMOUNT_SEND_QUEUE);
			dataPreferencesEditor.remove(AMOUNT_SEND_QUEUE_DATETIME);
		}
		dataPreferencesEditor.commit();

		return amount;
	}
	
	public static void pushAmountOnSendQueue(final Context context, final Amount amount) {
		final SharedPreferences dataPreferences = context.getSharedPreferences(PREFERENCES, 0);
		final Editor dataPreferencesEditor = dataPreferences.edit();
		
		pushAmountOnSendQueue(dataPreferences, dataPreferencesEditor, amount);
		
		dataPreferencesEditor.commit();
	}

	private static void pushAmountOnSendQueue(final SharedPreferences dataPreferences, final Editor dataPreferencesEditor, final Amount amount) {
		final String previousAmountToLog = dataPreferences.getString(AMOUNT_SEND_QUEUE, null);
		final String previousAmountToLogDate = dataPreferences.getString(AMOUNT_SEND_QUEUE_DATETIME, null);
		dataPreferencesEditor.putString(AMOUNT_SEND_QUEUE,
				previousAmountToLog == null ?
						String.valueOf(amount.getAmount()) :
							previousAmountToLog + ',' + amount.getAmount());
		dataPreferencesEditor.putString(AMOUNT_SEND_QUEUE_DATETIME,
				previousAmountToLogDate == null ?
						amount.getDatetime().format2445() :
							previousAmountToLogDate + ',' + amount.getDatetime().format2445());
	}
	
	
	public static void addAmount(final Context context, final float amountToLog) {
		final SharedPreferences dataPreferences = context.getSharedPreferences(PREFERENCES, 0);
		final Editor dataPreferencesEditor = dataPreferences.edit();

		final Time time = new Time();
		time.setToNow();

		pushAmountOnSendQueue(dataPreferences, dataPreferencesEditor, new Amount(amountToLog, time));
		
		dataPreferencesEditor.putFloat(AMOUNT_LOGGED, getAmountLoggedToday(dataPreferences, dataPreferencesEditor, time) + amountToLog);

		dataPreferencesEditor.commit();
	}

	public static void setGoal(final Context context, final float goal) {
		context.getSharedPreferences(PREFERENCES, 0).edit().putFloat(GOAL, goal).commit();
	}
	
	public static float getGoal(final Context context) {
		return context.getSharedPreferences(PREFERENCES, 0).getFloat(GOAL, 64);
	}

	public static void setAmountLoggedToday(final Context context, final float amount) {
		final Time time = new Time();
		time.setToNow();

		final SharedPreferences dataPreferences = context.getSharedPreferences(PREFERENCES, 0);
		final Editor dataPreferencesEditor = dataPreferences.edit();

		resetIfNewDay(dataPreferences, dataPreferencesEditor, time);

		dataPreferencesEditor.putFloat(AMOUNT_LOGGED, amount);

		dataPreferencesEditor.commit();
	}

	public static float getAmountLoggedToday(final Context context) {
		final Time time = new Time();
		time.setToNow();

		final SharedPreferences dataPreferences = context.getSharedPreferences(PREFERENCES, 0);
		final Editor dataPreferencesEditor = dataPreferences.edit();

		final float amountLoggedToday = getAmountLoggedToday(dataPreferences, dataPreferencesEditor, time);

		dataPreferencesEditor.commit();
		
		return amountLoggedToday;
	}

	private static float getAmountLoggedToday(final SharedPreferences dataPreferences, final Editor dataPreferencesEditor, final Time time) {
		resetIfNewDay(dataPreferences, dataPreferencesEditor, time);
		return dataPreferences.getFloat(AMOUNT_LOGGED, 0.0f);
	}
	
	private static void resetIfNewDay(final SharedPreferences dataPreferences, final Editor dataPreferencesEditor, final Time time) {
		if (!dataPreferences.getString(AMOUNT_LOGGED_DATE, "").equals(time.format3339(true))) {
			dataPreferencesEditor.putFloat(AMOUNT_LOGGED, 0.0f);
			dataPreferencesEditor.putString(AMOUNT_LOGGED_DATE, time.format3339(true));
		}
	}
}
