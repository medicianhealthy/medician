package com.robinzon.medicationwizard.reminders;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.hardware.camera2.CameraManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.entities.EForm;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.ui.settings.SettingsViewModel;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.Calendar;

/**
 * Background BroadcastReceiver that handles the "firing" of a medication reminder alarm.
 */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_REMIND = "com.robinzon.medicationwizard.ACTION_REMIND";
    public static final String EXTRA_INSTANCE_ID = "extra_instance_id";
    public static final String EXTRA_MED_NAME = "extra_med_name";
    public static final String EXTRA_AMOUNT = "extra_amount";
    public static final String EXTRA_FORM = "extra_form";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_REMIND.equals(intent.getAction())) {
            String medName = intent.getStringExtra(EXTRA_MED_NAME);
            float amount = intent.getFloatExtra(EXTRA_AMOUNT, 0f);
            String form = intent.getStringExtra(EXTRA_FORM);
            int instanceId = intent.getIntExtra(EXTRA_INSTANCE_ID, 0);

            com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Alarm fired for: " + medName);

            // Use goAsync to keep the receiver alive while playing sounds/vibrating/flashing
            final PendingResult pendingResult = goAsync();
            
            new Thread(() -> {
                try {
                    SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);

                    // Check for Quiet Hours entitlement
                    boolean unlockedQuietHours = com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(context, com.robinzon.medicationwizard.AppConfig.FeaturePassType.QUIET_HOURS);
                    boolean quietHoursEnabled = sp.getBoolean(SettingsViewModel.KEY_QUIET_HOURS_ENABLED, false);
                    boolean inQuietHours = unlockedQuietHours && quietHoursEnabled && isInQuietHours(sp);

                    showNotification(context, medName, amount, form, instanceId);

                    if (!inQuietHours) {
                        // Trigger alerts synchronously within this background thread
                        playAlertSoundSync(context);
                        triggerVibrationSync(context);
                        triggerFlashSync(context);
                    } else {
                        com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Quiet Hours active. Skipping alerts.");
                    }

                    // Consume any "Next Reminder" temporary passes
                    if (!com.robinzon.medicationwizard.AppConfig.isPremiumPurchased(context)) {
                        com.robinzon.medicationwizard.managers.FeaturePassManager.consumeNextReminderPasses(context);
                    }
                } catch (Exception e) {
                    com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Error in background processing: " + e.getMessage());
                } finally {
                    pendingResult.finish();
                }
            }).start();

        } else {
            com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Received unknown intent: " + intent.getAction());
        }
    }

    private boolean isInQuietHours(SharedPreferencesManager sp) {
        String startStr = sp.getString(SettingsViewModel.KEY_QUIET_HOURS_START, "23:00");
        String endStr = sp.getString(SettingsViewModel.KEY_QUIET_HOURS_END, "07:00");
        try {
            String[] startParts = startStr.split(":");
            String[] endParts = endStr.split(":");
            int startTotal = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
            int endTotal = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);
            Calendar now = Calendar.getInstance();
            int nowTotal = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            if (startTotal < endTotal) return nowTotal >= startTotal && nowTotal < endTotal;
            else return nowTotal >= startTotal || nowTotal < endTotal;
        } catch (Exception e) { return false; }
    }

    private void showNotification(Context context, String medName, float amount, String form, int instanceId) {
        String amountStr = amount == (long) amount ? String.valueOf((long) amount) : String.valueOf(amount);
        String message = "Time to take " + amountStr + " " + (form != null ? form.toLowerCase() : "dose") + " of " + medName;

        // Content Intent
        Intent contentIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, instanceId, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action Intents
        Intent takeIntent = new Intent(context, NotificationActionReceiver.class);
        takeIntent.setAction(NotificationActionReceiver.ACTION_TAKE);
        takeIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_ID, instanceId);
        PendingIntent takePI = PendingIntent.getBroadcast(context, instanceId + 1000, takeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction(NotificationActionReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_ID, instanceId);
        PendingIntent snoozePI = PendingIntent.getBroadcast(context, instanceId + 2000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent skipIntent = new Intent(context, NotificationActionReceiver.class);
        skipIntent.setAction(NotificationActionReceiver.ACTION_SKIP);
        skipIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_ID, instanceId);
        PendingIntent skipPI = PendingIntent.getBroadcast(context, instanceId + 3000, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int iconRes = R.drawable.ic_med_pill;
        if (form != null) {
            try {
                iconRes = switch (EForm.valueOf(form)) {
                    case Drops -> R.drawable.ic_med_drops;
                    case Injection -> R.drawable.ic_med_injection;
                    case Solution -> R.drawable.ic_med_solution;
                    case Inhaler -> R.drawable.ic_med_inhaler;
                    case Powder -> R.drawable.ic_med_powder;
                    case Other -> R.drawable.ic_med_other;
                    default -> R.drawable.ic_med_pill;
                };
            } catch (Exception ignored) {}
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationManager.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_med_pill)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), iconRes))
                .setContentTitle("Medication Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_list, context.getString(R.string.take), takePI)
                .addAction(android.R.drawable.ic_menu_recent_history, context.getString(R.string.button_snooze), snoozePI)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.button_skip), skipPI);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        try { manager.notify(instanceId, builder.build()); } catch (SecurityException ignored) {}
    }

    private void playAlertSoundSync(Context context) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        String uriStr = sp.getString(SettingsViewModel.KEY_NOTIF_SOUND_URI, "");
        boolean unlockedBypass = com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(context, com.robinzon.medicationwizard.AppConfig.FeaturePassType.BYPASS_VOLUME);
        boolean bypassPref = sp.getBoolean(SettingsViewModel.KEY_BYPASS_SYSTEM_VOLUME, false);
        boolean useBypass = unlockedBypass && bypassPref;
        int volumePercent = sp.getInt(SettingsViewModel.KEY_NOTIF_VOLUME, 70);

        Uri uri = uriStr.isEmpty() ? android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION) : Uri.parse(uriStr);
        if (uri == null) return;

        try {
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(context, uri);
            AudioAttributes.Builder attrs = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
            if (useBypass) {
                attrs.setUsage(AudioAttributes.USAGE_ALARM);
                float vol = volumePercent / 100f;
                player.setVolume(vol, vol);
            } else {
                attrs.setUsage(AudioAttributes.USAGE_NOTIFICATION);
            }
            player.setAudioAttributes(attrs.build());
            player.prepare();
            player.start();
            player.setOnCompletionListener(MediaPlayer::release);
        } catch (Exception e) {
            com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Sound error: " + e.getMessage());
        }
    }

    private void triggerVibrationSync(Context context) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        if (!sp.getBoolean(SettingsViewModel.KEY_VIBRATION_ENABLED, false)) return;
        if (!com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(context, com.robinzon.medicationwizard.AppConfig.FeaturePassType.VIBRATION)) return;

        String patternName = sp.getString(SettingsViewModel.KEY_VIBRATION_PATTERN, "Standard");
        long[] pattern = switch (patternName) {
            case "Heartbeat" -> new long[]{0, 150, 100, 150, 400};
            case "SOS" -> new long[]{0, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100};
            case "Long Pulse" -> new long[]{0, 800, 200, 800};
            default -> new long[]{0, 300, 200, 300, 200};
        };

        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1), audioAttributes);
        }
    }

    private void triggerFlashSync(Context context) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        String patternName = sp.getString(SettingsViewModel.KEY_FLASH_PATTERN, "None");
        if (patternName == null || "None".equalsIgnoreCase(patternName)) return;
        if (!com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(context, com.robinzon.medicationwizard.AppConfig.FeaturePassType.VIBRATION)) return;

        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) return;

        try {
            String[] ids = cameraManager.getCameraIdList();
            if (ids.length == 0) return;
            String cameraId = ids[0];
            int blinks; long onMs, offMs;
            switch (patternName) {
                case "Single Blink" -> { blinks = 1; onMs = 500; offMs = 500; }
                case "Double Pulse" -> { blinks = 2; onMs = 200; offMs = 200; }
                case "Strobe" -> { blinks = 10; onMs = 50; offMs = 50; }
                default -> { return; }
            }
            for (int k = 0; k < blinks; k++) {
                cameraManager.setTorchMode(cameraId, true);
                Thread.sleep(onMs);
                cameraManager.setTorchMode(cameraId, false);
                if (k < blinks - 1) Thread.sleep(offMs);
            }
        } catch (Exception e) {
            com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Flash error: " + e.getMessage());
        }
    }
}
