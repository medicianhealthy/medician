package com.robinzon.medicationwizard.utils;

import android.content.Context;
import android.widget.Toast;

import com.robinzon.medicationwizard.BuildConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for tracking application usage statistics.
 * <p>
 * Performance: Operations are performed asynchronously using a dedicated background executor
 * to ensure that disk I/O for SharedPreferences does not block the UI thread.
 * </p>
 */
public class Statisticator {

    private static final ExecutorService sExecutor = Executors.newSingleThreadExecutor();
    private static final String SPK_SESSION_COUNT = "spk_session_count";
    private static final String SPK_SESSION_TIME_MINUTES = "spk_session_time_minutes";
    private static final String SPK_USAGE_MINUTES_FOR_ADS = "spk_usage_minutes_for_ads";
    private static final String SPK_TOTAL_DOSES_LOGGED = "spk_total_doses_logged";
    private static final String SPK_ACTIONS_FOR_INTERSTITIAL = "spk_actions_for_interstitial";
    private static final String SPK_INTERSTITIAL_SCORE = "spk_interstitial_score";

    /**
     * Anchor for total usage calculation in the current foreground session.
     */
    private static long mStartUserActive;

    /**
     * Anchor for ad-specific usage calculation (resets after showing an ad).
     */
    private static long mStartAdUsageActive;

    /**
     * Records the start of a new app session and increments the persistent counter.
     *
     * @param context Application context. Must not be null.
     */
    public static void onSessionStarted(final Context context) {
        sExecutor.execute(() -> {
            SharedPreferencesManager.getInstance(context).setInt(SPK_SESSION_COUNT, getSessionCount(context) + 1);
        });
    }

    /**
     * Retrieves the total number of sessions started since the app was installed.
     *
     * @param context Application context. Must not be null.
     * @return Total number of app sessions started.
     */
    public static int getSessionCount(final Context context) {
        return SharedPreferencesManager.getInstance(context).getInt(SPK_SESSION_COUNT, 0);
    }

    /**
     * Calculates the total accumulated usage time in minutes.
     * <p>
     * Factors in both persistently stored time and the active elapsed time from the current session.
     * </p>
     *
     * @param context Application context. Must not be null.
     * @return Total accumulated usage time in minutes.
     */
    public static float getTotalUsageMinutes(final Context context) {
        float persisted = SharedPreferencesManager.getInstance(context).getFloat(SPK_SESSION_TIME_MINUTES, 0F);
        if (mStartUserActive > 0) {
            float sessionElapsed = ((float) System.currentTimeMillis() - (float) mStartUserActive) / 1000F / 60F;
            if (sessionElapsed > 0) {
                persisted += sessionElapsed;
            }
        }
        return persisted;
    }

    /**
     * Increments the total count of medication doses logged (either taken or skipped) by the user.
     *
     * @param context Application context. Must not be null.
     */
    public static void incrementDosesLogged(Context context) {
        sExecutor.execute(() -> {
            int count = SharedPreferencesManager.getInstance(context).getInt(SPK_TOTAL_DOSES_LOGGED, 0);
            SharedPreferencesManager.getInstance(context).setInt(SPK_TOTAL_DOSES_LOGGED, count + 1);
        });
    }

    /**
     * Retrieves the total count of medication doses logged across all time.
     *
     * @param context Application context. Must not be null.
     * @return Total number of doses logged.
     */
    public static int getTotalDosesLogged(Context context) {
        return SharedPreferencesManager.getInstance(context).getInt(SPK_TOTAL_DOSES_LOGGED, 0);
    }

    /**
     * Retrieves the number of actions recorded since the last interstitial ad reset.
     *
     * @param context Application context. Must not be null.
     * @return The current count of actions targeting interstitial logic.
     */
    public static int getActionsForInterstitialCount(Context context) {
        return SharedPreferencesManager.getInstance(context).getInt(SPK_ACTIONS_FOR_INTERSTITIAL, 0);
    }

    /**
     * Increments the persistent action counter and evaluates if an interstitial should be triggered.
     * <p>
     * Driven by the logic threshold defined by the Remote Config value. When the threshold is reached,
     * the counter is reset.
     * </p>
     *
     * @param context Application context. Can be null.
     * @return True if the interstitial threshold is met and an ad should be displayed, false otherwise.
     */
    public static boolean incrementActionsAndCheckAdEligibility(Context context) {
        if (context == null) return false;
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(context);
        int currentActions = prefs.getInt(SPK_ACTIONS_FOR_INTERSTITIAL, 0) + 1;
        int threshold = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getActionsPerInterstitial();

        if (currentActions >= threshold) {
            prefs.setInt(SPK_ACTIONS_FOR_INTERSTITIAL, 0);
            return true;
        } else {
            prefs.setInt(SPK_ACTIONS_FOR_INTERSTITIAL, currentActions);
            return false;
        }
    }

    /**
     * Increments the persistent interaction score and evaluates if the threshold is met.
     * <p>
     * Different actions can carry different weights (e.g., main items add 1.5, sub-items add 1.0).
     * The threshold is determined by Remote Config.
     * </p>
     *
     * @param context     Application context. Can be null.
     * @param scoreToAdd  The score to append to the current interstitial score pool.
     * @return True if the interaction threshold is reached, triggering a potential event/ad; false otherwise.
     */
    public static boolean addInteractionScoreAndCheck(Context context, float scoreToAdd) {
        if (context == null) return false;
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(context);
        float currentScore = prefs.getFloat(SPK_INTERSTITIAL_SCORE, 0.0f) + scoreToAdd;

        double threshold = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getDoubleValue("interstitial_score_threshold");
        if (threshold <= 0) threshold = 4.0; // Fallback

        if (currentScore >= threshold) {
            float remainder = (float) (currentScore - (float) threshold);
            prefs.setFloat(SPK_INTERSTITIAL_SCORE, remainder);
            return true;
        } else {
            prefs.setFloat(SPK_INTERSTITIAL_SCORE, currentScore);
            return false;
        }
    }

    /**
     * Retrieves the usage time accumulated since the last Full Screen Ad (FSA) was displayed.
     *
     * @param context Application context. Must not be null.
     * @return Usage time in minutes accumulated since the last FSA display, including the active session time.
     */
    public static float getUsageMinutesForAds(final Context context) {
        float persisted = SharedPreferencesManager.getInstance(context).getFloat(SPK_USAGE_MINUTES_FOR_ADS, 0F);
        if (mStartAdUsageActive > 0) {
            float sessionElapsed = ((float) System.currentTimeMillis() - (float) mStartAdUsageActive) / 1000F / 60F;
            if (sessionElapsed > 0) {
                persisted += sessionElapsed;
            }
        }
        return persisted;
    }

    /**
     * Resets the usage timer dedicated to ads.
     * <p>
     * Must be called immediately following the display of an FSA (Interstitial or Rewarded)
     * to reset the timer anchor.
     * </p>
     *
     * @param context Application context. Must not be null.
     */
    public static void resetUsageMinutesForAds(final Context context) {
        sExecutor.execute(() -> {
            SharedPreferencesManager.getInstance(context).setFloat(SPK_USAGE_MINUTES_FOR_ADS, 0F);
            // Reset the foreground anchor for ad usage so only future time counts
            mStartAdUsageActive = System.currentTimeMillis();
        });
    }

    /**
     * Halts usage time tracking when the app transitions to the background.
     * <p>
     * Synchronizes and persists the current session's elapsed time into the aggregate totals.
     * </p>
     *
     * @param context Application context. Must not be null.
     */
    public static void onMoveToBackground(final Context context) {
        sExecutor.execute(() -> {
            // Persist the live values which already include the elapsed foreground time
            SharedPreferencesManager.getInstance(context).setFloat(SPK_SESSION_TIME_MINUTES, getTotalUsageMinutes(context));
            SharedPreferencesManager.getInstance(context).setFloat(SPK_USAGE_MINUTES_FOR_ADS, getUsageMinutesForAds(context));

            // Stop live tracking
            mStartUserActive = 0;
            mStartAdUsageActive = 0;
        });
    }

    /**
     * Initiates active usage time tracking when the app enters the foreground.
     *
     * @param context Application context. Must not be null.
     */
    public static void onMoveToForeground(final Context context) {
        sExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            mStartUserActive = now;
            mStartAdUsageActive = now;
        });
    }
}
