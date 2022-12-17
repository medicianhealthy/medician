package com.robinzon.medicationwizard.ads;

import android.content.Context;

import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

public class AdsStatsManger {
    public static final String LAST_TIME_STAMP_FOR_INTER = "last_time_stamp_for_inter";

    public static void onInterstitialDismissed(final Context context) {
        SharedPreferencesManager.getInstance(context).setValue(LAST_TIME_STAMP_FOR_INTER, System.currentTimeMillis());
    }

    public static int getSecondsPassedFromLastInterstitialDismissed(final Context context) {
        final long timeNow = System.currentTimeMillis();
        final long lastInterDismissed = SharedPreferencesManager.getInstance(context).getLong(LAST_TIME_STAMP_FOR_INTER, 0L);
        return (int) ((timeNow - lastInterDismissed) / 1000);
    }
}
