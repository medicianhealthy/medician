package com.robinzon.medicationwizard.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.ui.settings.SettingsViewModel;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

/**
 * Handles background actions triggered from notification buttons (Take, Snooze, Skip).
 * <p>
 * This receiver ensures that user actions are processed without launching the main application UI.
 * It coordinates with the database to update dose statuses and interacts with {@link ReminderManager}
 * to handle snooze scheduling logic.
 * </p>
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_TAKE = "com.robinzon.medicationwizard.ACTION_TAKE";
    public static final String ACTION_SNOOZE = "com.robinzon.medicationwizard.ACTION_SNOOZE";
    public static final String ACTION_SKIP = "com.robinzon.medicationwizard.ACTION_SKIP";
    public static final String EXTRA_INSTANCE_ID = "extra_instance_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        int instanceId = intent.getIntExtra(EXTRA_INSTANCE_ID, -1);
        if (instanceId == -1) return;

        // NEW: Stop the reminder sound immediately upon any interaction
        ReminderAlertManager.getInstance().stopAlarm();

        // Dismiss the notification immediately
        com.robinzon.medicationwizard.notifications.NotificationManager.dismissNotification(context, instanceId);

        String action = intent.getAction();
        if (action == null) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            DoseInstanceEntity instance = db.doseInstanceDao().getInstanceById(instanceId);

            if (instance == null) return;

            switch (action) {
                case ACTION_TAKE:
                    instance.setStatus("TAKEN");
                    instance.setActionTime(System.currentTimeMillis());
                    db.doseInstanceDao().update(instance);
                    break;

                case ACTION_SKIP:
                    instance.setStatus("SKIPPED");
                    instance.setActionTime(System.currentTimeMillis());
                    db.doseInstanceDao().update(instance);
                    break;

                case ACTION_SNOOZE:
                    handleSnooze(context, db, instance);
                    break;
            }
        });
    }

    private void handleSnooze(Context context, AppDatabase db, DoseInstanceEntity instance) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        int maxSnoozes = sp.getInt(SettingsViewModel.KEY_MAX_SNOOZES, 3);
        int snoozeDuration = sp.getInt(SettingsViewModel.KEY_SNOOZE_DURATION_MINS, 10);

        int currentSnoozes = instance.getSnoozeCount();
        com.robinzon.medicationwizard.utils.Logger.log("Snooze", "Snoozing " + instance.getMedicationName() + ". Current: " + currentSnoozes + ", Max: " + maxSnoozes);

        if (maxSnoozes != -1 && currentSnoozes >= maxSnoozes) {
            com.robinzon.medicationwizard.utils.Logger.log("Snooze", "Limit reached for " + instance.getMedicationName() + ". Marking as SKIPPED.");
            // Reached limit, mark as skipped
            instance.setStatus("SKIPPED");
            instance.setActionTime(System.currentTimeMillis());
            db.doseInstanceDao().update(instance);
        } else {
            // Can snooze again
            instance.setSnoozeCount(currentSnoozes + 1);
            // Move scheduled time forward
            long newTime = System.currentTimeMillis() + (snoozeDuration * 60 * 1000L);
            instance.setScheduledTime(newTime);
            db.doseInstanceDao().update(instance);

            com.robinzon.medicationwizard.utils.Logger.log("Snooze", "Alarm rescheduled to " + newTime + " (Count: " + (currentSnoozes + 1) + ")");
            // Re-schedule the alarm
            ReminderManager.scheduleReminder(context, instance);
        }
    }
}