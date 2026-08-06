package com.robinzon.medicationwizard.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.managers.EngagementManager;
import com.robinzon.medicationwizard.managers.InventoryManager;
import com.robinzon.medicationwizard.ui.settings.SettingsViewModel;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.List;

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
    public static final String ACTION_TAKE_ALL = "com.robinzon.medicationwizard.ACTION_TAKE_ALL";
    public static final String ACTION_SNOOZE_ALL = "com.robinzon.medicationwizard.ACTION_SNOOZE_ALL";
    public static final String ACTION_SKIP_ALL = "com.robinzon.medicationwizard.ACTION_SKIP_ALL";
    public static final String ACTION_STOP_ALARM = "com.robinzon.medicationwizard.ACTION_STOP_ALARM";
    public static final String EXTRA_INSTANCE_ID = "extra_instance_id";
    public static final String EXTRA_INSTANCE_IDS = "extra_instance_ids";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        // Record interaction for engagement tracking
        EngagementManager.recordInteraction(context);

        int singleId = intent.getIntExtra(EXTRA_INSTANCE_ID, -1);
        int[] instanceIds = intent.getIntArrayExtra(EXTRA_INSTANCE_IDS);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, singleId);

        // NEW: Stop the reminder sound immediately upon any interaction
        ReminderAlertManager.getInstance().stopAlarm();

        if (ACTION_STOP_ALARM.equals(action)) return;

        if (singleId == -1 && (instanceIds == null || instanceIds.length == 0)) return;

        // Dismiss the notification immediately
        com.robinzon.medicationwizard.notifications.NotificationManager.dismissNotification(context, notificationId);

        final PendingResult pendingResult = goAsync();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(context);

                if (instanceIds != null && instanceIds.length > 0) {
                    for (int id : instanceIds) {
                        DoseInstanceEntity instance = db.doseInstanceDao().getInstanceById(id);
                        if (instance != null) {
                            processAction(context, db, instance, action);
                        }
                    }
                } else {
                    DoseInstanceEntity instance = db.doseInstanceDao().getInstanceById(singleId);
                    if (instance != null) {
                        processAction(context, db, instance, action);
                    }
                }
            } finally {
                if (pendingResult != null) {
                    pendingResult.finish();
                }
            }
        });
    }

    private void processAction(Context context, AppDatabase db, DoseInstanceEntity instance, String action) {
        long now = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();
        switch (action) {
            case ACTION_TAKE:
            case ACTION_TAKE_ALL:
                instance.setStatus("TAKEN");
                instance.setActionTime(now);
                db.doseInstanceDao().update(instance);
                
                // Inventory Logic
                InventoryManager.decrementInventory(context, instance.getMedicationId(), instance.getAmount());

                // Update parent medication definition's last taken time
                updateMedicationLastTakenTime(context, instance.getMedicationId(), now);
                break;

            case ACTION_SKIP:
            case ACTION_SKIP_ALL:
                instance.setStatus("SKIPPED");
                instance.setActionTime(now);
                db.doseInstanceDao().update(instance);
                break;

            case ACTION_SNOOZE:
            case ACTION_SNOOZE_ALL:
                handleSnooze(context, db, instance);
                break;
        }
    }

    private void updateMedicationLastTakenTime(Context context, String medId, long timestamp) {
        List<com.robinzon.medicationwizard.entities.Medication> allMeds = 
                com.robinzon.medicationwizard.entities.Medication.getSavedMedications(context);
        for (com.robinzon.medicationwizard.entities.Medication m : allMeds) {
            if (m.getId().equals(medId)) {
                m.setLastTakenTimestamp(timestamp);
                m.addToMedicationList(context);
                break;
            }
        }
    }

    private void handleSnooze(Context context, AppDatabase db, DoseInstanceEntity instance) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        boolean isCritical = instance.isCritical();
        
        int maxSnoozes = isCritical ? -1 : sp.getInt(SettingsViewModel.KEY_MAX_SNOOZES, 3);
        int snoozeDuration = isCritical ? 5 : sp.getInt(SettingsViewModel.KEY_SNOOZE_DURATION_MINS, 10);

        int currentSnoozes = instance.getSnoozeCount();
        com.robinzon.medicationwizard.utils.Logger.log("Snooze", "Snoozing " + instance.getMedicationName() + ". Current: " + currentSnoozes + ", Max: " + maxSnoozes);

        if (maxSnoozes != -1 && currentSnoozes + 1 >= maxSnoozes) {
            com.robinzon.medicationwizard.utils.Logger.log("Snooze", "Limit reached for " + instance.getMedicationName() + ". Marking as SKIPPED on this snooze attempt.");
            // Reached limit, mark as skipped
            instance.setStatus("SKIPPED");
            instance.setActionTime(com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal());
            db.doseInstanceDao().update(instance);
        } else {
            // Can snooze again
            instance.setSnoozeCount(currentSnoozes + 1);
            // Move scheduled time forward
            long newTime = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal() + (snoozeDuration * 60 * 1000L);
            instance.setScheduledTime(newTime);
            db.doseInstanceDao().update(instance);

            com.robinzon.medicationwizard.utils.Logger.log("Snooze", "Alarm rescheduled to " + newTime + " (Count: " + (currentSnoozes + 1) + ")");
            // Re-schedule the alarm
            ReminderManager.scheduleReminder(context, instance);
        }
    }
}