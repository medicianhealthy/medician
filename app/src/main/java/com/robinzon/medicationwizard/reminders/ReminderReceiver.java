package com.robinzon.medicationwizard.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.ui.settings.SettingsViewModel;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

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

            showNotification(context, medName, amount, form, instanceId);
            playAlertSound(context);
        }
    }

    private void showNotification(Context context, String medName, float amount, String form, int instanceId) {
        String amountStr = amount == (long) amount ? String.valueOf((long) amount) : String.valueOf(amount);
        String message = "Time to take " + amountStr + " " + (form != null ? form.toLowerCase() : "dose") + " of " + medName;

        Intent contentIntent = new Intent(context, MainActivity.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                context,
                instanceId,
                contentIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationManager.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_med_pill)
                .setContentTitle("Medication Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        try {
            manager.notify(instanceId, builder.build());
        } catch (SecurityException ignored) {}
    }

    private void playAlertSound(Context context) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        String uriStr = sp.getString(SettingsViewModel.KEY_NOTIF_SOUND_URI, "");
        boolean bypass = sp.getBoolean(SettingsViewModel.KEY_BYPASS_SYSTEM_VOLUME, false);
        int volumePercent = sp.getInt(SettingsViewModel.KEY_NOTIF_VOLUME, 70);

        if (uriStr.isEmpty()) return;

        try {
            Uri uri = Uri.parse(uriStr);
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(context, uri);

            if (bypass) {
                // Use ALARM stream to bypass DND/Silent if needed
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                
                float volume = volumePercent / 100f;
                player.setVolume(volume, volume);
            } else {
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            }

            player.prepare();
            player.start();
            player.setOnCompletionListener(MediaPlayer::release);
        } catch (Exception ignored) {}
    }
}