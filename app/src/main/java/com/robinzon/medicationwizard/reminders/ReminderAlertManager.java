package com.robinzon.medicationwizard.reminders;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.robinzon.medicationwizard.ui.settings.SettingsViewModel;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

/**
 * Singleton manager to coordinate reminder sounds.
 * Ensures only one sound plays at a time and allows stopping the sound from different entry points.
 */
public class ReminderAlertManager {

    private static ReminderAlertManager sInstance;
    private MediaPlayer mMediaPlayer;
    private Vibrator mVibrator;

    private ReminderAlertManager() {}

    public static synchronized ReminderAlertManager getInstance() {
        if (sInstance == null) {
            sInstance = new ReminderAlertManager();
        }
        return sInstance;
    }

    /**
     * Starts playing the reminder sound based on user settings.
     */
    public synchronized void startAlarm(Context context) {
        stopAlarm(); // Ensure previous alarm is stopped

        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        String uriStr = sp.getString(SettingsViewModel.KEY_NOTIF_SOUND_URI, "");
        Uri soundUri = uriStr.isEmpty() ? android.provider.Settings.System.DEFAULT_NOTIFICATION_URI : Uri.parse(uriStr);
        boolean bypassPref = sp.getBoolean(SettingsViewModel.KEY_BYPASS_SYSTEM_VOLUME, false);
        int volumePercent = sp.getInt(SettingsViewModel.KEY_NOTIF_VOLUME, 70);
        float volumeMultiplier = volumePercent / 100f;

        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(context, soundUri);
            if (bypassPref) {
                mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            } else {
                mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            }
            mMediaPlayer.setVolume(volumeMultiplier, volumeMultiplier);
            mMediaPlayer.setLooping(true); // Loop until user interacts
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            Logger.log("ReminderAlertManager", "Alarm started: %s", soundUri);

            // Handle Vibration
            startVibration(context, bypassPref);
        } catch (Exception e) {
            Logger.log("ReminderAlertManager", "Error starting alarm: %s", e.getMessage());
            releasePlayer();
        }
    }

    private void startVibration(Context context, boolean isAlarm) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        // Ensure we respect both the functional toggle and the premium pass
        boolean isVibrationEnabled = sp.getBoolean(SettingsViewModel.KEY_VIBRATION_ENABLED, false);
        
        if (!isVibrationEnabled) {
            Logger.log("ReminderAlertManager", "Vibration disabled in settings.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                mVibrator = vm.getDefaultVibrator();
            }
        } else {
            mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (mVibrator == null || !mVibrator.hasVibrator()) {
            Logger.log("ReminderAlertManager", "Vibrator not available on this device.");
            return;
        }

        String patternName = sp.getString(SettingsViewModel.KEY_VIBRATION_PATTERN, "Standard");
        long[] pattern = switch (patternName) {
            case "Heartbeat" -> new long[]{0, 200, 100, 200, 100, 200, 500};
            case "SOS" ->
                    new long[]{0, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 500};
            case "Long Pulse" -> new long[]{0, 800, 200, 800, 200};
            default -> new long[]{0, 500, 200, 500, 200};
        };

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(isAlarm ? AudioAttributes.USAGE_ALARM : AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        // Android 9+ (minSdk 28) supports VibrationEffect
        mVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), attrs);
        Logger.log("ReminderAlertManager", "Vibration started: %s (isAlarm: %b)", patternName, isAlarm);
    }

    /**
     * Stops and releases the active reminder sound.
     */
    public synchronized void stopAlarm() {
        if (mMediaPlayer != null) {
            try {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
            } catch (Exception ignored) {}
            releasePlayer();
            Logger.log("ReminderAlertManager", "Alarm stopped and released.");
        }
        
        if (mVibrator != null) {
            mVibrator.cancel();
            mVibrator = null;
            Logger.log("ReminderAlertManager", "Vibration stopped.");
        }
    }

    private void releasePlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}
