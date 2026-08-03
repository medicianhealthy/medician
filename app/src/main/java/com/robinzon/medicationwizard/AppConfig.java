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
    /**
     * Toggle for enabling/disabling the Cloud Backup feature globally.
     */
    public static final boolean CLOUD_BACKUP_ENABLED = false;
    // --- Persistence Keys ---
    public static final String KEY_CHEAT_PREMIUM = "cheat_is_premium";
    public static final String KEY_CHEAT_SHOW_ADS = "cheat_show_ads";
    /**
     * @deprecated Use individual feature pass keys instead.
     */
    @Deprecated
    public static final String KEY_TEMP_PREMIUM_EXPIRY = "temp_premium_expiry";
    public static final String KEY_PASS_THEME_EXPIRY = "pass_theme_expiry";
    public static final String KEY_PASS_SUPPORT_EXPIRY = "pass_support_expiry";
    public static final String KEY_PASS_BACKUP_EXPIRY = "pass_backup_expiry";
    public static final String KEY_PASS_QUIET_HOURS_EXPIRY = "pass_quiet_hours_expiry";
    public static final String KEY_PASS_BYPASS_VOLUME_ACTIVE = "pass_bypass_volume_active";
    public static final String KEY_PASS_VIBRATION_ACTIVE = "pass_vibration_active";
    public static final String KEY_PASS_STICKY_ACTIVE = "pass_sticky_active";
    public static final String KEY_PASS_DOSE_WINDOW_ACTIVE = "pass_dose_window_active";
    public static final String KEY_PASS_PHOTO_EXPIRY = "pass_photo_expiry";
    public static final String KEY_PASS_AD_FREE_EXPIRY = "pass_ad_free_expiry";

    public static final String KEY_MAGIC_BALANCE = "magic_balance";
    public static final String KEY_PERMANENT_PASS_PREFIX = "permanent_pass_";
    public static final String KEY_MEDS_SLOTS_UNLOCKED = "meds_slots_unlocked";

    // Magic Economy Costs
    public static final int MAGIC_COST_PASS_1H = 1;
    public static final int MAGIC_COST_AD_FREE_1H = 3;
    public static final int MAGIC_COST_PERMANENT = 15;
    public static final int MAGIC_COST_EXTRA_MED_SLOT = 5;

    public static final int FREE_MED_LIMIT = 3;

    /**
     * Master flag for purchased Premium status.
     */
    public static boolean IS_PREMIUM = false;
    /**
     * Developer cheat flag to force ads visibility for testing.
     */
    public static boolean FORCED_ADS_VISIBLE = false;

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

        // Check for permanent magic unlock first
        if (prefs.getBoolean(KEY_PERMANENT_PASS_PREFIX + feature.name(), false)) {
            return true;
        }

        long now = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();

        return switch (feature) {
            case THEME -> prefs.getLong(KEY_PASS_THEME_EXPIRY, 0) > now;
            case SUPPORT -> prefs.getLong(KEY_PASS_SUPPORT_EXPIRY, 0) > now;
            case BACKUP -> prefs.getLong(KEY_PASS_BACKUP_EXPIRY, 0) > now;
            case QUIET_HOURS -> prefs.getLong(KEY_PASS_QUIET_HOURS_EXPIRY, 0) > now;
            case BYPASS_VOLUME -> prefs.getBoolean(KEY_PASS_BYPASS_VOLUME_ACTIVE, false);
            case VIBRATION -> prefs.getBoolean(KEY_PASS_VIBRATION_ACTIVE, false);
            case STICKY_NOTIF -> prefs.getBoolean(KEY_PASS_STICKY_ACTIVE, false);
            case DOSE_WINDOW -> prefs.getBoolean(KEY_PASS_DOSE_WINDOW_ACTIVE, false);
            case PHOTO -> prefs.getLong(KEY_PASS_PHOTO_EXPIRY, 0) > now;
            case AD_FREE -> prefs.getLong(KEY_PASS_AD_FREE_EXPIRY, 0) > now;
            case EXTRA_MED_SLOT -> false; // This is a count-based unlock, not boolean/time
        };
    }

    /**
     * Legacy method for ad-eligibility. Purchase or active Magic Pass (deprecated)
     * should hide standard ads.
     */
    public static boolean isPremium(Context context) {
        return isPremiumPurchased(context) || isFeatureUnlocked(context, FeaturePassType.AD_FREE);
    }

    public static int getHistoryRetentionDays() {
        return com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getHistoryRetentionDays();
    }

    /**
     * Generates a human-readable label for a feature's active pass status.
     *
     * @param context Application context for string resources.
     * @param feature The feature type to check.
     * @return A string like " Active until 14:00" or "Active for next reminder", or empty if not active.
     */
    public static String getFeatureExpiryLabel(Context context, FeaturePassType feature) {
        if (isPremiumPurchased(context)) return "";

        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(context);

        return switch (feature) {
            case THEME -> formatExpiry(context, prefs.getLong(KEY_PASS_THEME_EXPIRY, 0));
            case SUPPORT -> formatExpiry(context, prefs.getLong(KEY_PASS_SUPPORT_EXPIRY, 0));
            case BACKUP -> formatExpiry(context, prefs.getLong(KEY_PASS_BACKUP_EXPIRY, 0));
            case QUIET_HOURS ->
                    formatExpiry(context, prefs.getLong(KEY_PASS_QUIET_HOURS_EXPIRY, 0));

            case DOSE_WINDOW -> prefs.getBoolean(KEY_PASS_DOSE_WINDOW_ACTIVE, false) ?
                    context.getString(R.string.active_for_next_take_outside_window) : "";

            case BYPASS_VOLUME -> prefs.getBoolean(KEY_PASS_BYPASS_VOLUME_ACTIVE, false) ?
                    context.getString(R.string.active_for_next_reminder) : "";
            case VIBRATION -> prefs.getBoolean(KEY_PASS_VIBRATION_ACTIVE, false) ?
                    context.getString(R.string.active_for_next_reminder) : "";
            case STICKY_NOTIF -> prefs.getBoolean(KEY_PASS_STICKY_ACTIVE, false) ?
                    context.getString(R.string.active_for_next_reminder) : "";
            case PHOTO -> formatExpiry(context, prefs.getLong(KEY_PASS_PHOTO_EXPIRY, 0));
            case AD_FREE -> formatExpiry(context, prefs.getLong(KEY_PASS_AD_FREE_EXPIRY, 0));
            case EXTRA_MED_SLOT -> "";
        };
    }

    private static String formatExpiry(Context context, long expiry) {
        if (expiry <= com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal()) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return context.getString(R.string.active_until_format, sdf.format(new java.util.Date(expiry)));
    }

    /**
     * Enum to distinguish between premium features for pass logic.
     */
    public enum FeaturePassType {
        THEME, SUPPORT, BACKUP, QUIET_HOURS, BYPASS_VOLUME, VIBRATION, STICKY_NOTIF, DOSE_WINDOW, PHOTO, AD_FREE, EXTRA_MED_SLOT
    }
}
