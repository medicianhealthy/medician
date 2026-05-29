package com.robinzon.medicationwizard.utils;

import android.app.Activity;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManagerFactory;

/**
 * Orchestrates the Google Play In-App Review flow.
 * <p>
 * This manager uses internal usage metrics to determine the optimal "Magic Moment" 
 * for presenting the review card, ensuring compliance with Google's zero-friction policy.
 * </p>
 */
public class ReviewManager {

    private static final int MIN_SESSIONS = 3;
    private static final int MIN_DOSES = 10;

    /**
     * Potentially triggers the in-app review flow based on user activity.
     * <p>
     * Performance: Checks eligibility using SharedPreferences before initializing the 
     * Play Store Review API to avoid unnecessary overhead.
     * </p>
     *
     * @param activity The active activity from which to launch the review.
     */
    public static void requestReviewIfEligible(Activity activity) {
        if (activity == null) return;

        // 1. Eligibility Check: Power User Thresholds
        int sessions = Statisticator.getSessionCount(activity);
        int doses = Statisticator.getTotalDosesLogged(activity);

        if (sessions < MIN_SESSIONS && doses < MIN_DOSES) {
            return;
        }

        // 2. Initialize Google Play Review Manager
        com.google.android.play.core.review.ReviewManager manager = ReviewManagerFactory.create(activity);
        
        // 3. Request the review info object
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 4. Launch the flow (Google decides if it actually shows based on quotas)
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                flow.addOnCompleteListener(t -> {
                    // Flow finished; user continues with the app.
                    Logger.log("ReviewManager", "In-App Review flow completed.");
                });
            } else {
                Logger.log("ReviewManager", "Failed to request review flow: " + task.getException());
            }
        });
    }
}
