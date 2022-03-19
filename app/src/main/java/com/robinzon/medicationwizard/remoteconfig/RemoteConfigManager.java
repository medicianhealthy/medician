package com.robinzon.medicationwizard.remoteconfig;

import androidx.annotation.NonNull;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.TimeInterval;
import com.robinzon.medicationwizard.utils.Validator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoteConfigManager extends MedicationWizardSuper {

    private static final String LOG_REMOTE_CONFIG_VALUES = "remote_config";
    public static WeakReference<RemoteConfigManager> sInstance;
    private final FirebaseRemoteConfig mFirebaseRemoteConfig;
    private Map<String, FirebaseRemoteConfigValue> mFirebaseValues;
    public static final float FETCH_INTERVAL_HOURS = 12;
    public static final byte FETCH_TIMEOUT_SECONDS = 3;

    private RemoteConfigManager() {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        final FirebaseRemoteConfigSettings firebaseRemoteConfigSettings = new FirebaseRemoteConfigSettings.
                Builder().
                setFetchTimeoutInSeconds(FETCH_TIMEOUT_SECONDS).
                setMinimumFetchIntervalInSeconds(TimeInterval.Seconds.getFromHors(FETCH_INTERVAL_HOURS)).
                build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(firebaseRemoteConfigSettings);
    }

    public static RemoteConfigManager getInstance() {
        if (null == sInstance || null == sInstance.get()) {
            sInstance = new WeakReference<>(new RemoteConfigManager());
        }
        return sInstance.get();
    }

    public void fetchConfiguration(final FireBaseFetchCallBack fireBaseFetchCallBack) {
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (Validator.isValidMap(mFirebaseRemoteConfig.getAll())) {
                    mFirebaseValues = mFirebaseRemoteConfig.getAll();
                    if (Logger.getInstance().isLoggingEnabled()) {
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
        if (Validator.isValidMap(mFirebaseValues)) {
            for (Map.Entry<String, FirebaseRemoteConfigValue> entry : mFirebaseValues.entrySet()) {
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
        return new ArrayList<String>() {{
            add(LOG_REMOTE_CONFIG_VALUES);
        }};
    }

    public int getIntValue(final @NonNull String key) {

        if (Validator.isValidMap(mFirebaseValues)) {
            final FirebaseRemoteConfigValue remoteConfigValue = mFirebaseValues.get(key);
            if (Validator.isValidObject(remoteConfigValue)) {
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
        if (Validator.isValidMap(mFirebaseValues)) {
            final FirebaseRemoteConfigValue remoteConfigValue = mFirebaseValues.get(key);
            if (Validator.isValidObject(remoteConfigValue)) {
                try {
                    return remoteConfigValue.asBoolean();
                } catch (IllegalArgumentException e) {
                    return getDefaultBooleanValue(key);
                }
            }
        }
        return getDefaultBooleanValue(key);
    }

    private boolean getDefaultBooleanValue(String key) {
        final Object defaultValueFromMap = RemoteConfigKeysAndDefaults.VALUES.get(key);
        return null != defaultValueFromMap && (boolean) defaultValueFromMap;
    }


    public String getStringValue(final String key) {
        if (Validator.isValidMap(mFirebaseValues)) {
            final FirebaseRemoteConfigValue remoteConfigValue = mFirebaseValues.get(key);
            if (Validator.isValidObject(remoteConfigValue)) {
                try {
                    return remoteConfigValue.asString();
                } catch (IllegalArgumentException e) {
                    return (String) RemoteConfigKeysAndDefaults.VALUES.get(key);
                }
            }
        }
        return (String) RemoteConfigKeysAndDefaults.VALUES.get(key);
    }


    @Override
    public String getClassName() {
        return "{RemoteConfigManager}";
    }
}
