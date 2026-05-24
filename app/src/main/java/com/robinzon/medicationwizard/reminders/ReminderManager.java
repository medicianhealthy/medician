package com.robinzon.medicationwizard.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.robinzon.medicationwizard.database.DoseInstanceEntity;

import java.util.List;

public class ReminderManager {

    public static void scheduleReminders(Context context, List<DoseInstanceEntity> instances) {
        if (instances == null) return;
        for (DoseInstanceEntity instance : instances) {
            scheduleReminder(context, instance);
        }
    }

    public static void scheduleReminder(Context context, DoseInstanceEntity instance) {
        if (instance == null || !"SCHEDULED".equals(instance.getStatus())) return;
        
        long time = instance.getScheduledTime();
        if (time < System.currentTimeMillis()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_REMIND);
        intent.putExtra(ReminderReceiver.EXTRA_INSTANCE_ID, instance.getId());
        intent.putExtra(ReminderReceiver.EXTRA_MED_NAME, instance.getMedicationName());
        intent.putExtra(ReminderReceiver.EXTRA_AMOUNT, instance.getAmount());
        intent.putExtra(ReminderReceiver.EXTRA_FORM, instance.getForm());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                instance.getId(), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            }
        }
    }

    public static void cancelReminder(Context context, int instanceId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_REMIND);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                instanceId, 
                intent, 
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}