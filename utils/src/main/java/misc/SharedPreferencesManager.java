package misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import interfaces.BasicMap;

public class SharedPreferencesManager implements BasicMap {

    private SharedPreferences sAndroidSharedPreferencesInstance;
    private static WeakReference<SharedPreferencesManager> sManagerInstance;

    public static SharedPreferencesManager getInstance(@NonNull final Context context) {
        if (null == sManagerInstance || null == sManagerInstance.get()) {
            sManagerInstance = new WeakReference<>(new SharedPreferencesManager(context));
        }
        return sManagerInstance.get();
    }

    private SharedPreferencesManager(@NonNull final Context context) {
        final String fileName = getFileName(context);
        if (!TextUtils.isEmpty(fileName)) {
            sAndroidSharedPreferencesInstance = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        } else {
            Logger.log(getClassNameForLog()+Logger.SHARED_PREFERENCES,
                    "File name of shared preferences is invalid. Could not create instance");
        }
    }

    private static String getFileName(final Context context) {
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


    @Override
    public void removeKey(String key) {
        final SharedPreferences.Editor editor = getEditor();
        if (null != editor) {
            editor.remove(key).apply();
        }
    }

    private SharedPreferences.Editor getEditor() {
        if (null != sAndroidSharedPreferencesInstance) {
            return sAndroidSharedPreferencesInstance.edit();
        }
        return null;
    }

    @Override
    public boolean containsKey(String key) {
        if (null != sAndroidSharedPreferencesInstance && !TextUtils.isEmpty(key)) {
            return sAndroidSharedPreferencesInstance.contains(key);
        }
        return false;
    }

    @Override
    public void setValue(final String key, final Object value) {
        if(TextUtils.isEmpty(key) ||
                null == value ||
                (value instanceof String && TextUtils.isEmpty((String)value))){
            Logger.log(getClassNameForLog()+Logger.SHARED_PREFERENCES,
                    "Trying to set value but key or value is invalid");
            return;
        }
        final SharedPreferences.Editor editor = getEditor();
        if(null == editor){
            Logger.log(getClassNameForLog()+Logger.SHARED_PREFERENCES,
                    "Trying to set value but editor object is null");
            return;
        }
        if (value instanceof Integer) {
            editor.putInt(key, (Integer)value).apply();
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

    @Override
    public int getInt(final String key, final int defaultValue) {
        if (null != sAndroidSharedPreferencesInstance) {
            return sAndroidSharedPreferencesInstance.getInt(key,defaultValue);
        }
        return defaultValue;
    }

    @Override
    public long getLong(final String key, final long defaultValue) {
        if (null != sAndroidSharedPreferencesInstance) {
            return sAndroidSharedPreferencesInstance.getLong(key,defaultValue);
        }
        return defaultValue;
    }

    @Override
    public float getFloat(final String key, final float defaultValue) {
        if (null != sAndroidSharedPreferencesInstance) {
            return sAndroidSharedPreferencesInstance.getFloat(key,defaultValue);
        }
        return defaultValue;
    }

    @Override
    public String getString(final String key, final String defaultValue) {
        if (null != sAndroidSharedPreferencesInstance) {
            return sAndroidSharedPreferencesInstance.getString(key,defaultValue);
        }
        return defaultValue;
    }
}
