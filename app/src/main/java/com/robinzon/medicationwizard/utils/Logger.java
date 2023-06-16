package com.robinzon.medicationwizard.utils;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.robinzon.medicationwizard.BuildConfig;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class Logger {

    private boolean sIsLoggingEnabled;
    private static WeakReference<Logger> sInstance;

    public static Logger getInstance() {
        if (null == sInstance || null == sInstance.get()) {
            sInstance = new WeakReference<>(new Logger());
        }
        return sInstance.get();
    }

    public void log(@Nullable final String className,
                    @Nullable final List<String> tags,
                    @Nullable final String message,
                    @Nullable Object... params) {
        if (!TextUtils.isEmpty(className) && null != tags && !tags.isEmpty() && !TextUtils.isEmpty(message) && null != params) {
            for (String tag : tags) {
                if (null != tag) {
                    log(className, tag, message, params);
                }
            }
        }
    }

    private void log(@NonNull final String className, @NonNull final String tag, @NonNull final String message, @NonNull Object... params) {
        Log.i(className.concat(" ").concat(tag), String.format(Locale.getDefault(), message, params));
    }

    public static boolean isLoggingEnabled() {
        return BuildConfig.DEBUG || getInstance().sIsLoggingEnabled;
    }

    @SuppressWarnings("unused")
    public void setLoggingEnabled(final boolean isEnabled) {
        sIsLoggingEnabled = isEnabled;
    }
}
