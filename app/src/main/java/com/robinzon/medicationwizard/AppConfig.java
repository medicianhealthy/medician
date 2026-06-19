package com.robinzon.medicationwizard;

public class AppConfig {
    public static final int NUMBER_OF_DAYS_TO_SCHEDULE = 7;
    public static boolean IS_PREMIUM = false;
    public static boolean FORCED_ADS_VISIBLE = false;

    public static final String KEY_CHEAT_PREMIUM = "cheat_is_premium";
    public static final String KEY_CHEAT_SHOW_ADS = "cheat_show_ads";
    public static final String KEY_TEMP_PREMIUM_EXPIRY = "temp_premium_expiry";

    public static int getMagicPassDurationHours() {
        return com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getMagicPassDurationHours();
    }

    public static int getHistoryRetentionDays() {
        return com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getHistoryRetentionDays();
    }

    public static boolean isPremium(android.content.Context context) {
        if (IS_PREMIUM) return true;
        long premiumExpiryTimestamp = com.robinzon.medicationwizard.utils.SharedPreferencesManager.getInstance(context).getLong(KEY_TEMP_PREMIUM_EXPIRY, 0);
        return System.currentTimeMillis() < premiumExpiryTimestamp;
    }
}
