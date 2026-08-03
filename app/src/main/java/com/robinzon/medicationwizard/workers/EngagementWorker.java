package com.robinzon.medicationwizard.workers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.notifications.NotificationManager;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Worker that fires after 24 hours of inactivity to re-engage the user.
 */
public class EngagementWorker extends Worker {

    public EngagementWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // 1. Check Time Window (9:00 - 18:00)
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);

        if (hour < 9 || hour >= 18) {
            // Outside window. Reschedule for the next 9:00 AM
            Calendar nextNine = (Calendar) now.clone();
            if (hour >= 18) {
                nextNine.add(Calendar.DAY_OF_YEAR, 1);
            }
            nextNine.set(Calendar.HOUR_OF_DAY, 9);
            nextNine.set(Calendar.MINUTE, 0);
            nextNine.set(Calendar.SECOND, 0);

            long delayMillis = nextNine.getTimeInMillis() - now.getTimeInMillis();
            
            OneTimeWorkRequest retryRequest = new OneTimeWorkRequest.Builder(EngagementWorker.class)
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .addTag("engagement")
                    .build();

            WorkManager.getInstance(context).enqueueUniqueWork(
                    "engagement_check",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    retryRequest
            );

            return Result.success();
        }

        // 2. Prepare content
        List<Medication> meds = Medication.getSavedMedications(context);

        String title;
        String message;

        if (meds.isEmpty()) {
            // Scenario 1: No meds yet
            title = context.getString(R.string.engagement_no_meds_title);
            message = context.getString(R.string.engagement_no_meds_message);
        } else {
            boolean isAllPrn = true;
            for (Medication m : meds) {
                if (!m.isAsNeeded()) {
                    isAllPrn = false;
                    break;
                }
            }

            if (isAllPrn) {
                // Scenario 2: All meds are PRN
                title = context.getString(R.string.engagement_prn_only_title);
                message = context.getString(R.string.engagement_prn_only_message);
            } else {
                // Normal user but hasn't interacted. 
                // Maybe he took meds but didn't open the app? 
                // We show a general "Wizard missed you" message.
                title = context.getString(R.string.engagement_general_title);
                message = context.getString(R.string.engagement_general_message);
            }
        }

        postEngagementNotification(context, title, message);

        return Result.success();
    }

    private void postEngagementNotification(Context context, String title, String message) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        String channelId = NotificationManager.CHANNEL_ID;

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 999, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_magic_wand)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setColor(context.getColor(R.color.md_theme_light_primary))
                .setCategory(NotificationCompat.CATEGORY_PROMO);

        try {
            nm.notify(12345, builder.build());
        } catch (SecurityException ignored) {
        }
    }
}
