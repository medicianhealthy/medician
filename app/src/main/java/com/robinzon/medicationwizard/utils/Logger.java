package com.robinzon.medicationwizard.utils;

import android.util.Log;

import com.robinzon.medicationwizard.BuildConfig;

import java.util.List;
import java.util.Locale;

public class Logger {

    private static boolean sIsLoggingEnabled;

    public static void logSingleTag(final String className,
                           final String tag,
                           final String message,
                           Object...params){
        if (isLoggingEnabled() &&
                Validator.isValidString(className) &&
                Validator.isValidString(tag) &&
                Validator.isValidString(message) &&
                Validator.isValidObject(params))
        log(className , tag , message, params);
    }

    public static void logMultipleTags(final String className,
                           final List<String> tags,
                           final String message,
                           Object...params){
        if (isLoggingEnabled() &&
                Validator.isValidString(className) &&
                Validator.isValidString(message) &&
                Validator.isValidCollection(tags) &&
                Validator.isValidObject(params)){
            for (String tag : tags) {
                if(Validator.isValidString(tag)) {
                    log(className, tag, message, params);
                }
            }
        }
    }

    private static void log(final String className, final String tag, final String message, Object...params){
        Log.i(className.concat("- ").concat(tag), String.format(Locale.getDefault(), message, params));
    }

    public static boolean isLoggingEnabled(){
        return BuildConfig.DEBUG || sIsLoggingEnabled;
    }

    public void setLoggingEnabled(final boolean isEnabled){
        sIsLoggingEnabled = isEnabled;
    }
}
