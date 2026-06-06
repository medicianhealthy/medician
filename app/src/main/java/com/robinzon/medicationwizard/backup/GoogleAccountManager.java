package com.robinzon.medicationwizard.backup;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

/**
 * Manages user's Google Account information for backup and profile display.
 */
public class GoogleAccountManager {

    private static final String PREF_ACCOUNT_EMAIL = "google_account_email";
    private static final String PREF_ACCOUNT_NAME = "google_account_name";
    private static final String PREF_ACCOUNT_PHOTO_URL = "google_account_photo_url";

    private static GoogleAccountManager sInstance;
    private final Context mContext;

    private GoogleAccountManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized GoogleAccountManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GoogleAccountManager(context);
        }
        return sInstance;
    }

    public void saveAccountInfo(@Nullable String email, @Nullable String name, @Nullable Uri photoUri) {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(mContext);
        if (email != null) prefs.setString(PREF_ACCOUNT_EMAIL, email);
        if (name != null) prefs.setString(PREF_ACCOUNT_NAME, name);
        if (photoUri != null) prefs.setString(PREF_ACCOUNT_PHOTO_URL, photoUri.toString());
    }

    public void clearAccountInfo() {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(mContext);
        prefs.removeKey(PREF_ACCOUNT_EMAIL);
        prefs.removeKey(PREF_ACCOUNT_NAME);
        prefs.removeKey(PREF_ACCOUNT_PHOTO_URL);
    }

    public boolean isSignedIn() {
        return getAccountEmail() != null;
    }

    @Nullable
    public String getAccountEmail() {
        return SharedPreferencesManager.getInstance(mContext).getString(PREF_ACCOUNT_EMAIL, null);
    }

    @Nullable
    public String getAccountName() {
        return SharedPreferencesManager.getInstance(mContext).getString(PREF_ACCOUNT_NAME, null);
    }

    @Nullable
    public String getAccountPhotoUrl() {
        return SharedPreferencesManager.getInstance(mContext).getString(PREF_ACCOUNT_PHOTO_URL, null);
    }
}
