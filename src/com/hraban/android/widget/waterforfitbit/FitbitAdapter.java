package com.hraban.android.widget.waterforfitbit;

import java.util.Locale;

import org.joda.time.LocalDate;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.Time;

import com.fitbit.api.FitbitAPIException;
import com.fitbit.api.FitbitApiError;
import com.fitbit.api.FitbitApiError.ErrorType;
import com.fitbit.api.client.FitbitAPIEntityCache;
import com.fitbit.api.client.FitbitApiClientAgent;
import com.fitbit.api.client.FitbitApiCredentialsCache;
import com.fitbit.api.client.FitbitApiCredentialsCacheMapImpl;
import com.fitbit.api.client.FitbitApiEntityCacheMapImpl;
import com.fitbit.api.client.FitbitApiSubscriptionStorage;
import com.fitbit.api.client.FitbitApiSubscriptionStorageInMemoryImpl;
import com.fitbit.api.client.LocalUserDetail;
import com.fitbit.api.client.service.FitbitAPIClientService;
import com.fitbit.api.common.model.foods.Water;
import com.fitbit.api.common.model.foods.WaterGoal;
import com.fitbit.api.common.model.units.UnitSystem;
import com.fitbit.api.model.APIResourceCredentials;
import com.fitbit.api.model.FitbitUser;
import com.hraban.android.widget.waterforfitbit.DataPreferences.Amount;

public class FitbitAdapter {
	
	private enum WaterUnit {
		IMPERIAL, METRIC
	}
	private static WaterUnit WATER_UNIT;
	
	private final FitbitAPIEntityCache entityCache = new FitbitApiEntityCacheMapImpl();
	private final FitbitApiCredentialsCache credentialsCache = new FitbitApiCredentialsCacheMapImpl();
	private final FitbitApiSubscriptionStorage subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();
	
	private final OAuthParameters oAuthParameters;
	private final FitbitAPIClientService<FitbitApiClientAgent> apiClientService;

	private LocalUserDetail localUserDetail;

	public static int getWaterResId(final int imperial, final int metric) {
		if (WaterUnit.METRIC.equals(WATER_UNIT)) {
			return metric;
		}
		else if (WaterUnit.IMPERIAL.equals(WATER_UNIT)) {
			return imperial;
		}
		else {
			return imperial;
		}
	}
	
	public static Locale getWaterLocale() {
		if (WaterUnit.METRIC.equals(WATER_UNIT)) {
			return Locale.UK;
		}
		else if (WaterUnit.IMPERIAL.equals(WATER_UNIT)) {
			return Locale.US;
		}
		else {
			return Locale.US;
		}
	}

	public static boolean isConnected(final Context context) {
		final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return networkInfo != null && networkInfo.isConnected();
	}

	public FitbitAdapter(final Context context) throws FitbitAPIException {

		oAuthParameters = new OAuthParameters(context);

		apiClientService = new FitbitAPIClientService<FitbitApiClientAgent>(
				new FitbitApiClientAgent("api.fitbit.com", "https://www.fitbit.com", credentialsCache),
				oAuthParameters.getConsumerKey(),
				oAuthParameters.getConsumerSecret(),
				credentialsCache,
				entityCache,
				subscriptionStore);
		
		initializeResourceCredentials(context);
	}

	public String getAuthorizationUrl() throws FitbitAPIException {
		return apiClientService.getResourceOwnerAuthorizationURL(new LocalUserDetail("-"), oAuthParameters.getCallbackUrl()) + "&display=touch";
	}
	
	public void completeAuthorization(final String oAuthTempToken, final String oAuthTempTokenVerifier) throws FitbitAPIException {
	    final APIResourceCredentials resourceCredentials = apiClientService.getResourceCredentialsByTempToken(oAuthTempToken);
	 
	    if (!resourceCredentials.isAuthorized()) {
	        resourceCredentials.setTempTokenVerifier(oAuthTempTokenVerifier);
	        // Make the request to get the token credentials
            apiClientService.getTokenCredentials(new LocalUserDetail(resourceCredentials.getLocalUserId()));

            oAuthParameters.setOAuthAccessToken(resourceCredentials.getAccessToken());
            oAuthParameters.setOAuthAccessTokenSecret(resourceCredentials.getAccessTokenSecret());
            oAuthParameters.setOAuthLocalUserId(resourceCredentials.getLocalUserId());
	    }
	}
	
	public void sendWaterConsumptionLog(final Context context) throws FitbitAPIException {
		if (!initializeResourceCredentials(context)) {
			throw new FitbitAPIException("No authorization credentials");
		}

		Time initialEntryTime = null;
		Amount amount;
		do {
			amount = DataPreferences.popAmountFromSendQueue(context);
			if (amount != null) {
				if (initialEntryTime == null) {
					initialEntryTime = amount.getDatetime();
				}
				else {
					// We have not had success and have looped around back to the intial entry - put it back and exit
					if (amount.getDatetime().format2445().equals(initialEntryTime.format2445())) {
						DataPreferences.pushAmountOnSendQueue(context, amount);
						break;
					}
				}

				try {
		    		apiClientService.getClient().setLocale(getWaterLocale());
		    		apiClientService.getClient().setLocalization(getWaterLocale());

					apiClientService.getClient().logWater(localUserDetail, amount.getAmount(), new LocalDate(amount.getDatetime().toMillis(false)));
				}
				catch (final FitbitAPIException e) {
					DataPreferences.pushAmountOnSendQueue(context, amount);

					checkForAuthenticationError(e);

					// TODO: Would be nice to differentiate between fatal for this entry
					// and fatal for all...
					throw e;
				}
			}
		} while (amount != null);
	}

	public void getAndStoreWaterConsumptionTotal(final Context context) throws FitbitAPIException {
		if (!initializeResourceCredentials(context)) {
			throw new FitbitAPIException("No authorization credentials");
		}

		try {
    		apiClientService.getClient().setLocale(getWaterLocale());
    		apiClientService.getClient().setLocalization(getWaterLocale());

			final Water loggedWater = apiClientService.getClient().getLoggedWater(
					localUserDetail,
					new FitbitUser(oAuthParameters.getOAuthLocalUserId()),
					new LocalDate());
			
			DataPreferences.setAmountLoggedToday(context, Double.valueOf(loggedWater.getSummary().getWater()).floatValue());
		}
		catch (final FitbitAPIException e) {
			checkForAuthenticationError(e);
		}

		BackgroundSyncService.notifySuccessfulSync(context);
	}

	public void getAndStoreWaterConsumptionGoal(final Context context) throws FitbitAPIException {
		if (!initializeResourceCredentials(context)) {
			throw new FitbitAPIException("No authorization credentials");
		}

		try {
    		apiClientService.getClient().setLocale(getWaterLocale());
    		apiClientService.getClient().setLocalization(getWaterLocale());

    		final WaterGoal waterGoal = apiClientService.getClient().getWaterGoal(
					localUserDetail,
					new FitbitUser(oAuthParameters.getOAuthLocalUserId()));

			DataPreferences.setGoal(context, Double.valueOf(waterGoal.getGoal()).floatValue());
		}
		catch (final FitbitAPIException e) {
			checkForAuthenticationError(e);
		}
	}

	private void checkForAuthenticationError(final FitbitAPIException e) throws FitbitAPIException {
		checkForAuthenticationError(e, true);
	}
	private void checkForAuthenticationError(final FitbitAPIException e, final boolean rethrow) throws FitbitAPIException {
		if (e.getApiErrors() != null) {
			for (FitbitApiError fitbitApiError : e.getApiErrors()) {
				if (ErrorType.Oauth.equals(fitbitApiError.getErrorType())) {
					oAuthParameters.resetAuthentication();
				}
			}
		}
		
		if (e.getCause() instanceof FitbitAPIException) {
			checkForAuthenticationError((FitbitAPIException) e.getCause(), false);
		}
		
		if (rethrow) {
			throw e;
		}
	}

	private boolean initializeResourceCredentials(final Context context) throws FitbitAPIException {
		if (oAuthParameters.isAuthorized()) {
			if (localUserDetail == null) {
				localUserDetail = new LocalUserDetail(oAuthParameters.getOAuthLocalUserId());
				
	        	APIResourceCredentials resourceCredentials = new APIResourceCredentials(oAuthParameters.getOAuthLocalUserId(), null, null);
	        	resourceCredentials.setAccessToken(oAuthParameters.getOAuthAccessToken());
	        	resourceCredentials.setAccessTokenSecret(oAuthParameters.getOAuthAccessTokenSecret());
	        	apiClientService.saveResourceCredentials(localUserDetail, resourceCredentials);

	        	try {
	        		UnitSystem waterUnitSystem = UnitSystem.findByDisplayLocale(apiClientService.getClient().getUserInfo(localUserDetail).getWaterUnit());
	        		switch (waterUnitSystem) {
	        		case METRIC:
	        		case UK:
			    		WATER_UNIT = WaterUnit.METRIC;
			    		break;
	        		case US:
        			default:
			    		WATER_UNIT = WaterUnit.IMPERIAL;
			    		break;
	        		}
	        	}
	        	catch (final FitbitAPIException e) {
	        		checkForAuthenticationError(e);
	        	}
			}
			return true;
		}
		return false;
	}
}
