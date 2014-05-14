package com.hraban.android.widget.waterforfitbit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.fitbit.api.FitbitAPIException;

public class AddWaterConsumptionActivity extends FragmentActivity {

	private NumberPicker amountToLog;
	private Button addChunk;
	private TextView units;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.container_dialog);

        setFinishOnTouchOutside(false);
        
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
            	.add(R.id.container_dialog, new PlaceholderFragment())
            	.add(R.id.container_dialog, new HorizontalDividerFragment())
            	.add(R.id.container_dialog, new OkCancelFragment())
            	.commit();
        }
	}

	@Override
	protected void onStart() {
		super.onStart();
		
        amountToLog = (NumberPicker) findViewById(R.id.numberpicker_amount_to_log);
        addChunk = (Button) findViewById(R.id.button_add_chunk);
		units = (TextView) findViewById(R.id.textview_amount_to_log_units);

        amountToLog.setMaxValue(96);
        amountToLog.setWrapSelectorWheel(false);
	}

	@Override
	protected void onResume() {
		super.onResume();
		addChunk.setText("+" + PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.ADD_CHUNK_SIZE, "0"));

		units.setText(FitbitAdapter.getWaterResId(R.string.water_units_imperial, R.string.water_units_metric));
		
		// TODO: Handle not being authorized
	}
	
	public void onClickedAddChunk(final View view) {
		amountToLog.setValue(amountToLog.getValue() +
				Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this)
						.getString(SettingsActivity.ADD_CHUNK_SIZE, "0")));
	}
	
	public void onClickedCancel(final View view) {
		finish();
	}

	public void onClickedOk(final View view) {
		
		final float amountToLogValue = amountToLog.getValue();
		if (amountToLogValue != 0.0f) {
			DataPreferences.addAmount(this, amountToLogValue);

			WaterWidgetProvider.update(getApplicationContext(), null);
			
			if (WaterWidgetProvider.isBatteryCritical(getApplicationContext())) {
				Toast.makeText(getApplicationContext(), R.string.add_water_failure_battery_low_toast, Toast.LENGTH_SHORT).show();
			}
			else if (!FitbitAdapter.isConnected(this)) {
				Toast.makeText(getApplicationContext(), R.string.add_water_failure_no_connection_toast, Toast.LENGTH_SHORT).show();
			}
			else {
				new CreateNewWaterConsumptionEntryTask().execute();
			}
		}

		finish();
	}

	private class CreateNewWaterConsumptionEntryTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {

			final FitbitAdapter fitbitAdapter;
			try {
				fitbitAdapter = new FitbitAdapter(getApplicationContext());
			}
			catch (final FitbitAPIException e) {
				Log.e(getClass().getSimpleName(), "Unable to create fitbit adapter", e);
				return false;
			}
			
			boolean failure = false;
			
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
					Log.e(getClass().getSimpleName(), "Unable to fetch water comsumption from fitbit", e);
				}

				try {
					fitbitAdapter.getAndStoreWaterConsumptionGoal(getApplicationContext());
				}
				catch (final FitbitAPIException e) {
					failure = true;
					Log.e(getClass().getSimpleName(), "Unable to fetch water comsumption goal from fitbit", e);
				}
			}

			return !failure;
		}
		
		@Override
		protected void onPostExecute(Boolean success) {
			super.onPostExecute(success);
			if (success) {
				Toast.makeText(getApplicationContext(), R.string.add_water_success_toast, Toast.LENGTH_SHORT).show();
				WaterWidgetProvider.update(getApplicationContext(), null);
			}
			else {
				Toast.makeText(getApplicationContext(), R.string.add_water_failure_toast, Toast.LENGTH_SHORT).show();
			}
		}
	}

	public static class PlaceholderFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_add_water_consumption, container, false);
        }
    }
}
