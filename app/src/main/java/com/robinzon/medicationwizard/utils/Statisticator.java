package com.robinzon.medicationwizard.utils;

import android.content.Context;
import android.os.AsyncTask;

public class Statisticator {

    private static final String SPK_SESSION_COUNT = "spk_session_count";
    private static final String SPK_SESSION_TIME_MINUTES = "spk_session_time_minutes";
    private static long mStartUserActive;

    public static void onSessionStarted(final Context context) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                SharedPreferencesManager.getInstance(context).setInt(SPK_SESSION_COUNT, getSessionCount(context) + 1);
            }
        });
    }

    public static int getSessionCount(final Context context) {
        return SharedPreferencesManager.getInstance(context).getInt(SPK_SESSION_COUNT, 0);
    }

    private static float getLastSavedSessionTimeMinutes(Context context) {
        return SharedPreferencesManager.getInstance(context).getFloat(SPK_SESSION_TIME_MINUTES, 0F);
    }

    public static void onMoveToBackground(final Context context) {
        Runnable runnable = new Runnable() {
            public void run() {
                final float currentSessionTimeInMinutes = ((float) System.currentTimeMillis() - (float) mStartUserActive) / 1000F / 60F;
                SharedPreferencesManager.getInstance(context).setFloat(SPK_SESSION_TIME_MINUTES,
                        getLastSavedSessionTimeMinutes(context) + currentSessionTimeInMinutes);
            }
        };
        AsyncTask.execute(runnable);
    }

    /** @noinspection unused*/
    public static void onMoveToForeground(final Context context) {
        Runnable runnable = new Runnable() {
            public void run() {
                mStartUserActive = System.currentTimeMillis();
            }
        };
        AsyncTask.execute(runnable);
    }
}
