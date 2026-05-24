package com.robinzon.medicationwizard.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                long now = System.currentTimeMillis();
                List<DoseInstanceEntity> pending = AppDatabase.getDatabase(context)
                        .doseInstanceDao().getInstancesInRangeInternal(now, now + 8 * 24 * 60 * 60 * 1000L);

                ReminderManager.scheduleReminders(context, pending);
            });
        }
    }
}