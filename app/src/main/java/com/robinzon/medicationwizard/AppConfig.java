package com.robinzon.medicationwizard;

import android.content.Context;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

/**
 * Global application configuration and entitlement manager.
 * <p>
 * This class handles both permanent Premium status (purchased) and temporary 
 * Feature-Specific Passes (unlocked via Rewarded Video).
 * </p>
 */
public class AppConfig {
    public static final int NUMBER_OF_DAYS_TO_SCHEDULE = 7;
    
    /** Master flag for purchased Premium status. */
    public static boolean IS_PREMIUM = false;

    /** Toggle for enabling/disabling the Cloud Backup feature globally. */
    public static final boolean CLOUD_BACKUP_ENABLED = false;
    
    /** Developer cheat flag to force ads visibility for testing. */
    public static boolean FORCED_ADS_VISIBLE = false;

    // --- Persistence Keys ---
    public static final String KEY_CHEAT_PREMIUM = "cheat_is_premium";
    public static final String KEY_CHEAT_SHOW_ADS = "cheat_show_ads";
    
    /** @deprecated Use individual feature pass keys instead. */
    @Deprecated
    public static final String KEY_TEMP_PREMIUM_EXPIRY = "temp_premium_expiry";
    
    public static final String KEY_PASS_THEME_EXPIRY = "pass_theme_expiry";
    public static final String KEY_PASS_SUPPORT_EXPIRY = "pass_support_expiry";
    public static final String KEY_PASS_BACKUP_EXPIRY = "pass_backup_expiry";
    public static final String KEY_PASS_QUIET_HOURS_EXPIRY = "pass_quiet_hours_expiry";
    public static final String KEY_PASS_BYPASS_VOLUME_ACTIVE = "pass_bypass_volume_active";
    public static final String KEY_PASS_VIBRATION_ACTIVE = "pass_vibration_active";
    public static final String KEY_PASS_STICKY_ACTIVE = "pass_sticky_active";
    public static final String KEY_PASS_DOSE_WINDOW_EXPIRY = "pass_dose_window_expiry";

    /**
     * @return True if the user has purchased the full version of the app.
     */
    public static boolean isPremiumPurchased(Context context) {
        if (IS_PREMIUM) return true;
        // Check standard purchase cache (populated by BillingManager)
        return SharedPreferencesManager.getInstance(context).getBoolean("cached_premium_status", false);
    }

    /**
     * Checks if a specific feature is currently unlocked (either via purchase or active pass).
     */
    public static boolean isFeatureUnlocked(Context context, FeaturePassType feature) {
        if (isPremiumPurchased(context)) return true;
        
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(context);
        long now = System.currentTimeMillis();

        return switch (feature) {
            case THEME -> prefs.getLong(KEY_PASS_THEME_EXPIRY, 0) > now;
            case SUPPORT -> prefs.getLong(KEY_PASS_SUPPORT_EXPIRY, 0) > now;
            case BACKUP -> prefs.getLong(KEY_PASS_BACKUP_EXPIRY, 0) > now;
            case QUIET_HOURS -> prefs.getLong(KEY_PASS_QUIET_HOURS_EXPIRY, 0) > now;
            case BYPASS_VOLUME -> prefs.getBoolean(KEY_PASS_BYPASS_VOLUME_ACTIVE, false);
            case VIBRATION -> prefs.getBoolean(KEY_PASS_VIBRATION_ACTIVE, false);
            case STICKY_NOTIF -> prefs.getBoolean(KEY_PASS_STICKY_ACTIVE, false);
            case DOSE_WINDOW -> prefs.getLong(KEY_PASS_DOSE_WINDOW_EXPIRY, 0) > now;
        };
    }

    /**
     * Legacy method for ad-eligibility. Purchase or active Magic Pass (deprecated) 
     * should hide standard ads.
     */
    public static boolean isPremium(Context context) {
        return isPremiumPurchased(context);
    }

    public static int getHistoryRetentionDays() {
        return com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getHistoryRetentionDays();
    }

    /** Enum to distinguish between premium features for pass logic. */
    public enum FeaturePassType {
        THEME, SUPPORT, BACKUP, QUIET_HOURS, BYPASS_VOLUME, VIBRATION, STICKY_NOTIF, DOSE_WINDOW
    }

    /**
     * Generates a human-readable label for a feature's active pass status.
     * 
     * @param context Application context for string resources.
     * @param feature The feature type to check.
     * @return A string like "Active until 14:00" or "Active for next reminder", or empty if not active.
     */
    public static String getFeatureExpiryLabel(Context context, FeaturePassType feature) {
        if (isPremiumPurchased(context)) return ""; 

        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(context);

        return switch (feature) {
            case THEME -> formatExpiry(context, prefs.getLong(KEY_PASS_THEME_EXPIRY, 0));
            case SUPPORT -> formatExpiry(context, prefs.getLong(KEY_PASS_SUPPORT_EXPIRY, 0));
            case BACKUP -> formatExpiry(context, prefs.getLong(KEY_PASS_BACKUP_EXPIRY, 0));
            case QUIET_HOURS -> formatExpiry(context, prefs.getLong(KEY_PASS_QUIET_HOURS_EXPIRY, 0));
            case DOSE_WINDOW -> formatExpiry(context, prefs.getLong(KEY_PASS_DOSE_WINDOW_EXPIRY, 0));
            
            case BYPASS_VOLUME -> prefs.getBoolean(KEY_PASS_BYPASS_VOLUME_ACTIVE, false) ? 
                    context.getString(R.string.active_for_next_reminder) : "";
            case VIBRATION -> prefs.getBoolean(KEY_PASS_VIBRATION_ACTIVE, false) ? 
                    context.getString(R.string.active_for_next_reminder) : "";
            case STICKY_NOTIF -> prefs.getBoolean(KEY_PASS_STICKY_ACTIVE, false) ? 
                    context.getString(R.string.active_for_next_reminder) : "";
        };
    }

    private static String formatExpiry(Context context, long expiry) {
        if (expiry <= System.currentTimeMillis()) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return context.getString(R.string.active_until_format, sdf.format(new java.util.Date(expiry)));
    }
}
