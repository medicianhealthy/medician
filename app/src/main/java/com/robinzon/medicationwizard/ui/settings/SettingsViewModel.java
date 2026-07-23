package com.robinzon.medicationwizard.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
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
    public static final String KEY_VIBRATION_PATTERN = "vibration_pattern";
    public static final String KEY_FLASH_PATTERN = "flash_pattern";
    public static final String KEY_STICKY_NOTIF_ENABLED = "sticky_notif_enabled";
    public static final String KEY_CUSTOM_EARLY_THRESHOLD = "custom_early_threshold";
    public static final String KEY_CUSTOM_LATE_THRESHOLD = "custom_late_threshold";

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
    private final MutableLiveData<String> mVibrationPattern = new MutableLiveData<>();
    private final MutableLiveData<String> mFlashPattern = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mStickyNotif = new MutableLiveData<>();
    private final MutableLiveData<Integer> mCustomEarlyThreshold = new MutableLiveData<>();
    private final MutableLiveData<Integer> mCustomLateThreshold = new MutableLiveData<>();

    /**
     * Constructs the ViewModel and loads current preferences from disk.
     */
    public SettingsViewModel(@NonNull Application application) {
        super(application);
        refreshSettings();
    }

    /**
     * Global utility to check if any temporary passes have expired and revert their
     * associated settings immediately.
     */
    public static void enforceEntitlements(android.content.Context context) {
        if (com.robinzon.medicationwizard.AppConfig.isPremiumPurchased(context)) return;

        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);

        // 1. Theme Check
        int savedTheme = sp.getInt(KEY_APP_THEME, THEME_SYSTEM);
        if (savedTheme != THEME_SYSTEM && !com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(context, com.robinzon.medicationwizard.AppConfig.FeaturePassType.THEME)) {
            sp.setInt(KEY_APP_THEME, THEME_SYSTEM);
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        // 2. Volume Check
        if (!com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(context, com.robinzon.medicationwizard.AppConfig.FeaturePassType.BYPASS_VOLUME)) {
            sp.setBoolean(KEY_BYPASS_SYSTEM_VOLUME, false);
        }

        // 3. Precision Checks
        if (!com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(context, com.robinzon.medicationwizard.AppConfig.FeaturePassType.VIBRATION)) {
            sp.setBoolean(KEY_VIBRATION_ENABLED, false);
        }
        if (!com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(context, com.robinzon.medicationwizard.AppConfig.FeaturePassType.STICKY_NOTIF)) {
            sp.setBoolean(KEY_STICKY_NOTIF_ENABLED, false);
        }
    }

    /**
     * Re-scans all preferences to ensure the UI is in sync with feature pass consumption.
     */
    public void refreshSettings() {
        enforceEntitlements(getApplication());
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(getApplication());

        mTheme.setValue(sp.getInt(KEY_APP_THEME, THEME_SYSTEM));
        mVibration.setValue(sp.getBoolean(KEY_VIBRATION_ENABLED, false));
        mStickyNotif.setValue(sp.getBoolean(KEY_STICKY_NOTIF_ENABLED, false));
        mBypassVolume.setValue(sp.getBoolean(KEY_BYPASS_SYSTEM_VOLUME, false));

        com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager rcm = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance();
        mCustomEarlyThreshold.setValue(sp.getInt(KEY_CUSTOM_EARLY_THRESHOLD, rcm.getEarlyTakeThresholdMins()));
        mCustomLateThreshold.setValue(sp.getInt(KEY_CUSTOM_LATE_THRESHOLD, rcm.getLateTakeThresholdMins()));

        String start = sp.getString(KEY_QUIET_HOURS_START, "23:00");
        String end = sp.getString(KEY_QUIET_HOURS_END, "07:00");
        mQuietHoursRange.setValue(start + " - " + end);

        mNotifVolume.setValue(sp.getInt(KEY_NOTIF_VOLUME, 70));
        mSoundName.setValue(sp.getString(KEY_NOTIF_SOUND_NAME, "Default"));
        mSoundUri.setValue(sp.getString(KEY_NOTIF_SOUND_URI, ""));
        mVibrationPattern.setValue(sp.getString(KEY_VIBRATION_PATTERN, "Standard"));
        mFlashPattern.setValue(sp.getString(KEY_FLASH_PATTERN, "None"));
        mSnoozeDuration.setValue(sp.getInt(KEY_SNOOZE_DURATION_MINS, 10));
        mMaxSnoozes.setValue(sp.getInt(KEY_MAX_SNOOZES, 3));
        
        String currentLang = sp.getString(KEY_APP_LANGUAGE, "");
        if (currentLang.isEmpty()) {
            currentLang = getApplication().getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        mLanguageCode.setValue(currentLang);
    }

    /**
     * @return Observable LiveData for the current app theme ID.
     */
    public LiveData<Integer> getTheme() {
        return mTheme;
    }

    /**
     * Updates the application theme globally.
     * This method triggers {@link androidx.appcompat.app.AppCompatDelegate} to immediately re-draw the app
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
     * @return Observable LiveData for the formatted Quiet Hours range string.
     */
    public LiveData<String> getQuietHoursRange() {
        return mQuietHoursRange;
    }

    /**
     * @return Observable LiveData for the Bypass System Volume status.
     */
    public LiveData<Boolean> getBypassVolume() {
        return mBypassVolume;
    }

    /**
     * Toggles whether reminder alerts should bypass the device's ringer mode.
     */
    public void setBypassVolume(boolean bypass) {
        mBypassVolume.setValue(bypass);
        SharedPreferencesManager.getInstance(getApplication()).setBoolean(KEY_BYPASS_SYSTEM_VOLUME, bypass);
    }

    /**
     * @return Observable LiveData for the notification volume level.
     */
    public LiveData<Integer> getNotifVolume() {
        return mNotifVolume;
    }

    /**
     * Updates the custom alert volume percentage (0-100).
     */
    public void setNotifVolume(int volume) {
        mNotifVolume.setValue(volume);
        SharedPreferencesManager.getInstance(getApplication()).setInt(KEY_NOTIF_VOLUME, volume);
    }

    /**
     * @return Observable LiveData for the current notification sound name.
     */
    public LiveData<String> getSoundName() {
        return mSoundName;
    }

    /**
     * @return Observable LiveData for the current notification sound URI.
     */
    public LiveData<String> getSoundUri() {
        return mSoundUri;
    }

    /**
     * @return Observable LiveData for the snooze interval in minutes.
     */
    public LiveData<Integer> getSnoozeDuration() {
        return mSnoozeDuration;
    }

    /**
     * Updates and persists the snooze duration.
     *
     * @param mins The number of minutes for each snooze.
     */
    public void setSnoozeDuration(int mins) {
        mSnoozeDuration.setValue(mins);
        SharedPreferencesManager.getInstance(getApplication()).setInt(KEY_SNOOZE_DURATION_MINS, mins);
    }

    /**
     * @return Observable LiveData for the maximum allowed snoozes (-1 for unlimited).
     */
    public LiveData<Integer> getMaxSnoozes() {
        return mMaxSnoozes;
    }

    /**
     * Updates and persists the maximum number of snoozes.
     *
     * @param max The limit, or -1 for unlimited.
     */
    public void setMaxSnoozes(int max) {
        mMaxSnoozes.setValue(max);
        SharedPreferencesManager.getInstance(getApplication()).setInt(KEY_MAX_SNOOZES, max);
    }

    /**
     * @return Observable LiveData for the current application language code.
     */
    public LiveData<String> getLanguageCode() {
        return mLanguageCode;
    }

    /**
     * @return Observable LiveData for the custom vibration status.
     */
    public LiveData<Boolean> getVibration() {
        return mVibration;
    }

    /**
     * Updates and persists the custom vibration preference.
     *
     * @param enabled True to enable custom vibration patterns.
     */
    public void setVibration(boolean enabled) {
        mVibration.setValue(enabled);
        SharedPreferencesManager.getInstance(getApplication()).setBoolean(KEY_VIBRATION_ENABLED, enabled);
    }

    /**
     * @return Observable LiveData for the sticky notification status.
     */
    public LiveData<Boolean> getStickyNotif() {
        return mStickyNotif;
    }

    /**
     * Updates and persists the sticky notification preference.
     *
     * @param enabled True to prevent notifications from being swiped away.
     */
    public void setStickyNotif(boolean enabled) {
        mStickyNotif.setValue(enabled);
        SharedPreferencesManager.getInstance(getApplication()).setBoolean(KEY_STICKY_NOTIF_ENABLED, enabled);
    }

    public LiveData<String> getVibrationPattern() {
        return mVibrationPattern;
    }

    public void setVibrationPattern(String pattern) {
        mVibrationPattern.setValue(pattern);
        SharedPreferencesManager.getInstance(getApplication()).setString(KEY_VIBRATION_PATTERN, pattern);
    }

    public LiveData<String> getFlashPattern() {
        return mFlashPattern;
    }

    public void setFlashPattern(String pattern) {
        mFlashPattern.setValue(pattern);
        SharedPreferencesManager.getInstance(getApplication()).setString(KEY_FLASH_PATTERN, pattern);
    }

    public LiveData<Integer> getCustomEarlyThreshold() {
        return mCustomEarlyThreshold;
    }

    public void setCustomEarlyThreshold(int mins) {
        SharedPreferencesManager.getInstance(getApplication()).setInt(KEY_CUSTOM_EARLY_THRESHOLD, mins);
        mCustomEarlyThreshold.setValue(mins);
    }

    public LiveData<Integer> getCustomLateThreshold() {
        return mCustomLateThreshold;
    }

    public void setCustomLateThreshold(int mins) {
        SharedPreferencesManager.getInstance(getApplication()).setInt(KEY_CUSTOM_LATE_THRESHOLD, mins);
        mCustomLateThreshold.setValue(mins);
    }

    /**
     * Updates the application language and applies the change globally.
     *
     * @param langCode The new ISO language code (e.g., "en", "iw"), or empty for system default.
     */
    public void setLanguage(String langCode) {
        if (langCode == null || langCode.isEmpty()) {
            SharedPreferencesManager.getInstance(getApplication()).removeKey(KEY_APP_LANGUAGE);
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.getEmptyLocaleList());
            refreshSettings();
            return;
        }

        if (langCode.equals(mLanguageCode.getValue())) return;

        mLanguageCode.setValue(langCode);
        SharedPreferencesManager.getInstance(getApplication()).setString(KEY_APP_LANGUAGE, langCode);

        // Apply the language change
        androidx.core.os.LocaleListCompat locales = androidx.core.os.LocaleListCompat.forLanguageTags(langCode);
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(locales);
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
     * Low-level helper to trigger the Android system theme switch.
     */
    private void applyTheme(int theme) {
        switch (theme) {
            case THEME_LIGHT ->
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            case THEME_DARK ->
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
            default ->
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}