package com.robinzon.medicationwizard.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class SharedPreferencesManager extends MedicationWizardSuper {

    private WeakReference<String> SHARED_PREFERENCES;
    private WeakReference<SharedPreferences> mAndroidSharedPreferencesInstance;
    private static WeakReference<SharedPreferencesManager> sManagerInstance;
    private WeakReference<ArrayList<String>> LOG_TAGS;

    public static SharedPreferencesManager getInstance(@NonNull final Context context) {
        if (null == sManagerInstance || null == sManagerInstance.get()) {
            sManagerInstance = new WeakReference<>(new SharedPreferencesManager(context));
        } else if (null == sManagerInstance.get().getAndroidSharedPreferencesInstance()) {
            final String fileName = getFileName(context);
            if (!TextUtils.isEmpty(fileName)) {
                final SharedPreferences sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
                sManagerInstance.get().mAndroidSharedPreferencesInstance = new WeakReference<>(sharedPreferences);
            }
        }
        return sManagerInstance.get();
    }

    private SharedPreferencesManager(@NonNull final Context context) {
        final String fileName = getFileName(context);
        if (!TextUtils.isEmpty(fileName)) {
            final SharedPreferences sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
            mAndroidSharedPreferencesInstance = new WeakReference<>(sharedPreferences);
        } else {
            Logger.getInstance().log(getClassName(), getSharedPreferencesLogs(),
                    "File name of shared preferences is invalid. Could not create instance");
        }
    }

    @Nullable private static String getFileName(final Context context) {
        if (null != context) {
            final Context applicationContext = context.getApplicationContext();
            if (null != applicationContext) {
                final String packageName = applicationContext.getPackageName();
                if (!TextUtils.isEmpty(packageName)) {
                    return packageName.concat(".sharedpreferences");
                }
            }
        }
        return null;
    }


    @SuppressWarnings("unused")
    public void removeKey(String key) {
        final SharedPreferences.Editor editor = getEditor();
        if (null != editor) {
            editor.remove(key).apply();
        }
    }

    private SharedPreferences.Editor getEditor() {
        if (null != getAndroidSharedPreferencesInstance()) {
            return getAndroidSharedPreferencesInstance().edit();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public boolean containsKey(String key) {
        if (null != getAndroidSharedPreferencesInstance() && !TextUtils.isEmpty(key)) {
            return getAndroidSharedPreferencesInstance().contains(key);
        }
        return false;
    }

    public void setValue(final String key, final Object value) {
        if (TextUtils.isEmpty(key) ||
                null == value ||
                (value instanceof String && TextUtils.isEmpty((String) value))) {
            Logger.getInstance().log(getClassName(), getSharedPreferencesLogs(),
                    "Trying to set value but key or value is invalid");
            return;
        }
        final SharedPreferences.Editor editor = getEditor();
        if (null == editor) {
            Logger.getInstance().log(getClassName(), getSharedPreferencesLogs(),
                    "Trying to set value but editor object is null");
            return;
        }
        if (value instanceof Integer) {
            editor.putInt(key, (Integer) value).apply();
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value).apply();
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value).apply();
        } else if (value instanceof String) {
            editor.putString(key, (String) value).apply();
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value).apply();
        }
    }

    @NonNull private ArrayList<String> getSharedPreferencesLogs() {
        if (null == LOG_TAGS || null == LOG_TAGS.get()) {
            LOG_TAGS = new WeakReference<>(new ArrayList<>() {{
                if (null == SHARED_PREFERENCES.get()){
                    SHARED_PREFERENCES = new WeakReference<>("shared_prefernces");
                }
                add(SHARED_PREFERENCES.get());
            }});
        }
        return LOG_TAGS.get();
    }

    @SuppressWarnings("unused")
    public void setJsonArray(@Nullable final String key, @NonNull final JSONArray jsonArray) {
        if (0 != jsonArray.length()) {
            final SharedPreferences.Editor editor = getEditor();
            if (null != editor && !TextUtils.isEmpty(key)) {
                editor.putString(key, jsonArray.toString()).apply();
            }
        }
    }

    @SuppressWarnings("unused")
    public int getInt(@NonNull final String key, final int defaultValue) {
        if (null != getAndroidSharedPreferencesInstance()) {
            return getAndroidSharedPreferencesInstance().getInt(key, defaultValue);
        }
        return defaultValue;
    }

    public long getLong(final String key, final long defaultValue) {
        if (null != getAndroidSharedPreferencesInstance()) {
            return getAndroidSharedPreferencesInstance().getLong(key, defaultValue);
        }
        return defaultValue;
    }
    @SuppressWarnings("unused")
    public float getFloat(final String key, final float defaultValue) {
        if (null != getAndroidSharedPreferencesInstance()) {
            return getAndroidSharedPreferencesInstance().getFloat(key, defaultValue);
        }
        return defaultValue;
    }
    @SuppressWarnings("unused")
    public String getString(final String key, final String defaultValue) {
        if (null != getAndroidSharedPreferencesInstance()) {
            return getAndroidSharedPreferencesInstance().getString(key, defaultValue);
        }
        return defaultValue;
    }


    @Override
    public String getClassName() {
        return "{SharedPreferencesManager}";
    }

    @SuppressWarnings("unused")
    public boolean getBoolean(String key, Boolean defaultValue) {
        if (null != getAndroidSharedPreferencesInstance()) {
            return getAndroidSharedPreferencesInstance().getBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    @Nullable private SharedPreferences getAndroidSharedPreferencesInstance() {
        return mAndroidSharedPreferencesInstance.get();
    }
}
