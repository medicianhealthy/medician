package com.robinzon.medicationwizard.utils;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.robinzon.medicationwizard.BuildConfig;

public class Logger {
    public static final String REMOTE_CONFIG = "medi_remoteconfig";
    public static final String SHARED_PREFS = "medi_shared_prefs";
    public static final boolean IS_LOGGING_ENABLED = BuildConfig.DEBUG;

    public static void log(@NonNull final String tag,
                           @NonNull final String message,
                           @NonNull Object... params) {

        if (IS_LOGGING_ENABLED &&
                !TextUtils.isEmpty(tag) &&
                !TextUtils.isEmpty(message)) {
            Log.i(tag, String.format(message, params));
        }

    }
}
