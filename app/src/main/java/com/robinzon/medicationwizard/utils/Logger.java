package com.robinzon.medicationwizard.utils;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.robinzon.medicationwizard.BuildConfig;

/**
 * Global logging utility for the Medication Wizard project.
 * <p>
 * This class provides a centralized way to manage debug logs. It automatically
 * suppresses logs in production builds by checking {@link BuildConfig#DEBUG}.
 * It also supports formatted strings for cleaner log messages.
 * </p>
 */
public class Logger {
    /**
     * Log tag for Remote Config operations.
     */
    public static final String REMOTE_CONFIG = "medi_remoteconfig";
    /**
     * Log tag for SharedPreferences operations.
     */
    public static final String SHARED_PREFS = "medi_shared_prefs";

    /**
     * Global master switch for logging. Disabled in RELEASE builds.
     */
    public static final boolean IS_LOGGING_ENABLED = BuildConfig.DEBUG;

    /**
     * Prints an INFO level log if logging is enabled.
     *
     * @param tag     The category of the log.
     * @param message The message (supports format placeholders).
     * @param params  Values to fill the placeholders in the message.
     */
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