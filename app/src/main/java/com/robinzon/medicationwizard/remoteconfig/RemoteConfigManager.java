package com.robinzon.medicationwizard.remoteconfig;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import com.robinzon.medicationwizard.ads.rootclasses.ISuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.TimeInterval;
import com.robinzon.medicationwizard.utils.Validator;

import java.lang.ref.WeakReference;
import java.util.Map;

public class RemoteConfigManager implements ISuper {

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
                setFetchTimeoutInSeconds((long)FETCH_TIMEOUT_SECONDS).
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
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if (task.isSuccessful()) {
                    if (Validator.isValidMap(mFirebaseRemoteConfig.getAll())) {
                        mFirebaseValues = mFirebaseRemoteConfig.getAll();
                        if (Logger.isLoggingEnabled()){
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

            }
        });
    }

    private void logRemoteConfigValues() {
        if (Validator.isValidMap(mFirebaseValues)){
            for (Map.Entry<String, FirebaseRemoteConfigValue> entry : mFirebaseValues.entrySet()) {
                Logger.logSingleTag(getClassName(),
                        LOG_REMOTE_CONFIG_VALUES,
                        "[%s, %s]",
                        entry.getKey(),
                        entry.getValue().asString());
            }
        } else {
            Logger.logSingleTag(getClassName(),
                    LOG_REMOTE_CONFIG_VALUES ,
                    "Remote config values are empty");
        }
    }

    public int getIntValue(final @NonNull String key) {
        if (Validator.isValidMap(mFirebaseValues)) {
            final FirebaseRemoteConfigValue remoteConfigValue = mFirebaseValues.get(key);
            if (Validator.isValidObject(remoteConfigValue)) {
                try {
                    return (int) remoteConfigValue.asLong();
                } catch (IllegalArgumentException e) {
                    return (int) RemoteConfigKeysAndDefaults.VALUES.get(key);
                }
            }
        }
        return (int) RemoteConfigKeysAndDefaults.VALUES.get(key);
    }



    public boolean getBooleanValue(final String key) {
        if (Validator.isValidMap(mFirebaseValues)) {
            final FirebaseRemoteConfigValue remoteConfigValue = mFirebaseValues.get(key);
            if (Validator.isValidObject(remoteConfigValue)) {
                try {
                    return remoteConfigValue.asBoolean();
                } catch (IllegalArgumentException e) {
                    return (boolean) RemoteConfigKeysAndDefaults.VALUES.get(key);
                }
            }
        }
        return (boolean) RemoteConfigKeysAndDefaults.VALUES.get(key);
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
