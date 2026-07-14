package com.robinzon.medicationwizard.managers;

import android.content.Context;

import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.Calendar;

/**
 * Logic controller for granting and managing feature-specific premium passes.
 */
public class FeaturePassManager {

    private static final long HOUR_IN_MILLIS = 3600000L;

    /**
     * Grants a temporary pass for a specific feature upon successful RV completion.
     */
    public static void grantPass(Context context, AppConfig.FeaturePassType feature) {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(context);
        long now = System.currentTimeMillis();

        switch (feature) {
            case THEME -> prefs.setLong(AppConfig.KEY_PASS_THEME_EXPIRY, now + HOUR_IN_MILLIS);
            case SUPPORT -> prefs.setLong(AppConfig.KEY_PASS_SUPPORT_EXPIRY, now + HOUR_IN_MILLIS);
            case BACKUP -> prefs.setLong(AppConfig.KEY_PASS_BACKUP_EXPIRY, now + HOUR_IN_MILLIS);
            case DOSE_WINDOW -> prefs.setBoolean(AppConfig.KEY_PASS_DOSE_WINDOW_ACTIVE, true);
            case BYPASS_VOLUME -> prefs.setBoolean(AppConfig.KEY_PASS_BYPASS_VOLUME_ACTIVE, true);
            case VIBRATION -> prefs.setBoolean(AppConfig.KEY_PASS_VIBRATION_ACTIVE, true);
            case STICKY_NOTIF -> prefs.setBoolean(AppConfig.KEY_PASS_STICKY_ACTIVE, true);
            case QUIET_HOURS -> {
                // Unlock until next morning (8:00 AM)
                Calendar cal = Calendar.getInstance();
                if (cal.get(Calendar.HOUR_OF_DAY) >= 8) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
                cal.set(Calendar.HOUR_OF_DAY, 8);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                prefs.setLong(AppConfig.KEY_PASS_QUIET_HOURS_EXPIRY, cal.getTimeInMillis());
            }
        }
    }

    /**
     * Consumes all "Next Reminder" temporary passes.
     * Should be called when a reminder alarm fires.
     */
    public static void consumeNextReminderPasses(Context context) {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(context);
        prefs.setBoolean(AppConfig.KEY_PASS_BYPASS_VOLUME_ACTIVE, false);
        prefs.setBoolean(AppConfig.KEY_PASS_VIBRATION_ACTIVE, false);
        prefs.setBoolean(AppConfig.KEY_PASS_STICKY_ACTIVE, false);

        // Also explicitly disable the functional switches in preferences
        // to ensure the UI stays in sync after consumption.
        prefs.setBoolean("bypass_system_volume", false);
        prefs.setBoolean("vibration_enabled", false);
        prefs.setBoolean("sticky_notif_enabled", false);
    }

    /**
     * Consumes a specific "Marked Taken" pass.
     */
    public static void consumeDoseWindowPass(Context context) {
        SharedPreferencesManager.getInstance(context).setBoolean(AppConfig.KEY_PASS_DOSE_WINDOW_ACTIVE, false);
    }

    @Deprecated
    public static void consumeBypassVolumePass(Context context) {
        SharedPreferencesManager.getInstance(context).setBoolean(AppConfig.KEY_PASS_BYPASS_VOLUME_ACTIVE, false);
    }
}
