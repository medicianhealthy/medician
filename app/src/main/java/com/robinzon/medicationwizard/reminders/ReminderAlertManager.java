package com.robinzon.medicationwizard.reminders;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.robinzon.medicationwizard.ui.settings.SettingsViewModel;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages reminder sounds and vibrations for the application.
 * Ensures only one sound plays at a time and allows stopping the sound from different entry points.
 */
public class ReminderAlertManager {

    private static final long ALARM_TIMEOUT_MS = 10000; // 10 seconds safety timeout

    private static ReminderAlertManager sInstance;
    private MediaPlayer mMediaPlayer;
    private Vibrator mVibrator;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mTimeoutRunnable = this::stopAlarm;
    private final List<OnAlarmStateChangedListener> mListeners = new ArrayList<>();

    private ReminderAlertManager() {}

    /**
     * Retrieves the singleton instance of the ReminderAlertManager.
     *
     * @return The singleton instance.
     */
    public static synchronized ReminderAlertManager getInstance() {
        if (sInstance == null) {
            sInstance = new ReminderAlertManager();
        }
        return sInstance;
    }

    /**
     * Starts playing the reminder sound based on user settings.
     *
     * @param context The application context.
     */
    public synchronized void startAlarm(Context context) {
        startAlarm(context, false);
    }

    /**
     * Starts playing the reminder sound and triggers vibrations.
     * <p>
     * This handles configuring the MediaPlayer, checking bypass preferences, setting volume levels,
     * and ensuring a timeout is scheduled to prevent alarms from playing indefinitely.
     * </p>
     *
     * @param context    The application context.
     * @param isCritical Indicates if the alarm is for a critical medication, altering sound and vibration behavior.
     */
    public synchronized void startAlarm(Context context, boolean isCritical) {
        stopAlarm(); // Ensure previous alarm is stopped

        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        String uriStr = sp.getString(SettingsViewModel.KEY_NOTIF_SOUND_URI, "");
        Uri soundUri = uriStr.isEmpty() ? android.provider.Settings.System.DEFAULT_NOTIFICATION_URI : Uri.parse(uriStr);
        boolean bypassPref = isCritical || sp.getBoolean(SettingsViewModel.KEY_BYPASS_SYSTEM_VOLUME, false);
        int volumePercent = isCritical ? 85 : sp.getInt(SettingsViewModel.KEY_NOTIF_VOLUME, 70);
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
            mMediaPlayer.setLooping(isCritical); // Loop if critical
            mMediaPlayer.setOnCompletionListener(mp -> {
                if (!isCritical) stopAlarm();
            });
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            Logger.log("ReminderAlertManager", "Alarm started: %s (Critical: %b)", soundUri, isCritical);

            notifyListeners(true);

            // Handle Vibration
            startVibration(context, bypassPref, isCritical);

            // Safety timeout: 30 seconds for critical, 10 for normal
            mHandler.postDelayed(mTimeoutRunnable, isCritical ? 30000 : ALARM_TIMEOUT_MS);
        } catch (Exception e) {
            Logger.log("ReminderAlertManager", "Error starting alarm: %s", e.getMessage());
            releasePlayer();
        }
    }

    /**
     * Triggers the vibration pattern associated with the alarm state.
     *
     * @param context    The application context.
     * @param isAlarm    Specifies if the vibration uses the ALARM audio attributes.
     * @param isCritical Specifies if the urgent heartbeat pattern should be prioritized.
     */
    private void startVibration(Context context, boolean isAlarm, boolean isCritical) {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        // Ensure we respect both the functional toggle and the premium pass
        boolean isVibrationEnabled = isCritical || sp.getBoolean(SettingsViewModel.KEY_VIBRATION_ENABLED, false);
        
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

        String patternName = isCritical ? "Urgent Heartbeat" : sp.getString(SettingsViewModel.KEY_VIBRATION_PATTERN, "Standard");
        long[] pattern = switch (patternName) {
            case "Urgent Heartbeat" -> new long[]{0, 150, 100, 150, 500};
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
        mVibrator.vibrate(VibrationEffect.createWaveform(pattern, isCritical ? 0 : -1), attrs);
        Logger.log("ReminderAlertManager", "Vibration started: %s (isAlarm: %b)", patternName, isAlarm);
    }

    /**
     * Stops and releases the active reminder sound and vibration.
     */
    public synchronized void stopAlarm() {
        notifyListeners(false);
        mHandler.removeCallbacks(mTimeoutRunnable);

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

        notifyListeners(false);
    }

    /**
     * Safely releases the internal media player resources.
     */
    private void releasePlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * Checks whether a reminder sound is currently playing.
     *
     * @return True if a reminder sound is currently playing, false otherwise.
     */
    public synchronized boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    /**
     * Subscribes a listener to alarm state changes.
     *
     * @param listener The callback interface for alarm state updates.
     */
    public synchronized void addListener(OnAlarmStateChangedListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * Unsubscribes a listener from alarm state changes.
     *
     * @param listener The callback interface to remove.
     */
    public synchronized void removeListener(OnAlarmStateChangedListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Dispatches the current alarm playing state to all registered listeners on the main thread.
     *
     * @param isPlaying The current playback state.
     */
    private void notifyListeners(boolean isPlaying) {
        new Handler(Looper.getMainLooper()).post(() -> {
            synchronized (this) {
                for (OnAlarmStateChangedListener listener : mListeners) {
                    listener.onAlarmStateChanged(isPlaying);
                }
            }
        });
    }

    /**
     * Interface definition for a callback to be invoked when the alarm playback state changes.
     */
    public interface OnAlarmStateChangedListener {
        /**
         * Called when the alarm starts or stops playing.
         *
         * @param isPlaying True if playing, false if stopped.
         */
        void onAlarmStateChanged(boolean isPlaying);
    }
}
