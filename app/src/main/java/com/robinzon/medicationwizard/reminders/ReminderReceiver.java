package com.robinzon.medicationwizard.reminders;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
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
        String medName = intent.getStringExtra(EXTRA_MED_NAME);
        float amount = intent.getFloatExtra(EXTRA_AMOUNT, 1.0f);
        String form = intent.getStringExtra(EXTRA_FORM);

        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        boolean quietHoursEnabled = sp.getBoolean(SettingsViewModel.KEY_QUIET_HOURS_ENABLED, false);
        if (quietHoursEnabled && isInQuietHours(sp)) {
            // Log or silent handle
            return;
        }

        showNotification(context, medName, amount, form, instanceId);
        
        // Effects (Sound, Vibration, Flash)
        new Thread(() -> {
            playAlertSoundSync(context);
            triggerVibrationSync(context);
            triggerFlashSync(context);
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
        } catch (Exception e) { return false; }
    }

    private void showNotification(Context context, String medName, float amount, String form, int instanceId) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        String channelId = "med_reminders";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, context.getString(R.string.notification_channel_name), android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.notification_channel_desc));
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(channel);
        }

        String amountStr = amount == (long) amount ? String.valueOf((long) amount) : String.valueOf(amount);
        String formStr;
        if (form != null) {
            try {
                EForm eForm = EForm.valueOf(form);
                formStr = eForm.getLabel(context);
            } catch (Exception e) {
                formStr = form;
            }
        } else {
            formStr = context.getString(R.string.notification_reminder_dose);
        }
        
        String message = context.getString(R.string.notification_reminder_message, amountStr, formStr, medName);

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

        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        boolean stickyEnabled = sp.getBoolean(SettingsViewModel.KEY_STICKY_NOTIF_ENABLED, false);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_med_pill)
                .setContentTitle(context.getString(R.string.notification_reminder_title))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(stickyEnabled)
                .addAction(R.drawable.ic_done_pill, context.getString(R.string.button_take), takePI)
                .addAction(R.drawable.ic_clock, context.getString(R.string.button_snooze), snoozePI)
                .addAction(R.drawable.ic_med_other, context.getString(R.string.button_skip), skipPI);

        try { nm.notify(instanceId, builder.build()); } catch (SecurityException ignored) {}
    }

    private void playAlertSoundSync(Context context) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        String uriStr = sp.getString(SettingsViewModel.KEY_NOTIF_SOUND_URI, "");
        Uri soundUri = uriStr.isEmpty() ? android.provider.Settings.System.DEFAULT_NOTIFICATION_URI : Uri.parse(uriStr);
        boolean bypassPref = sp.getBoolean(SettingsViewModel.KEY_BYPASS_SYSTEM_VOLUME, false);
        
        int volumePercent = sp.getInt(SettingsViewModel.KEY_NOTIF_VOLUME, 70);
        float volume = volumePercent / 100f;

        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(context, soundUri);
            if (bypassPref) {
                mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            } else {
                mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            }
            mp.setVolume(volume, volume);
            mp.prepare();
            mp.start();
            mp.setOnCompletionListener(MediaPlayer::release);
        } catch (Exception e) { mp.release(); }
    }

    private void triggerVibrationSync(Context context) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        if (!sp.getBoolean(SettingsViewModel.KEY_VIBRATION_ENABLED, false)) return;

        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator == null || !vibrator.hasVibrator()) return;

        String patternName = sp.getString(SettingsViewModel.KEY_VIBRATION_PATTERN, "Standard");
        long[] pattern = switch (patternName) {
            case "Heartbeat" -> new long[]{0, 200, 100, 200, 100, 200, 500};
            case "SOS" -> new long[]{0, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 500};
            case "Long Pulse" -> new long[]{0, 800, 200, 800, 200};
            default -> new long[]{0, 500, 200, 500, 200};
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
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
        } catch (Exception ignored) {}
    }
}