package com.robinzon.medicationwizard.utils;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Utility class for tracking application usage statistics.
 * <p>
 * Performance: Operations are performed asynchronously using {@link AsyncTask} 
 * to ensure that disk I/O for SharedPreferences does not block the UI thread.
 * </p>
 */
public class Statisticator {

    private static final String SPK_SESSION_COUNT = "spk_session_count";
    private static final String SPK_SESSION_TIME_MINUTES = "spk_session_time_minutes";
    private static final String SPK_USAGE_MINUTES_FOR_ADS = "spk_usage_minutes_for_ads";
    private static final String SPK_TOTAL_DOSES_LOGGED = "spk_total_doses_logged";
    private static long mStartUserActive;

    /**
     * Records the start of a new app session and increments the persistent counter.
     *
     * @param context Application context.
     */
    public static void onSessionStarted(final Context context) {
        AsyncTask.execute(() -> {
            SharedPreferencesManager.getInstance(context).setInt(SPK_SESSION_COUNT, getSessionCount(context) + 1);
        });
    }

    /**
     * @param context Application context.
     * @return Total number of app sessions started.
     */
    public static int getSessionCount(final Context context) {
        return SharedPreferencesManager.getInstance(context).getInt(SPK_SESSION_COUNT, 0);
    }

    /**
     * @param context Application context.
     * @return Total accumulated usage time in minutes.
     */
    public static float getTotalUsageMinutes(final Context context) {
        return SharedPreferencesManager.getInstance(context).getFloat(SPK_SESSION_TIME_MINUTES, 0F);
    }

    /**
     * Increments the total count of medication doses logged (taken/skipped) by the user.
     *
     * @param context Application context.
     */
    public static void incrementDosesLogged(Context context) {
        AsyncTask.execute(() -> {
            int count = SharedPreferencesManager.getInstance(context).getInt(SPK_TOTAL_DOSES_LOGGED, 0);
            SharedPreferencesManager.getInstance(context).setInt(SPK_TOTAL_DOSES_LOGGED, count + 1);
        });
    }

    /**
     * @param context Application context.
     * @return Total number of doses logged across all time.
     */
    public static int getTotalDosesLogged(Context context) {
        return SharedPreferencesManager.getInstance(context).getInt(SPK_TOTAL_DOSES_LOGGED, 0);
    }

    /**
     * @param context Application context.
     * @return Usage time in minutes accumulated since the last Full Screen Ad (FSA).
     */
    public static float getUsageMinutesForAds(final Context context) {
        return SharedPreferencesManager.getInstance(context).getFloat(SPK_USAGE_MINUTES_FOR_ADS, 0F);
    }

    /**
     * Resets the usage timer for ads. Should be called after an FSA (Interstitial or Rewarded) is shown.
     *
     * @param context Application context.
     */
    public static void resetUsageMinutesForAds(final Context context) {
        AsyncTask.execute(() -> {
            SharedPreferencesManager.getInstance(context).setFloat(SPK_USAGE_MINUTES_FOR_ADS, 0F);
        });
    }

    /**
     * Called when the app moves to the background to finalize usage time tracking for the current session.
     *
     * @param context Application context.
     */
    public static void onMoveToBackground(final Context context) {
        AsyncTask.execute(() -> {
            final float currentSessionTimeInMinutes = ((float) System.currentTimeMillis() - (float) mStartUserActive) / 1000F / 60F;
            float totalUsage = getTotalUsageMinutes(context) + currentSessionTimeInMinutes;
            float adUsage = getUsageMinutesForAds(context) + currentSessionTimeInMinutes;
            
            SharedPreferencesManager.getInstance(context).setFloat(SPK_SESSION_TIME_MINUTES, totalUsage);
            SharedPreferencesManager.getInstance(context).setFloat(SPK_USAGE_MINUTES_FOR_ADS, adUsage);
        });
    }

    /**
     * Called when the app moves to the foreground to start tracking active usage time.
     *
     * @param context Application context.
     */
    public static void onMoveToForeground(final Context context) {
        AsyncTask.execute(() -> {
            mStartUserActive = System.currentTimeMillis();
        });
    }
}
