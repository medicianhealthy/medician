package com.robinzon.medicationwizard.ads;

import android.content.Context;

import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.lang.ref.WeakReference;

public class AdsStatsManger {
    public final String LAST_TIME_STAMP_FOR_INTER = "last_time_stamp_for_inter";

    private static WeakReference<AdsStatsManger> sThisInstance;


    public static AdsStatsManger getInstance() {
        if (null == sThisInstance || null == sThisInstance.get()){
            sThisInstance = new WeakReference<>(new AdsStatsManger());
        }
        return sThisInstance.get();
    }


    public void onInterstitialDismissed(final Context context) {
        SharedPreferencesManager.getInstance(context).setValue(LAST_TIME_STAMP_FOR_INTER, System.currentTimeMillis());
    }

    public int getSecondsPassedFromLastInterstitialDismissed(final Context context) {
        final long timeNow = System.currentTimeMillis();
        final long lastInterDismissed = SharedPreferencesManager.getInstance(context).getLong(LAST_TIME_STAMP_FOR_INTER, 0L);
        return (int) ((timeNow - lastInterDismissed) / 1000);
    }
}
