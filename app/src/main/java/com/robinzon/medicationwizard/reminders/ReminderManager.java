package com.robinzon.medicationwizard.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.robinzon.medicationwizard.database.DoseInstanceEntity;

import java.util.List;

/**
 * Orchestrator class responsible for managing Android System Alarms for medication reminders.
 * <p>
 * This class translates database records ({@link DoseInstanceEntity}) into low-level
 * {@link AlarmManager} schedules. It ensures that reminders are set accurately,
 * respecting modern Android background execution restrictions and power-saving modes.
 * </p>
 */
public class ReminderManager {

    /**
     * Batch schedules multiple medication reminders. 
     * Commonly used during boot-up or when multiple doses are created at once.
     *
     * @param context   The application context.
     * @param instances The list of dose entities to schedule.
     */
    /**
     * Helper to batch-schedule multiple medication dose alarms.
     */
    public static void scheduleReminders(Context context, List<DoseInstanceEntity> instances) {
        if (instances == null) return;
        for (DoseInstanceEntity instance : instances) {
            scheduleReminder(context, instance);
        }
    }

    /**
     * Schedules a single precise alarm for a specific medication dose.
     * <p>
     * Logic implemented:
     * 1. Validates that the dose is in "SCHEDULED" status and in the future.
     * 2. Packages medication data (name, amount, form) into a Broadcast Intent.
     * 3. Sets an Exact Alarm (on Android 12+) or a fallback inexact alarm if 
     *    permissions are missing.
     * </p>
     *
     * @param context  The application context.
     * @param instance The specific dose record to schedule.
     */
    /**
     * Schedules a precise system alarm for a single medication dose.
     * Use {@link android.app.AlarmManager} to trigger {@link ReminderReceiver} at the exact minute.
     *
     * @param context  Application context.
     * @param instance The dose instance defining the name and target time.
     */
    public static void scheduleReminder(Context context, DoseInstanceEntity instance) {
        if (instance == null || !"SCHEDULED".equals(instance.getStatus())) return;

        long time = instance.getScheduledTime();
        // Allow a 1-minute grace period for "now" reminders to account for processing time
        if (time < System.currentTimeMillis() - 60000) return;

        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(appContext, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_REMIND);
        intent.putExtra(ReminderReceiver.EXTRA_INSTANCE_ID, instance.getId());
        intent.putExtra(ReminderReceiver.EXTRA_MED_NAME, instance.getMedicationName());
        intent.putExtra(ReminderReceiver.EXTRA_AMOUNT, instance.getAmount());
        intent.putExtra(ReminderReceiver.EXTRA_FORM, instance.getForm());

        // Use instance.getId() as the requestCode to keep individual alarms unique
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                appContext,
                instance.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            com.robinzon.medicationwizard.utils.Logger.log("ReminderManager", "Scheduling alarm for " + instance.getMedicationName() + " at " + time);
            try {
                // Android 12 (API 31) introduced strict exact alarm permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
                    } else {
                        // Safety fallback if user hasn't granted "Schedule Exact Alarms" permission
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
                }
            } catch (SecurityException e) {
                // Fallback for Android 14+ if permission is revoked between the check and the call
                com.robinzon.medicationwizard.utils.Logger.log("ReminderManager", "SecurityException: Falling back to inexact alarm.");
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            }
        }
    }

    /**
     * Cancels an existing system alarm for a medication dose.
     * Useful when a dose is taken early, deleted, or rescheduled.
     *
     * @param context    The application context.
     * @param instanceId The unique ID of the dose instance (matches the requestCode).
     */
    /**
     * Cancels a previously scheduled alarm for a medication dose.
     *
     * @param context    Application context.
     * @param instanceId The unique ID of the dose instance.
     */
    public static void cancelReminder(Context context, int instanceId) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(appContext, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_REMIND);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                appContext,
                instanceId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            com.robinzon.medicationwizard.utils.Logger.log("ReminderManager", "Successfully cancelled alarm ID: " + instanceId);
        }

        // FIX: Also dismiss any active notification for this instance.
        com.robinzon.medicationwizard.notifications.NotificationManager.dismissNotification(appContext, instanceId);
    }
}