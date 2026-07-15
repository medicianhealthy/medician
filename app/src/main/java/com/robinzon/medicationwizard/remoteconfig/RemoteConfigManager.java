package com.robinzon.medicationwizard.remoteconfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.MedicationWizardApplication;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.utils.TimeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoteConfigManager {

    public static final float FETCH_INTERVAL_HOURS = 0.5F;
    public static final byte FETCH_TIMEOUT_SECONDS = 5;
    private static final String PREF_PREFIX = "rc_cache_";
    private static RemoteConfigManager sRemoteConfigManagerInstance;
    private String LOG_REMOTE_CONFIG_VALUES;
    private Map<String, FirebaseRemoteConfigValue> mRemoteConfigValues;

    private RemoteConfigManager() {
        if (Logger.IS_LOGGING_ENABLED) {
            LOG_REMOTE_CONFIG_VALUES = "remote_config";
        }
        final FirebaseRemoteConfigSettings firebaseRemoteConfigSettings = new FirebaseRemoteConfigSettings.
                Builder().
                setFetchTimeoutInSeconds(FETCH_TIMEOUT_SECONDS).
                setMinimumFetchIntervalInSeconds(getMinimumFetchIntervalInSeconds()).
                build();
        getFirebaseClient().setConfigSettingsAsync(firebaseRemoteConfigSettings);
    }

    @NonNull
    public static synchronized RemoteConfigManager getInstance() {
        if (null == sRemoteConfigManagerInstance) {
            sRemoteConfigManagerInstance = new RemoteConfigManager();
        }
        return sRemoteConfigManagerInstance;
    }

    private long getMinimumFetchIntervalInSeconds() {
        if (BuildConfig.DEBUG) {
            return FETCH_TIMEOUT_SECONDS;
        } else {
            return TimeManager.getInstance().toSecondsFromHours(FETCH_INTERVAL_HOURS);
        }
    }

    @NonNull
    private FirebaseRemoteConfig getFirebaseClient() {
        return FirebaseRemoteConfig.getInstance();
    }

    public void fetchConfiguration(@NonNull final FireBaseFetchCallBack fetchCallbackListener) {
        getFirebaseClient().fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                setFirebaseValues(getFirebaseClient().getAll());
                cacheValuesToPrefs(mRemoteConfigValues);
                if (Logger.IS_LOGGING_ENABLED) {
                    logRemoteConfigValues();
                }
            }
            fetchCallbackListener.onFetchCompleted(task.isSuccessful());
        });
    }

    private void cacheValuesToPrefs(Map<String, FirebaseRemoteConfigValue> remoteValuesMap) {
        if (remoteValuesMap == null) return;
        SharedPreferencesManager sharedPreferencesManager = SharedPreferencesManager.getInstance(MedicationWizardApplication.getContext());
        for (Map.Entry<String, FirebaseRemoteConfigValue> entry : remoteValuesMap.entrySet()) {
            String configKey = entry.getKey();
            FirebaseRemoteConfigValue remoteValue = entry.getValue();
            // We store everything as strings in the cache for simplicity
            sharedPreferencesManager.setString(PREF_PREFIX + configKey, remoteValue.asString());
        }
    }

    private void logRemoteConfigValues() {
        final Map<String, FirebaseRemoteConfigValue> remoteConfigValues = getFirebaseValues();
        if (null != remoteConfigValues && !remoteConfigValues.isEmpty()) {
            for (Map.Entry<String, FirebaseRemoteConfigValue> entry : remoteConfigValues.entrySet()) {
                if (Logger.IS_LOGGING_ENABLED) {
                    Logger.log(Logger.REMOTE_CONFIG,
                            "[%s, %s]",
                            entry.getKey(),
                            entry.getValue().asString());
                }
            }
        } else {
            if (Logger.IS_LOGGING_ENABLED) {
                Logger.log(Logger.REMOTE_CONFIG,
                        "Remote config values are empty");
            }
        }
    }

    @Nullable
    private List<String> getRemoteConfigLogs() {
        if (Logger.IS_LOGGING_ENABLED) {
            return new ArrayList<>() {{
                add(null != LOG_REMOTE_CONFIG_VALUES ? LOG_REMOTE_CONFIG_VALUES : "null");
            }};
        }
        return null;
    }

    // --- Core 3-Tier Getters ---

    public int getIntValue(final @NonNull String configKey) {
        // 1. Fresh Memory Cache
        final Map<String, FirebaseRemoteConfigValue> remoteConfigValues = getFirebaseValues();
        if (remoteConfigValues != null && remoteConfigValues.containsKey(configKey)) {
            FirebaseRemoteConfigValue remoteValue = remoteConfigValues.get(configKey);
            // Only use if the value actually came from a Remote source (Server or Cache)
            if (remoteValue != null && remoteValue.getSource() != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
                try {
                    String rawStringValue = remoteValue.asString();
                    return Integer.parseInt(rawStringValue);
                } catch (Exception ignored) {
                }
            }
        }

        // 2. Persistent Prefs Cache
        SharedPreferencesManager sharedPreferencesManager = SharedPreferencesManager.getInstance(MedicationWizardApplication.getContext());
        String cachedValue = sharedPreferencesManager.getString(PREF_PREFIX + configKey, null);
        if (cachedValue != null) {
            try {
                return Integer.parseInt(cachedValue);
            } catch (Exception ignored) {
            }
        }

        // 3. Static Defaults
        return getDefaultIntValue(configKey);
    }

    private int getDefaultIntValue(@NonNull String configKey) {
        final Object defaultValueFromMap = RemoteConfigKeysAndDefaults.VALUES.get(configKey);
        return null != defaultValueFromMap ? (int) defaultValueFromMap : 0;
    }

    public boolean getBooleanValue(final @NonNull String configKey) {
        // 1. Fresh Memory Cache
        final Map<String, FirebaseRemoteConfigValue> remoteConfigValues = getFirebaseValues();
        if (remoteConfigValues != null && remoteConfigValues.containsKey(configKey)) {
            FirebaseRemoteConfigValue remoteValue = remoteConfigValues.get(configKey);
            if (remoteValue != null && remoteValue.getSource() != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
                try {
                    return remoteValue.asBoolean();
                } catch (Exception ignored) {
                }
            }
        }

        // 2. Persistent Prefs Cache
        SharedPreferencesManager sharedPreferencesManager = SharedPreferencesManager.getInstance(MedicationWizardApplication.getContext());
        String cachedValue = sharedPreferencesManager.getString(PREF_PREFIX + configKey, null);
        if (cachedValue != null) {
            return Boolean.parseBoolean(cachedValue);
        }

        // 3. Static Defaults
        return getDefaultBooleanValue(configKey);
    }

    private boolean getDefaultBooleanValue(String configKey) {
        final Object defaultValueFromMap = RemoteConfigKeysAndDefaults.VALUES.get(configKey);
        return null != defaultValueFromMap && (boolean) defaultValueFromMap;
    }

    @NonNull
    public String getStringValue(final @NonNull String configKey) {
        // 1. Fresh Memory Cache
        final Map<String, FirebaseRemoteConfigValue> remoteConfigValues = getFirebaseValues();
        if (remoteConfigValues != null && remoteConfigValues.containsKey(configKey)) {
            FirebaseRemoteConfigValue remoteValue = remoteConfigValues.get(configKey);
            if (remoteValue != null && remoteValue.getSource() != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
                return remoteValue.asString();
            }
        }

        // 2. Persistent Prefs Cache
        SharedPreferencesManager sharedPreferencesManager = SharedPreferencesManager.getInstance(MedicationWizardApplication.getContext());
        String cachedValue = sharedPreferencesManager.getString(PREF_PREFIX + configKey, null);
        if (cachedValue != null) {
            return cachedValue;
        }

        // 3. Static Defaults
        Object defaultValue = RemoteConfigKeysAndDefaults.VALUES.get(configKey);
        return defaultValue != null ? defaultValue.toString() : "";
    }

    public boolean shouldShowAppOpen() {
        return getBooleanValue(RemoteConfigKeysAndDefaults.AD_SHOULD_SHOW_APP_OPEN);
    }

    public double getDoubleValue(final @NonNull String configKey) {
        final Map<String, FirebaseRemoteConfigValue> remoteConfigValues = getFirebaseValues();
        if (remoteConfigValues != null && remoteConfigValues.containsKey(configKey)) {
            FirebaseRemoteConfigValue remoteValue = remoteConfigValues.get(configKey);
            if (remoteValue != null && remoteValue.getSource() != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
                return remoteValue.asDouble();
            }
        }
        Object defaultValue = RemoteConfigKeysAndDefaults.VALUES.get(configKey);
        return defaultValue instanceof Double ? (Double) defaultValue : 0.0;
    }

    // --- Semantic Getters ---

    /**
     * @return The minimum number of application sessions required before showing interstitials.
     */
    public int getMinSessionsForInterstitial() {
        return getIntValue(RemoteConfigKeysAndDefaults.MIN_SESSIONS_INTERSTITIAL);
    }

    /**
     * @return The minimum number of application sessions required before showing App Open ads.
     */
    public int getMinSessionsAppOpen() {
        return getIntValue(RemoteConfigKeysAndDefaults.MIN_SESSIONS_APP_OPEN);
    }

    /**
     * @return Minimum app usage time (minutes) required for App Open ad eligibility.
     */
    public int getMinAppTimeAppOpenMins() {
        return getIntValue(RemoteConfigKeysAndDefaults.MIN_APP_TIME_APP_OPEN_MINS);
    }

    /**
     * @return Minimum app usage time (minutes) required for Interstitial ad eligibility.
     */
    public int getMinAppTimeForInterstitialMins() {
        return getIntValue(RemoteConfigKeysAndDefaults.MIN_APP_TIME_INTERSTITIAL_MINS);
    }

    /**
     * @return Minimum app usage time (minutes) required for Banner ad visibility.
     */
    public int getMinAppTimeForBannerMins() {
        return getIntValue(RemoteConfigKeysAndDefaults.MIN_APP_TIME_BANNER_MINS);
    }

    /**
     * @return Duration in hours for a temporary Magic Pass.
     */
    public int getMagicPassDurationHours() {
        return getIntValue(RemoteConfigKeysAndDefaults.MAGIC_PASS_DURATION_HOURS);
    }

    /**
     * @return Number of user actions required between Interstitial ads.
     */
    public int getActionsPerInterstitial() {
        return getIntValue(RemoteConfigKeysAndDefaults.ACTIONS_PER_INTERSTITIAL);
    }

    /**
     * @return Maximum number of days to retain medication history in the database.
     */
    public int getHistoryRetentionDays() {
        return getIntValue(RemoteConfigKeysAndDefaults.HISTORY_RETENTION_DAYS);
    }

    /**
     * @return Minutes before scheduled time when a "Take" is considered early.
     */
    public int getEarlyTakeThresholdMins() {
        return getIntValue(RemoteConfigKeysAndDefaults.EARLY_TAKE_THRESHOLD_MINS);
    }

    /**
     * @return Minutes after scheduled time when a "Take" is considered late.
     */
    public int getLateTakeThresholdMins() {
        return getIntValue(RemoteConfigKeysAndDefaults.LATE_TAKE_THRESHOLD_MINS);
    }

    public int getNumOfMedsToShowRv() {
        return getIntValue(RemoteConfigKeysAndDefaults.NUM_OF_MEDS_TO_SHOW_RV);
    }

    public int getAdInterstitialCoolDownSeconds() {
        return getIntValue(RemoteConfigKeysAndDefaults.AD_INTERSTITIAL_COOL_DOWN_SECONDS);
    }

    // --- Internal Helpers ---

    boolean isFirBaseValueExist(final String configKey) {
        final Map<String, FirebaseRemoteConfigValue> remoteConfigValues = getFirebaseValues();
        return remoteConfigValues != null && remoteConfigValues.containsKey(configKey);
    }

    @Nullable
    private Map<String, FirebaseRemoteConfigValue> getFirebaseValues() {
        return mRemoteConfigValues;
    }

    private void setFirebaseValues(@NonNull Map<String, FirebaseRemoteConfigValue> remoteConfigValues) {
        mRemoteConfigValues = remoteConfigValues;
    }

}

