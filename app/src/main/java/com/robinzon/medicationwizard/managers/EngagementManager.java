package com.robinzon.medicationwizard.managers;

import android.content.Context;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.workers.EngagementWorker;

import java.util.concurrent.TimeUnit;

/**
 * Manages user engagement tracking and triggers re-engagement notifications.
 */
public class EngagementManager {

    private static final String WORK_NAME = "engagement_check";

    /**
     * Records a new user interaction and resets the 24-hour inactivity timer.
     */
    public static void recordInteraction(Context context) {
        long now = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();
        SharedPreferencesManager.getInstance(context).setLong(AppConfig.KEY_LAST_INTERACTION_TIME, now);

        // Reset the 24-hour countdown worker
        scheduleEngagementCheck(context);
    }

    /**
     * Schedules a one-time worker to fire in 24 hours.
     * Every call with REPLACE will reset the timer.
     */
    private static void scheduleEngagementCheck(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(EngagementWorker.class)
                .setInitialDelay(24, TimeUnit.HOURS)
                .addTag("engagement")
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }
}
