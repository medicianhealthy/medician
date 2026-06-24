package com.robinzon.medicationwizard.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

/**
 * ViewModel for application-wide settings and preferences.
 * <p>
 * This class serves as the bridge between the {@link SettingsFragment} and 
 * the persistent {@link SharedPreferencesManager}. It manages live states 
 * for the UI and applies global changes such as theme switching.
 * </p>
 */
public class SettingsViewModel extends AndroidViewModel {

    // Shared Preference Keys
    public static final String KEY_APP_THEME = "app_theme";
    public static final String KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";
    public static final String KEY_QUIET_HOURS_START = "quiet_hours_start";
    public static final String KEY_QUIET_HOURS_END = "quiet_hours_end";
    public static final String KEY_BYPASS_SYSTEM_VOLUME = "bypass_system_volume";
    public static final String KEY_NOTIF_VOLUME = "notif_volume";
    public static final String KEY_NOTIF_SOUND_NAME = "notif_sound_name";
    public static final String KEY_NOTIF_SOUND_URI = "notif_sound_uri";
    public static final String KEY_SNOOZE_DURATION_MINS = "snooze_duration_mins";
    public static final String KEY_MAX_SNOOZES = "max_snoozes";
    public static final String KEY_APP_LANGUAGE = "app_language";
    public static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    public static final String KEY_STICKY_NOTIF_ENABLED = "sticky_notif_enabled";

    // Theme Constants
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    // Observable States
    private final MutableLiveData<Integer> mTheme = new MutableLiveData<>();
    private final MutableLiveData<String> mQuietHoursRange = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mBypassVolume = new MutableLiveData<>();
    private final MutableLiveData<Integer> mNotifVolume = new MutableLiveData<>();
    private final MutableLiveData<String> mSoundName = new MutableLiveData<>();
    private final MutableLiveData<String> mSoundUri = new MutableLiveData<>();
    private final MutableLiveData<Integer> mSnoozeDuration = new MutableLiveData<>();
    private final MutableLiveData<Integer> mMaxSnoozes = new MutableLiveData<>();
    private final MutableLiveData<String> mLanguageCode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mVibration = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mStickyNotif = new MutableLiveData<>();

    /**
     * Constructs the ViewModel and loads current preferences from disk.
     */
    public SettingsViewModel(@NonNull Application application) {
        super(application);
        refreshSettings();
    }

    /**
     * Re-scans all preferences to ensure the UI is in sync with feature pass consumption.
     */
    public void refreshSettings() {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(getApplication());
        
        int savedTheme = sp.getInt(KEY_APP_THEME, THEME_SYSTEM);
        
        // Entitlement Enforcement: Revert restricted features if pass expired
        if (!com.robinzon.medicationwizard.AppConfig.isPremiumPurchased(getApplication())) {
            // 1. Theme Check
            if (savedTheme != THEME_SYSTEM && !com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(getApplication(), com.robinzon.medicationwizard.AppConfig.FeaturePassType.THEME)) {
                savedTheme = THEME_SYSTEM;
                sp.setInt(KEY_APP_THEME, THEME_SYSTEM);
                applyTheme(THEME_SYSTEM);
            }
            
            // 2. Volume Check
            if (!com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(getApplication(), com.robinzon.medicationwizard.AppConfig.FeaturePassType.BYPASS_VOLUME)) {
                sp.setBoolean(KEY_BYPASS_SYSTEM_VOLUME, false);
            }
            
            // 3. Precision Checks
            if (!com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(getApplication(), com.robinzon.medicationwizard.AppConfig.FeaturePassType.VIBRATION)) {
                sp.setBoolean(KEY_VIBRATION_ENABLED, false);
            }
            if (!com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(getApplication(), com.robinzon.medicationwizard.AppConfig.FeaturePassType.STICKY_NOTIF)) {
                sp.setBoolean(KEY_STICKY_NOTIF_ENABLED, false);
            }
        }

        mTheme.setValue(savedTheme);
        mVibration.setValue(sp.getBoolean(KEY_VIBRATION_ENABLED, false));
        mStickyNotif.setValue(sp.getBoolean(KEY_STICKY_NOTIF_ENABLED, false));
        mBypassVolume.setValue(sp.getBoolean(KEY_BYPASS_SYSTEM_VOLUME, false));

        String start = sp.getString(KEY_QUIET_HOURS_START, "23:00");
        String end = sp.getString(KEY_QUIET_HOURS_END, "07:00");
        mQuietHoursRange.setValue(start + " - " + end);

        mNotifVolume.setValue(sp.getInt(KEY_NOTIF_VOLUME, 70));
        mSoundName.setValue(sp.getString(KEY_NOTIF_SOUND_NAME, "Default"));
        mSoundUri.setValue(sp.getString(KEY_NOTIF_SOUND_URI, ""));
        mSnoozeDuration.setValue(sp.getInt(KEY_SNOOZE_DURATION_MINS, 10));
        mMaxSnoozes.setValue(sp.getInt(KEY_MAX_SNOOZES, 3));
        mLanguageCode.setValue(sp.getString(KEY_APP_LANGUAGE, "en"));
    }

    public LiveData<Integer> getTheme() { return mTheme; }
    public LiveData<String> getQuietHoursRange() { return mQuietHoursRange; }
    public LiveData<Boolean> getBypassVolume() { return mBypassVolume; }
    public LiveData<Integer> getNotifVolume() { return mNotifVolume; }
    public LiveData<String> getSoundName() { return mSoundName; }
    public LiveData<String> getSoundUri() { return mSoundUri; }
    public LiveData<Integer> getSnoozeDuration() { return mSnoozeDuration; }
    public LiveData<Integer> getMaxSnoozes() { return mMaxSnoozes; }
    public LiveData<String> getLanguageCode() { return mLanguageCode; }
    public LiveData<Boolean> getVibration() { return mVibration; }
    public LiveData<Boolean> getStickyNotif() { return mStickyNotif; }

    public void setVibration(boolean enabled) {
        mVibration.setValue(enabled);
        SharedPreferencesManager.getInstance(getApplication()).setBoolean(KEY_VIBRATION_ENABLED, enabled);
    }

    public void setStickyNotif(boolean enabled) {
        mStickyNotif.setValue(enabled);
        SharedPreferencesManager.getInstance(getApplication()).setBoolean(KEY_STICKY_NOTIF_ENABLED, enabled);
    }

    public void setLanguage(String langCode) {
        if (langCode.equals(mLanguageCode.getValue())) return;
        
        mLanguageCode.setValue(langCode);
        SharedPreferencesManager.getInstance(getApplication()).setString(KEY_APP_LANGUAGE, langCode);
        
        // Apply the language change
        androidx.core.os.LocaleListCompat locales = androidx.core.os.LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    public void setSnoozeDuration(int mins) {
        mSnoozeDuration.setValue(mins);
        SharedPreferencesManager.getInstance(getApplication()).setInt(KEY_SNOOZE_DURATION_MINS, mins);
    }

    public void setMaxSnoozes(int max) {
        mMaxSnoozes.setValue(max);
        SharedPreferencesManager.getInstance(getApplication()).setInt(KEY_MAX_SNOOZES, max);
    }

    /**
     * Toggles whether reminder alerts should bypass the device's ringer mode.
     */
    public void setBypassVolume(boolean bypass) {
        mBypassVolume.setValue(bypass);
        SharedPreferencesManager.getInstance(getApplication()).setBoolean(KEY_BYPASS_SYSTEM_VOLUME, bypass);
    }

    /**
     * Updates the custom alert volume percentage (0-100).
     */
    public void setNotifVolume(int volume) {
        mNotifVolume.setValue(volume);
        SharedPreferencesManager.getInstance(getApplication()).setInt(KEY_NOTIF_VOLUME, volume);
    }

    /**
     * Updates the selected reminder sound name and system URI.
     */
    public void setSound(String name, String uri) {
        mSoundName.setValue(name);
        mSoundUri.setValue(uri);
        SharedPreferencesManager.getInstance(getApplication()).setString(KEY_NOTIF_SOUND_NAME, name);
        SharedPreferencesManager.getInstance(getApplication()).setString(KEY_NOTIF_SOUND_URI, uri);
    }

    /**
     * Updates the time range during which alerts are suppressed.
     */
    public void setQuietHours(int startH, int startM, int endH, int endM) {
        String start = String.format(java.util.Locale.getDefault(), "%02d:%02d", startH, startM);
        String end = String.format(java.util.Locale.getDefault(), "%02d:%02d", endH, endM);
        
        SharedPreferencesManager.getInstance(getApplication()).setString(KEY_QUIET_HOURS_START, start);
        SharedPreferencesManager.getInstance(getApplication()).setString(KEY_QUIET_HOURS_END, end);
        
        mQuietHoursRange.setValue(start + " - " + end);
    }

    /**
     * Updates the application theme globally.
     * This method triggers {@link AppCompatDelegate} to immediately re-draw the app
     * in the selected mode.
     *
     * @param theme One of {@link #THEME_LIGHT}, {@link #THEME_DARK}, or {@link #THEME_SYSTEM}.
     */
    public void setTheme(int theme) {
        if (mTheme.getValue() != null && mTheme.getValue() == theme) return;
        
        mTheme.setValue(theme);
        SharedPreferencesManager.getInstance(getApplication()).setInt(KEY_APP_THEME, theme);
        applyTheme(theme);
    }

    /**
     * Low-level helper to trigger the Android system theme switch.
     */
    private void applyTheme(int theme) {
        switch (theme) {
            case THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            case THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            default -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}