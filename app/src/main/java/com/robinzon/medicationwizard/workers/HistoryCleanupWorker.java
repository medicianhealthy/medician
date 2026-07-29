package com.robinzon.medicationwizard.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.utils.Logger;

/**
 * Background worker that periodically purges old dose history to keep the database size manageable.
 * <p>
 * This worker calculates the cut-off timestamp based on {@link AppConfig#NUMBER_OF_DAYS_TO_KEEP_HISTORY}
 * and deletes all dose instances older than that.
 * </p>
 */
public class HistoryCleanupWorker extends Worker {

    public HistoryCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.log("HistoryCleanupWorker", "Starting history cleanup...");

        try {
            int retentionDays = AppConfig.getHistoryRetentionDays();
            long thresholdMillis = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal() - ((long) retentionDays * 24 * 60 * 60 * 1000L);

            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            db.doseInstanceDao().deleteOldInstances(thresholdMillis);

            Logger.log("HistoryCleanupWorker", "Cleanup completed successfully. Retained: " + retentionDays + " days.");
            return Result.success();
        } catch (Exception e) {
            Logger.log("HistoryCleanupWorker", "Error during cleanup: " + e.getMessage());
            return Result.failure();
        }
    }
}
