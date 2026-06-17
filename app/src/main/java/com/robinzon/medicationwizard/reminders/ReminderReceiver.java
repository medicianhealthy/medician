package com.robinzon.medicationwizard.reminders;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

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
 * <p>
 * This class is the "Action Engine" of the reminders. When a system alarm goes off, 
 * it wakes up this receiver to perform two critical tasks:
 * 1. Post a high-priority System Notification with medication details.
 * 2. Play the user's selected alert sound, potentially bypassing system volume if configured.
 * </p>
 */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_REMIND = "com.robinzon.medicationwizard.ACTION_REMIND";
    public static final String EXTRA_INSTANCE_ID = "extra_instance_id";
    public static final String EXTRA_MED_NAME = "extra_med_name";
    public static final String EXTRA_AMOUNT = "extra_amount";
    public static final String EXTRA_FORM = "extra_form";

    /**
     * Entry point triggered by the Android System Alarm.
     *
     * @param context The application context.
     * @param intent  The intent containing medication metadata (name, amount, form).
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_REMIND.equals(intent.getAction())) {
            String medName = intent.getStringExtra(EXTRA_MED_NAME);
            float amount = intent.getFloatExtra(EXTRA_AMOUNT, 0f);
            String form = intent.getStringExtra(EXTRA_FORM);
            int instanceId = intent.getIntExtra(EXTRA_INSTANCE_ID, 0);

            com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Alarm fired for: " + medName);
            
            SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
            boolean quietHoursEnabled = sp.getBoolean(SettingsViewModel.KEY_QUIET_HOURS_ENABLED, false);
            boolean inQuietHours = quietHoursEnabled && isInQuietHours(sp);

            showNotification(context, medName, amount, form, instanceId);
            
            if (!inQuietHours) {
                playAlertSound(context);
            } else {
                com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Quiet Hours active. Skipping alert sound.");
            }
        } else {
            com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Received unknown intent: " + intent.getAction());
        }
    }

    /**
     * Checks if the current time falls within the user-defined Quiet Hours.
     */
    private boolean isInQuietHours(SharedPreferencesManager sp) {
        String startStr = sp.getString(SettingsViewModel.KEY_QUIET_HOURS_START, "23:00");
        String endStr = sp.getString(SettingsViewModel.KEY_QUIET_HOURS_END, "07:00");

        try {
            String[] startParts = startStr.split(":");
            String[] endParts = endStr.split(":");

            int startHour = Integer.parseInt(startParts[0]);
            int startMin = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMin = Integer.parseInt(endParts[1]);

            Calendar now = Calendar.getInstance();
            int nowHour = now.get(Calendar.HOUR_OF_DAY);
            int nowMin = now.get(Calendar.MINUTE);

            int nowTotalMinutes = nowHour * 60 + nowMin;
            int startTotalMinutes = startHour * 60 + startMin;
            int endTotalMinutes = endHour * 60 + endMin;

            if (startTotalMinutes < endTotalMinutes) {
                // Same day range (e.g., 09:00 - 17:00)
                return nowTotalMinutes >= startTotalMinutes && nowTotalMinutes < endTotalMinutes;
            } else {
                // Overnight range (e.g., 22:00 - 07:00)
                return nowTotalMinutes >= startTotalMinutes || nowTotalMinutes < endTotalMinutes;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Constructs and displays a high-priority system notification.
     * <p>
     * Features:
     * - Personalized message (e.g., "Time to take 2 Pills of Aspirin").
     * - Tapping the notification opens the main app screen.
     * - Uses the dedicated "Medication Reminders" notification channel.
     * </p>
     */
    private void showNotification(Context context, String medName, float amount, String form, int instanceId) {
        String amountStr = amount == (long) amount ? String.valueOf((long) amount) : String.valueOf(amount);
        String message = "Time to take " + amountStr + " " + (form != null ? form.toLowerCase() : "dose") + " of " + medName;

        // 1. Content Intent (Open App)
        Intent contentIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                instanceId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 2. Action: Take
        Intent takeIntent = new Intent(context, NotificationActionReceiver.class);
        takeIntent.setAction(NotificationActionReceiver.ACTION_TAKE);
        takeIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_ID, instanceId);
        PendingIntent takePendingIntent = PendingIntent.getBroadcast(context, instanceId + 1000, takeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 3. Action: Snooze
        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction(NotificationActionReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_ID, instanceId);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, instanceId + 2000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 4. Action: Skip
        Intent skipIntent = new Intent(context, NotificationActionReceiver.class);
        skipIntent.setAction(NotificationActionReceiver.ACTION_SKIP);
        skipIntent.putExtra(NotificationActionReceiver.EXTRA_INSTANCE_ID, instanceId);
        PendingIntent skipPendingIntent = PendingIntent.getBroadcast(context, instanceId + 3000, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 5. Form-specific Large Icon
        int iconRes = R.drawable.ic_med_pill;
        if (form != null) {
            try {
                EForm eForm = EForm.valueOf(form);
                iconRes = switch (eForm) {
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
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), iconRes);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationManager.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_med_pill)
                .setLargeIcon(largeIcon)
                .setContentTitle("Medication Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_list, context.getString(R.string.take), takePendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history, context.getString(R.string.button_snooze), snoozePendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.button_skip), skipPendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        try {
            manager.notify(instanceId, builder.build());
        } catch (SecurityException ignored) {}
    }

    /**
     * Handles the audio playback for the reminder.
     * <p>
     * Advanced Logic:
     * 1. Fetches the user's custom ringtone URI from settings.
     * 2. If "Bypass System Volume" is enabled, it forces playback through the 
     *    ALARM stream and ignores the device's ringer mode.
     * 3. Scales the volume based on the user's preference (0-100%).
     * </p>
     */
    private void playAlertSound(Context context) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        String uriStr = sp.getString(SettingsViewModel.KEY_NOTIF_SOUND_URI, "");
        boolean bypass = sp.getBoolean(SettingsViewModel.KEY_BYPASS_SYSTEM_VOLUME, false);
        int volumePercent = sp.getInt(SettingsViewModel.KEY_NOTIF_VOLUME, 70);

        Uri uri;
        if (uriStr.isEmpty()) {
            // Fallback to system default notification sound
            uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
        } else {
            uri = Uri.parse(uriStr);
        }

        if (uri == null) return;

        // Run MediaPlayer preparation in a background thread to prevent Main-thread ANRs
        // especially if the URI is on a slow network or SD card.
        new Thread(() -> {
            try {
                MediaPlayer player = new MediaPlayer();
                player.setDataSource(context, uri);

                if (bypass) {
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

                // prepare() is a blocking I/O call.
                player.prepare();
                player.start();
                
                // Release resources once finished
                player.setOnCompletionListener(mp -> {
                    mp.release();
                    com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "MediaPlayer released.");
                });
                
                // Safety: Release if error occurs during playback
                player.setOnErrorListener((mp, what, extra) -> {
                    mp.release();
                    return true;
                });

            } catch (Exception e) {
                com.robinzon.medicationwizard.utils.Logger.log("ReminderReceiver", "Error playing sound: " + e.getMessage());
            }
        }).start();
    }
}