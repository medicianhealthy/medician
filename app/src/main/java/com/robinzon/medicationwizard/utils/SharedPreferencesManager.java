package com.robinzon.medicationwizard.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class SharedPreferencesManager {

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
            if (Logger.IS_LOGGING_ENABLED) {
                Logger.log(Logger.SHARED_PREFS,
                        "File name of shared preferences is invalid. Could not create instance");
            }
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

    @Nullable private SharedPreferences.Editor getEditor() {
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
        final SharedPreferences.Editor editor = getEditor();
        if (null != editor && !TextUtils.isEmpty(key)) {
            editor.putString(key, jsonArray.toString()).apply();
        }
    }

    @Nullable public JSONArray getJsonArray(@Nullable final String key, @Nullable final JSONArray defaultValue) {
        if (null != getAndroidSharedPreferencesInstance()) {
            try {
                String jsonString = getAndroidSharedPreferencesInstance().getString(key, null);
                return jsonString == null ? defaultValue : new JSONArray(jsonString);
            } catch (JSONException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @SuppressWarnings("unused")
    public int getInt(@NonNull final String key, final int defaultValue) {
        if (null != getAndroidSharedPreferencesInstance()) {
            return getAndroidSharedPreferencesInstance().getInt(key, defaultValue);
        }
        return defaultValue;
    }

    public void setInt (@NonNull final String key, final int value){
        if (null != getEditor()){
            getEditor().putInt(key, value).apply();
        }
    }

    public long getLong(final String key, final long defaultValue) {
        if (null != getAndroidSharedPreferencesInstance()) {
            return getAndroidSharedPreferencesInstance().getLong(key, defaultValue);
        }
        return defaultValue;
    }

    public void setLong (@NonNull final String key, final long value){
        if (null != getEditor()){
            getEditor().putLong(key, value).apply();
        }
    }

    @SuppressWarnings("unused")
    public float getFloat(final String key, final float defaultValue) {
        if (null != getAndroidSharedPreferencesInstance()) {
            return getAndroidSharedPreferencesInstance().getFloat(key, defaultValue);
        }
        return defaultValue;
    }

    public void setFloat (@NonNull final String key, final float value){
        if (null != getEditor()){
            getEditor().putFloat(key, value).apply();
        }
    }
    @SuppressWarnings("unused")
    public String getString(final String key, final String defaultValue) {
        if (null != getAndroidSharedPreferencesInstance()) {
            return getAndroidSharedPreferencesInstance().getString(key, defaultValue);
        }
        return defaultValue;
    }

    public void setString (@NonNull final String key, @NonNull final String value){
        if (null != getEditor()){
            getEditor().putString(key, value).apply();
        }
    }



    @SuppressWarnings("unused")
    public boolean getBoolean(String key, Boolean defaultValue) {
        if (null != getAndroidSharedPreferencesInstance()) {
            return getAndroidSharedPreferencesInstance().getBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    public void setBoolean (@NonNull final String key, final boolean value){
        if (null != getEditor()){
            getEditor().putBoolean(key, value).apply();
        }
    }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (getAndroidSharedPreferencesInstance() != null) {
            getAndroidSharedPreferencesInstance().registerOnSharedPreferenceChangeListener(listener);
        }
    }

    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (getAndroidSharedPreferencesInstance() != null) {
            getAndroidSharedPreferencesInstance().unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    @Nullable public SharedPreferences getAndroidSharedPreferencesInstance() {
        return mAndroidSharedPreferencesInstance != null ? mAndroidSharedPreferencesInstance.get() : null;
    }
}
