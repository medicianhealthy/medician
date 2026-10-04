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
 * types such as JSON Arrays, Booleans, Strings, and primitive numbers.
 * </p>
 */
public class SharedPreferencesManager {

    private static SharedPreferencesManager sManagerInstance;
    private SharedPreferences mAndroidSharedPreferences;

    /**
     * Initializes the manager with the specified context.
     *
     * @param context The application context. Must not be null.
     */
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
     * Retrieves the singleton instance of the SharedPreferencesManager.
     *
     * @param context The application context. Must not be null.
     * @return The active SharedPreferencesManager instance.
     */
    public static synchronized SharedPreferencesManager getInstance(@NonNull final Context context) {
        if (null == sManagerInstance) {
            sManagerInstance = new SharedPreferencesManager(context.getApplicationContext());
        }
        return sManagerInstance;
    }

    /**
     * Generates a unique file name for the preferences based on the application package name.
     *
     * @param context The application context.
     * @return The generated file name, or null if the context or package name is unavailable.
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
     * Permanently removes a specified key and its associated value from storage.
     *
     * @param key The key to remove.
     */
    public void removeKey(String key) {
        final SharedPreferences.Editor editor = getEditor();
        if (null != editor) {
            editor.remove(key).apply();
        }
    }

    /**
     * Retrieves the SharedPreferences.Editor instance for data modification.
     *
     * @return The Editor instance, or null if SharedPreferences initialization failed.
     */
    @Nullable
    private SharedPreferences.Editor getEditor() {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.edit();
        }
        return null;
    }

    /**
     * Checks if a specific key exists within the preferences storage.
     *
     * @param key The key to verify.
     * @return True if the key exists, false otherwise.
     */
    public boolean containsKey(String key) {
        if (null != mAndroidSharedPreferences && !TextUtils.isEmpty(key)) {
            return mAndroidSharedPreferences.contains(key);
        }
        return false;
    }

    /**
     * Serializes and saves a {@link JSONArray} as a String under the specified key.
     * <p>
     * If the provided JSON array is null, the corresponding key will be removed from storage.
     * </p>
     *
     * @param key       The storage key.
     * @param jsonArray The JSON array to save, or null to remove the key.
     */
    public void setJsonArray(@Nullable final String key, @Nullable final JSONArray jsonArray) {
        final SharedPreferences.Editor editor = getEditor();
        if (null != editor && !TextUtils.isEmpty(key)) {
            if (jsonArray == null) {
                editor.remove(key).apply();
            } else {
                editor.putString(key, jsonArray.toString()).apply();
            }
        }
    }

    /**
     * Retrieves and parses a {@link JSONArray} from storage using the specified key.
     *
     * @param key          The storage key.
     * @param defaultValue The fallback value to return if the key is missing or parsing fails.
     * @return The parsed JSONArray, or the default value.
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

    /**
     * Retrieves an integer value from storage.
     *
     * @param key          The storage key. Must not be null.
     * @param defaultValue The fallback value to return if the key is missing.
     * @return The stored integer, or the default value.
     */
    public int getInt(@NonNull final String key, final int defaultValue) {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.getInt(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Saves an integer value to storage.
     *
     * @param key   The storage key. Must not be null.
     * @param value The integer value to save.
     */
    public void setInt(@NonNull final String key, final int value) {
        if (null != getEditor()) {
            getEditor().putInt(key, value).apply();
        }
    }

    /**
     * Retrieves a long value from storage.
     *
     * @param key          The storage key.
     * @param defaultValue The fallback value to return if the key is missing.
     * @return The stored long, or the default value.
     */
    public long getLong(final String key, final long defaultValue) {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.getLong(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Saves a long value to storage.
     *
     * @param key   The storage key. Must not be null.
     * @param value The long value to save.
     */
    public void setLong(@NonNull final String key, final long value) {
        if (null != getEditor()) {
            getEditor().putLong(key, value).apply();
        }
    }

    /**
     * Retrieves a float value from storage.
     *
     * @param key          The storage key.
     * @param defaultValue The fallback value to return if the key is missing.
     * @return The stored float, or the default value.
     */
    public float getFloat(final String key, final float defaultValue) {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.getFloat(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Saves a float value to storage.
     *
     * @param key   The storage key. Must not be null.
     * @param value The float value to save.
     */
    public void setFloat(@NonNull final String key, final float value) {
        if (null != getEditor()) {
            getEditor().putFloat(key, value).apply();
        }
    }

    /**
     * Retrieves a String value from storage.
     *
     * @param key          The storage key.
     * @param defaultValue The fallback value to return if the key is missing.
     * @return The stored String, or the default value.
     */
    public String getString(final String key, final String defaultValue) {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.getString(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Saves a String value to storage.
     *
     * @param key   The storage key. Must not be null.
     * @param value The String value to save. Must not be null.
     */
    public void setString(@NonNull final String key, @NonNull final String value) {
        if (null != getEditor()) {
            getEditor().putString(key, value).apply();
        }
    }

    /**
     * Retrieves a boolean value from storage.
     *
     * @param key          The storage key.
     * @param defaultValue The fallback value to return if the key is missing.
     * @return The stored boolean, or the default value.
     */
    public boolean getBoolean(String key, Boolean defaultValue) {
        if (null != mAndroidSharedPreferences) {
            return mAndroidSharedPreferences.getBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Saves a boolean value to storage.
     *
     * @param key   The storage key. Must not be null.
     * @param value The boolean value to save.
     */
    public void setBoolean(@NonNull final String key, final boolean value) {
        if (null != getEditor()) {
            getEditor().putBoolean(key, value).apply();
        }
    }

    /**
     * Registers a listener to be notified of preference changes.
     * <p>
     * Useful for observing data updates and triggering UI refreshes appropriately.
     * </p>
     *
     * @param listener The listener to register.
     */
    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (mAndroidSharedPreferences != null) {
            mAndroidSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    /**
     * Unregisters a previously registered preference change listener.
     *
     * @param listener The listener to remove.
     */
    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (mAndroidSharedPreferences != null) {
            mAndroidSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    /**
     * Exposes the underlying Android SharedPreferences instance.
     *
     * @return The SharedPreferences object used by this manager, or null if initialization failed.
     */
    @Nullable
    public SharedPreferences getAndroidSharedPreferencesInstance() {
        return mAndroidSharedPreferences;
    }
}
