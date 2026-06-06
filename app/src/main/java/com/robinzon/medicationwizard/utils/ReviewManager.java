package com.robinzon.medicationwizard.utils;

import android.app.Activity;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.robinzon.medicationwizard.MainActivity;

/**
 * Orchestrates the Google Play In-App Review flow.
 */
public class ReviewManager {

    private static final String SPK_LAST_REVIEW_TIME = "spk_last_review_time";
    
    private static final int MIN_SESSIONS = 3;
    private static final int MIN_DOSES_LOGGED = 15;
    private static final float MIN_USAGE_MINUTES = 5.0f;
    private static final long PROMPT_INTERVAL_MS = 30 * 24 * 60 * 60 * 1000L; // 30 days

    /**
     * Potentially triggers the in-app review flow based on user activity.
     */
    public static void requestReviewIfEligible(Activity activity) {
        if (activity == null) return;

        // 1. Satisfaction Check: Grace period and usage thresholds
        int sessions = Statisticator.getSessionCount(activity);
        int doses = Statisticator.getTotalDosesLogged(activity);
        float usageMinutes = Statisticator.getTotalUsageMinutes(activity);

        if (sessions < MIN_SESSIONS || doses < MIN_DOSES_LOGGED || usageMinutes < MIN_USAGE_MINUTES) {
            Logger.log("ReviewManager", "Thresholds not met: Sess=" + sessions + ", Doses=" + doses + ", Mins=" + usageMinutes);
            return;
        }

        // 2. Frequency Check: Don't prompt too often
        long lastPrompt = SharedPreferencesManager.getInstance(activity).getLong(SPK_LAST_REVIEW_TIME, 0);
        if (System.currentTimeMillis() - lastPrompt < PROMPT_INTERVAL_MS) {
            Logger.log("ReviewManager", "Prompted recently. Waiting for interval.");
            return;
        }

        // 3. Collision Avoidance: Check AdsManager cooldown (Interstitials)
        if (activity instanceof MainActivity) {
            MainActivity main = (MainActivity) activity;
            if (main.getAdsManager() != null && !main.getAdsManager().hasCoolDownForFullScreenNonUserInitiatedAd()) {
                Logger.log("ReviewManager", "Skipping to avoid ad/overlay collision.");
                return;
            }
        }

        // 4. Launch Play Store Review
        com.google.android.play.core.review.ReviewManager manager = ReviewManagerFactory.create(activity);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                flow.addOnCompleteListener(t -> {
                    // Update timestamp regardless of outcome (we don't know if they rated)
                    SharedPreferencesManager.getInstance(activity).setLong(SPK_LAST_REVIEW_TIME, System.currentTimeMillis());
                    Logger.log("ReviewManager", "Review flow finished.");
                });
            } else {
                Logger.log("ReviewManager", "Review request failed.");
            }
        });
    }
}