package com.robinzon.medicationwizard.backup;

import android.content.Context;

import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

/**
 * Manages settings for cloud backup.
 */
public class CloudBackupSettings {

    private static final String PREF_AUTO_BACKUP = "cloud_auto_backup";
    private static final String PREF_WIFI_ONLY = "cloud_backup_wifi_only";

    private static CloudBackupSettings sInstance;
    private final Context mContext;

    private CloudBackupSettings(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized CloudBackupSettings getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CloudBackupSettings(context);
        }
        return sInstance;
    }

    public boolean isAutoBackupEnabled() {
        return SharedPreferencesManager.getInstance(mContext).getBoolean(PREF_AUTO_BACKUP, false);
    }

    public void setAutoBackupEnabled(boolean enabled) {
        SharedPreferencesManager.getInstance(mContext).setBoolean(PREF_AUTO_BACKUP, enabled);
    }

    public boolean isWifiOnlyEnabled() {
        return SharedPreferencesManager.getInstance(mContext).getBoolean(PREF_WIFI_ONLY, true);
    }

    public void setWifiOnlyEnabled(boolean enabled) {
        SharedPreferencesManager.getInstance(mContext).setBoolean(PREF_WIFI_ONLY, enabled);
    }
}