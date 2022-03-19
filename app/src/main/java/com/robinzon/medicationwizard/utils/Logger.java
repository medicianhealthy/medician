package com.robinzon.medicationwizard.utils;

import android.util.Log;

import com.robinzon.medicationwizard.BuildConfig;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class Logger {

    private static boolean mIsLoggingEnabled;
    private static WeakReference<Logger> sInstance;

    public static Logger getInstance() {
        if (null == sInstance || null == sInstance.get()) {
            sInstance = new WeakReference<>(new Logger());
        }
        return sInstance.get();
    }

    public void log(final String className,
                    final List<String> tags,
                    final String message,
                    Object... params) {
        if (Validator.isValidString(className) &&
                Validator.isValidString(message) &&
                Validator.isValidCollection(tags) &&
                Validator.isValidObject(params)) {
            for (String tag : tags) {
                if (Validator.isValidString(tag)) {
                    log(className, tag, message, params);
                }
            }
        }
    }

    private void log(final String className, final String tag, final String message, Object... params) {
        Log.i(className.concat(" ").concat(tag), String.format(Locale.getDefault(), message, params));
    }

    public static boolean isLoggingEnabled() {
        return BuildConfig.DEBUG || mIsLoggingEnabled;
    }

    public void setLoggingEnabled(final boolean isEnabled) {
        mIsLoggingEnabled = isEnabled;
    }
}
