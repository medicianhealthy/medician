package com.robinzon.medicationwizard.utils;

import android.content.Context;

public class Statisticator {

    private static final String SPK_SESSION_COUNT = "spk_session_count";

    public static void onSessionStarted(final Context context) {
        final SharedPreferencesManager sharedPreferencesManager = SharedPreferencesManager.getInstance(context);
        final int numberOfSessionsSoFar = sharedPreferencesManager.getInt(SPK_SESSION_COUNT, 0);
        sharedPreferencesManager.setInt(SPK_SESSION_COUNT, numberOfSessionsSoFar + 1);
    }
}
