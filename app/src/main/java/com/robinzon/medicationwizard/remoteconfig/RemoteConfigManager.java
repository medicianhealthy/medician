package com.robinzon.medicationwizard.remoteconfig;

import androidx.annotation.NonNull;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.TimeInterval;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RemoteConfigManager extends MedicationWizardSuper {

    private String LOG_REMOTE_CONFIG_VALUES;
    private final boolean mIsLoggingEnabled;
    public static WeakReference<RemoteConfigManager> sThisInstance;



    private Map<String, FirebaseRemoteConfigValue> mFirebaseValues;
    public static final float FETCH_INTERVAL_HOURS = 0.5F;
    public static final byte FETCH_TIMEOUT_SECONDS = 5;

    private RemoteConfigManager() {
        mIsLoggingEnabled = Logger.isLoggingEnabled();
        if (mIsLoggingEnabled) {
            LOG_REMOTE_CONFIG_VALUES = "remote_config";
        }
        final FirebaseRemoteConfigSettings firebaseRemoteConfigSettings = new FirebaseRemoteConfigSettings.
                Builder().
                setFetchTimeoutInSeconds(FETCH_TIMEOUT_SECONDS).
                setMinimumFetchIntervalInSeconds(getMinimumFetchIntervalInSeconds()).
                build();
        getFirebaseClient().setConfigSettingsAsync(firebaseRemoteConfigSettings);
    }

    private long getMinimumFetchIntervalInSeconds() {
        if (BuildConfig.DEBUG) {
            return FETCH_TIMEOUT_SECONDS;
        } else {
            return TimeInterval.Seconds.getFromHors(FETCH_INTERVAL_HOURS);
        }
    }

    private FirebaseRemoteConfig getFirebaseClient() {
        return FirebaseRemoteConfig.getInstance();
    }

    public static RemoteConfigManager getInstance() {
        if (null == sThisInstance || null == sThisInstance.get()) {
            sThisInstance = new WeakReference<>(new RemoteConfigManager());
        }
        return sThisInstance.get();
    }

    public void fetchConfiguration(final FireBaseFetchCallBack fireBaseFetchCallBack) {
        getFirebaseClient().fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                getFirebaseClient().getAll();
                if (!getFirebaseClient().getAll().isEmpty()) {
                    setFirebaseValues(getFirebaseClient().getAll());
                    if (mIsLoggingEnabled) {
                        logRemoteConfigValues();
                    }
                    if (null != fireBaseFetchCallBack) {
                        fireBaseFetchCallBack.onFetchCompleted(true);
                    }
                }
            } else {
                if (null != fireBaseFetchCallBack) {
                    fireBaseFetchCallBack.onFetchCompleted(false);
                }
            }
        });
    }

    private void logRemoteConfigValues() {
        final Map<String, FirebaseRemoteConfigValue> firebaseValues = getFirebaseValues();
        if (null != firebaseValues && !firebaseValues.isEmpty()) {
            for (Map.Entry<String, FirebaseRemoteConfigValue> entry : firebaseValues.entrySet()) {
                Logger.getInstance().log(getClassName(),
                        getRemoteConfigLogs(),
                        "[%s, %s]",
                        entry.getKey(),
                        entry.getValue().asString());
            }
        } else {
            Logger.getInstance().log(getClassName(),
                    getRemoteConfigLogs(),
                    "Remote config values are empty");
        }
    }

    private List<String> getRemoteConfigLogs() {
        if (mIsLoggingEnabled) {
            return new ArrayList<String>() {{
                add(null != LOG_REMOTE_CONFIG_VALUES ? LOG_REMOTE_CONFIG_VALUES : "null");
            }};
        }
        return null;
    }

    public int getIntValue(final @NonNull String key) {
        final Map<String, FirebaseRemoteConfigValue> firebaseValues = getFirebaseValues();
        if (null != firebaseValues && !firebaseValues.isEmpty()) {
            final FirebaseRemoteConfigValue remoteConfigValue = firebaseValues.get(key);
            if (null != remoteConfigValue) {
                try {
                    return (int) remoteConfigValue.asLong();
                } catch (IllegalArgumentException e) {
                    return getDefaultIntValue(key);
                }
            }
        }
        return getDefaultIntValue(key);
    }

    private int getDefaultIntValue(@NonNull String key) {
        final Object defaultValueFromMap = RemoteConfigKeysAndDefaults.VALUES.get(key);
        return null != defaultValueFromMap ? (int) defaultValueFromMap : Integer.MAX_VALUE;
    }


    public boolean getBooleanValue(final String key) {
        if (isFirBaseValueExist(key)) {
            try {
                return Objects.requireNonNull(getFirebaseValues().get(key)).asBoolean();
            } catch (IllegalArgumentException e) {
                return getDefaultBooleanValue(key);
            }
        }
        return getDefaultBooleanValue(key);
    }


    private boolean getDefaultBooleanValue(String key) {
        final Object defaultValueFromMap = RemoteConfigKeysAndDefaults.VALUES.get(key);
        return null != defaultValueFromMap && (boolean) defaultValueFromMap;
    }


    public String getStringValue(final String key) {
        if (null != getFirebaseValues() && !getFirebaseValues().isEmpty()) {
            final FirebaseRemoteConfigValue remoteConfigValue = getFirebaseValues().get(key);
            if (null != remoteConfigValue) {
                try {
                    return remoteConfigValue.asString();
                } catch (IllegalArgumentException e) {
                    return (String) RemoteConfigKeysAndDefaults.VALUES.get(key);
                }
            }
        }
        return (String) RemoteConfigKeysAndDefaults.VALUES.get(key);
    }


    boolean isFirBaseValueExist(final String key) {
        if (null != getFirebaseValues() && !getFirebaseValues().isEmpty()) {
            final FirebaseRemoteConfigValue remoteConfigValue = getFirebaseValues().get(key);
            return null != remoteConfigValue;
        }
        return false;
    }

    private Map<String, FirebaseRemoteConfigValue> getFirebaseValues() {
        return mFirebaseValues;
    }

    private void setFirebaseValues(Map<String, FirebaseRemoteConfigValue> firebaseValues) {
        mFirebaseValues = firebaseValues;
    }


    @Override
    public String getClassName() {
        return RemoteConfigManager.class.getCanonicalName();
    }
}
