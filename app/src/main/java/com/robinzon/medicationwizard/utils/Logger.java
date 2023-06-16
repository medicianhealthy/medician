package com.robinzon.medicationwizard.utils;

import android.text.TextUtils;
import android.util.Log;

import com.robinzon.medicationwizard.BuildConfig;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class Logger {

    private static boolean sIsLoggingEnabled;
    private static WeakReference<Logger> sInstance;

    public static Logger getInstance() {
        if (null == sInstance || null == sInstance.get()) {
            sInstance = new WeakReference<>(new Logger());
        }
        return sInstance.get();
    }

    public void log(final String className, final List<String> tags, final String message, Object... params) {
        if (!TextUtils.isEmpty(className) && null != tags && !tags.isEmpty() && !TextUtils.isEmpty(message) && null != params) {
            for (String tag : tags) {
                if (null != tag) {
                    log(className, tag, message, params);
                }
            }
        }
    }

    private void log(final String className, final String tag, final String message, Object... params) {
        Log.i(className.concat(" ").concat(tag), String.format(Locale.getDefault(), message, params));
    }

    public static boolean isLoggingEnabled() {
        return BuildConfig.DEBUG || sIsLoggingEnabled;
    }

    public void setLoggingEnabled(final boolean isEnabled) {
        sIsLoggingEnabled = isEnabled;
    }
}
