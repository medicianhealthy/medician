package com.robinzon.medicationwizard.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;

import java.util.List;

/**
 * BroadcastReceiver responsible for restoring alarms after a device reboot.
 * <p>
 * On Android, all {@link android.app.AlarmManager} schedules are lost when the device is
 * turned off. This receiver catches the {@code BOOT_COMPLETED} signal and
 * re-schedules all future medication doses stored in the Room database to ensure
 * the user continues to receive reminders.
 * </p>
 */
public class BootReceiver extends BroadcastReceiver {

    /**
     * Entry point triggered by the system boot sequence.
     * <p>
     * Operation:
     * 1. Confirms the intent is indeed a device reboot completion.
     * 2. Fetches all future "SCHEDULED" doses from the database (looking up to 8 days ahead).
     * 3. Passes these doses to {@link ReminderManager} to restore the system alarms.
     * </p>
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                long now = System.currentTimeMillis();
                // Buffer of 8 days to catch everything scheduled by our current NUMBER_OF_DAYS_TO_SCHEDULE logic
                List<DoseInstanceEntity> pending = AppDatabase.getDatabase(context)
                        .doseInstanceDao().getInstancesInRangeInternal(now, now + 8 * 24 * 60 * 60 * 1000L);

                ReminderManager.scheduleReminders(context, pending);
            });
        }
    }
}