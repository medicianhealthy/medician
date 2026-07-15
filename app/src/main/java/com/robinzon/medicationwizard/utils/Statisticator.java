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
     * @param context Application context.
     */
    public static void onSessionStarted(final Context context) {
        sExecutor.execute(() -> {
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
     * @return Total accumulated usage time in minutes, including the current active session.
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
     * Increments the total count of medication doses logged (taken/skipped) by the user.
     *
     * @param context Application context.
     */
    public static void incrementDosesLogged(Context context) {
        sExecutor.execute(() -> {
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
     * @return The number of actions performed since the last interstitial reset.
     */
    public static int getActionsForInterstitialCount(Context context) {
        return SharedPreferencesManager.getInstance(context).getInt(SPK_ACTIONS_FOR_INTERSTITIAL, 0);
    }

    /**
     * Increments the persistent action counter and checks if an interstitial should be triggered.
     * Logic: Returns true every N actions (defined by Remote Config).
     */
    public static boolean incrementActionsAndCheckAdEligibility(Context context) {
        if (context == null) return false;
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(context);
        int currentActions = prefs.getInt(SPK_ACTIONS_FOR_INTERSTITIAL, 0) + 1;
        int threshold = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getActionsPerInterstitial();

        if (currentActions >= threshold) {
            prefs.setInt(SPK_ACTIONS_FOR_INTERSTITIAL, 0);
            if (BuildConfig.DEBUG) {
                Toast.makeText(context, "Action " + threshold + "/" + threshold + " reached, triggering ad", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else {
            prefs.setInt(SPK_ACTIONS_FOR_INTERSTITIAL, currentActions);
            if (BuildConfig.DEBUG) {
                Toast.makeText(context, "Action " + currentActions + "/" + threshold + " reached", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    /**
     * Increments the persistent interaction score and checks if it has met the threshold.
     * Main items add 1.5, sub-items add 1.0.
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
            if (BuildConfig.DEBUG && scoreToAdd > 0) {
                Toast.makeText(context, "Added " + scoreToAdd + " points, reached " + threshold + ", total is " + remainder, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else {
            prefs.setFloat(SPK_INTERSTITIAL_SCORE, currentScore);
            if (BuildConfig.DEBUG && scoreToAdd > 0) {
                Toast.makeText(context, "Added " + scoreToAdd + " points, total is " + currentScore, Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    /**
     * @param context Application context.
     * @return Usage time in minutes accumulated since the last Full Screen Ad (FSA), including current session.
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
     * Resets the usage timer for ads. Should be called after an FSA (Interstitial or Rewarded) is shown.
     *
     * @param context Application context.
     */
    public static void resetUsageMinutesForAds(final Context context) {
        sExecutor.execute(() -> {
            SharedPreferencesManager.getInstance(context).setFloat(SPK_USAGE_MINUTES_FOR_ADS, 0F);
            // Reset the foreground anchor for ad usage so only future time counts
            mStartAdUsageActive = System.currentTimeMillis();
        });
    }

    /**
     * Called when the app moves to the background to finalize usage time tracking for the current session.
     *
     * @param context Application context.
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
     * Called when the app moves to the foreground to start tracking active usage time.
     *
     * @param context Application context.
     */
    public static void onMoveToForeground(final Context context) {
        sExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            mStartUserActive = now;
            mStartAdUsageActive = now;
        });
    }
}
