package com.robinzon.medicationwizard.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * A robust singleton manager for handling all Android SharedPreferences operations.
 * <p>
 * This class provides a high-level API for persistent data storage, abstracting
 * the complexities of the {@link SharedPreferences.Editor} and handling data
 * types such as JSON Arrays, Booleans, and Strings.
 * </p>
 */
public class SharedPreferencesManager {

    private static SharedPreferencesManager sManagerInstance;
    private SharedPreferences mAndroidSharedPreferences;

    private SharedPreferencesManager(@NonNull final Context context) {
        final String fileName = getFileName(context);
        if (!TextUtils.isEmpty(fileName)) {
            mAndroidSharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        } else {
            if (Logger.IS_LOGGING_ENABLED) {
                Logger.log(Logger.SHARED_PREFS,
                        "File name of shared preferences is invalid. Could not create instance");
            }
        }
    }

    /**
     * Retrieves the singleton instance of the manager.
     *
     * @param context The application context.
     * @return The active SharedPreferencesManager instance.
     */
    public static synchronized SharedPreferencesManager getInstance(@NonNull final Context context) {
        if (null == sManagerInstance) {
            sManagerInstance = new SharedPreferencesManager(context.getApplicationContext());
        }
        return sManagerInstance;
    }

    /**
     * Generates a unique file name for the preferences based on the package name.
     */
    @Nullable
    private static String getFileName(final Context context) {
        if (null != context) {
            final String packageName = context.getPackageName();
            if (!TextUtils.isEmpty(packageName)) {
                return packageName.concat(".sharedpreferences");
            }
        }
        return null;
    }


    /**
     * Permanently removes a key and its value from storage.
     *
     * @param key The key to remove.
     */
    public void removeKey(String key) {
        final SharedPreferences.Editor editor = getEditor();
        if (null != editor) {
            editor.remove(key).apply();
        }
    }

    @Nullable
    private SharedPreferences.Editor getEditor() {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.edit();
        }
        return null;
    }

    /**
     * Checks if a specific key exists in the preferences.
     *
     * @param key The key to check.
     * @return True if the key exists.
     */
    public boolean containsKey(String key) {
        if (null != mAndroidSharedPreferences && !TextUtils.isEmpty(key)) {
            return mAndroidSharedPreferences.contains(key);
        }
        return false;
    }

    /**
     * Serializes and saves a {@link JSONArray} as a String.
     *
     * @param key       The storage key.
     * @param jsonArray The data to save.
     */
    public void setJsonArray(@Nullable final String key, @NonNull final JSONArray jsonArray) {
        final SharedPreferences.Editor editor = getEditor();
        if (null != editor && !TextUtils.isEmpty(key)) {
            editor.putString(key, jsonArray.toString()).apply();
        }
    }

    /**
     * Retrieves and parses a {@link JSONArray} from storage.
     *
     * @param key          The storage key.
     * @param defaultValue The value to return if the key is missing or invalid.
     * @return The parsed JSONArray or the default value.
     */
    @Nullable
    public JSONArray getJsonArray(@Nullable final String key, @Nullable final JSONArray defaultValue) {
        if (null != mAndroidSharedPreferences) {
            try {
                String jsonString = mAndroidSharedPreferences.getString(key, null);
                return jsonString == null ? defaultValue : new JSONArray(jsonString);
            } catch (JSONException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public int getInt(@NonNull final String key, final int defaultValue) {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.getInt(key, defaultValue);
        }
        return defaultValue;
    }

    public void setInt(@NonNull final String key, final int value) {
        if (null != getEditor()) {
            getEditor().putInt(key, value).apply();
        }
    }

    public long getLong(final String key, final long defaultValue) {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.getLong(key, defaultValue);
        }
        return defaultValue;
    }

    public void setLong(@NonNull final String key, final long value) {
        if (null != getEditor()) {
            getEditor().putLong(key, value).apply();
        }
    }

    public float getFloat(final String key, final float defaultValue) {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.getFloat(key, defaultValue);
        }
        return defaultValue;
    }

    public void setFloat(@NonNull final String key, final float value) {
        if (null != getEditor()) {
            getEditor().putFloat(key, value).apply();
        }
    }

    public String getString(final String key, final String defaultValue) {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.getString(key, defaultValue);
        }
        return defaultValue;
    }

    public void setString(@NonNull final String key, @NonNull final String value) {
        if (null != getEditor()) {
            getEditor().putString(key, value).apply();
        }
    }

    public boolean getBoolean(String key, Boolean defaultValue) {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.getBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    public void setBoolean(@NonNull final String key, final boolean value) {
        if (null != getEditor()) {
            getEditor().putBoolean(key, value).apply();
        }
    }

    /**
     * Registers a listener for preference changes.
     * Used by ViewModels to trigger UI refreshes when data is modified.
     *
     * @param listener The listener to register.
     */
    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (mAndroidSharedPreferences != null) {
            mAndroidSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    /**
     * Unregisters a previously registered listener.
     *
     * @param listener The listener to remove.
     */
    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (mAndroidSharedPreferences != null) {
            mAndroidSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    /**
     * @return The low-level Android SharedPreferences instance.
     */
    @Nullable
    public SharedPreferences getAndroidSharedPreferencesInstance() {
        return mAndroidSharedPreferences;
    }
}