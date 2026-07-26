package com.robinzon.medicationwizard.reminders;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.entities.EForm;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.ui.settings.SettingsViewModel;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_REMIND = "com.robinzon.medicationwizard.ACTION_REMIND";
    public static final String EXTRA_INSTANCE_ID = "extra_instance_id";
    public static final String EXTRA_MED_NAME = "extra_med_name";
    public static final String EXTRA_AMOUNT = "extra_amount";
    public static final String EXTRA_FORM = "extra_form";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_REMIND.equals(intent.getAction())) return;

        int instanceId = intent.getIntExtra(EXTRA_INSTANCE_ID, -1);
        if (instanceId == -1) return;

        com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Alarm received for ID: " + instanceId);

        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        boolean quietHoursEnabled = sp.getBoolean(SettingsViewModel.KEY_QUIET_HOURS_ENABLED, false);
        if (quietHoursEnabled && isInQuietHours(sp)) {
            com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Silent during quiet hours.");
            return;
        }

        final PendingResult pendingResult = goAsync();

        // Logic for unified notifications:
        // 1. Fetch all medications due at the exact same time
        new Thread(() -> {
            try {
                com.robinzon.medicationwizard.database.AppDatabase db = com.robinzon.medicationwizard.database.AppDatabase.getDatabase(context);
                com.robinzon.medicationwizard.database.DoseInstanceEntity current = db.doseInstanceDao().getInstanceById(instanceId);
                
                if (current == null) {
                    com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Error: Dose instance not found in DB: " + instanceId);
                    return;
                }

                long scheduledTime = current.getScheduledTime();
                java.util.List<com.robinzon.medicationwizard.database.DoseInstanceEntity> dosesAtTime = db.doseInstanceDao().getScheduledAtTime(scheduledTime);

                com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Unified check: Found " + dosesAtTime.size() + " doses for time " + scheduledTime);

                // 2. Show notification (unified or single)
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> {
                    try {
                        showNotification(context, dosesAtTime, scheduledTime);
                    } catch (Exception e) {
                        com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Error showing notification: " + e.getMessage());
                    }
                });

                // 3. Effects
                ReminderAlertManager.getInstance().startAlarm(context);
                triggerFlashSync(context);
                com.robinzon.medicationwizard.managers.FeaturePassManager.consumeNextReminderPasses(context);
            } catch (Exception e) {
                com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Background error: " + e.getMessage());
            } finally {
                if (pendingResult != null) {
                    pendingResult.finish();
                }
            }
        }).start();
    }

    private boolean isInQuietHours(SharedPreferencesManager sp) {
        String startStr = sp.getString(SettingsViewModel.KEY_QUIET_HOURS_START, "23:00");
        String endStr = sp.getString(SettingsViewModel.KEY_QUIET_HOURS_END, "07:00");

        try {
            String[] startParts = startStr.split(":");
            String[] endParts = endStr.split(":");
            int startH = Integer.parseInt(startParts[0]), startM = Integer.parseInt(startParts[1]);
            int endH = Integer.parseInt(endParts[0]), endM = Integer.parseInt(endParts[1]);

            Calendar now = Calendar.getInstance();
            int nowH = now.get(Calendar.HOUR_OF_DAY), nowM = now.get(Calendar.MINUTE);
            int nowTotal = nowH * 60 + nowM;
            int startTotal = startH * 60 + startM;
            int endTotal = endH * 60 + endM;

            if (startTotal < endTotal) return nowTotal >= startTotal && nowTotal < endTotal;
            else return nowTotal >= startTotal || nowTotal < endTotal;
        } catch (Exception e) {
            return false;
        }
    }

    private void showNotification(Context context, java.util.List<com.robinzon.medicationwizard.database.DoseInstanceEntity> doses, long scheduledTime) {
        if (doses == null || doses.isEmpty()) return;

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        String channelId = NotificationManager.CHANNEL_ID;

        // Stable notification ID based on the exact minute
        int notificationId = (int) (scheduledTime / 60000);

        String title;
        String message;
        int[] instanceIds = new int[doses.size()];

        if (doses.size() == 1) {
            com.robinzon.medicationwizard.database.DoseInstanceEntity dose = doses.get(0);
            title = context.getString(R.string.notification_reminder_title);
            message = formatDoseMessage(context, dose);
            instanceIds[0] = dose.getId();
        } else {
            title = context.getString(R.string.notification_reminder_title) + " (" + doses.size() + ")";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < doses.size(); i++) {
                com.robinzon.medicationwizard.database.DoseInstanceEntity dose = doses.get(i);
                sb.append("• ").append(formatDoseMessage(context, dose));
                if (i < doses.size() - 1) sb.append("\n");
                instanceIds[i] = dose.getId();
            }
            message = sb.toString();
        }

        // Content Intent
        Intent contentIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action Intents
        Intent takeIntent = new Intent(context, NotificationActionReceiver.class);
        takeIntent.setAction(doses.size() > 1 ? NotificationActionReceiver.ACTION_TAKE_ALL : NotificationActionReceiver.ACTION_TAKE);
        takeIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_ID, instanceIds[0]);
        takeIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_IDS, instanceIds);
        takeIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        PendingIntent takePI = PendingIntent.getBroadcast(context, notificationId + 1, takeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction(doses.size() > 1 ? NotificationActionReceiver.ACTION_SNOOZE_ALL : NotificationActionReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_ID, instanceIds[0]);
        snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_IDS, instanceIds);
        snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        PendingIntent snoozePI = PendingIntent.getBroadcast(context, notificationId + 2, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent skipIntent = new Intent(context, NotificationActionReceiver.class);
        skipIntent.setAction(doses.size() > 1 ? NotificationActionReceiver.ACTION_SKIP_ALL : NotificationActionReceiver.ACTION_SKIP);
        skipIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_ID, instanceIds[0]);
        skipIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_IDS, instanceIds);
        skipIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        PendingIntent skipPI = PendingIntent.getBroadcast(context, notificationId + 3, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        boolean stickyEnabled = sp.getBoolean(SettingsViewModel.KEY_STICKY_NOTIF_ENABLED, false);

        // Delete Intent: Stop alarm if user swipes away the notification
        Intent stopIntent = new Intent(context, NotificationActionReceiver.class);
        stopIntent.setAction(NotificationActionReceiver.ACTION_STOP_ALARM);
        PendingIntent stopPI = PendingIntent.getBroadcast(context, notificationId + 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_med_pill)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(doses.size() == 1 ? message : context.getString(R.string.times_a_day, doses.size()))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(stopPI)
                .setAutoCancel(true)
                .setOngoing(stickyEnabled)
                .addAction(R.drawable.ic_done_pill, doses.size() > 1 ? context.getString(R.string.button_take_all) : context.getString(R.string.button_take), takePI)
                .addAction(R.drawable.ic_clock, context.getString(R.string.button_snooze), snoozePI)
                .addAction(R.drawable.ic_med_other, doses.size() > 1 ? context.getString(R.string.button_skip_all) : context.getString(R.string.button_skip), skipPI);

        try {
            nm.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    private String formatDoseMessage(Context context, com.robinzon.medicationwizard.database.DoseInstanceEntity dose) {
        float amount = dose.getAmount();
        String amountStr = amount == (long) amount ? String.valueOf((long) amount) : String.valueOf(amount);
        String formStr;
        if (dose.getForm() != null) {
            try {
                EForm eForm = EForm.valueOf(dose.getForm());
                formStr = eForm.getLabel(context);
            } catch (Exception e) {
                formStr = dose.getForm();
            }
        } else {
            formStr = context.getString(R.string.notification_reminder_dose);
        }
        return context.getString(R.string.notification_reminder_message, amountStr, formStr, dose.getMedicationName());
    }

    private void triggerFlashSync(Context context) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        String patternName = sp.getString(SettingsViewModel.KEY_FLASH_PATTERN, "None");
        if ("None".equals(patternName)) return;

        CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cm.getCameraIdList()[0];
            int flashes = switch (patternName) {
                case "Single Blink" -> 1;
                case "Double Pulse" -> 2;
                case "Strobe" -> 5;
                default -> 0;
            };

            for (int i = 0; i < flashes; i++) {
                cm.setTorchMode(cameraId, true);
                Thread.sleep(150);
                cm.setTorchMode(cameraId, false);
                Thread.sleep(150);
            }
        } catch (Exception ignored) {
        }
    }
}