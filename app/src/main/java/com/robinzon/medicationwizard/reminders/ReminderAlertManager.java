package com.robinzon.medicationwizard.reminders;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

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
            Logger.log("ReminderAlertManager", "Alarm started: " + soundUri);
        } catch (Exception e) {
            Logger.log("ReminderAlertManager", "Error starting alarm: " + e.getMessage());
            releasePlayer();
        }
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
    }

    private void releasePlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}
