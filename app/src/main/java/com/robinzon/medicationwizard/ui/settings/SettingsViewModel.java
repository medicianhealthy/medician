package com.robinzon.medicationwizard.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

public class SettingsViewModel extends AndroidViewModel {

    public static final String KEY_APP_THEME = "app_theme";
    public static final String KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";
    public static final String KEY_QUIET_HOURS_START = "quiet_hours_start";
    public static final String KEY_QUIET_HOURS_END = "quiet_hours_end";

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private final MutableLiveData<Integer> mTheme = new MutableLiveData<>();
    private final MutableLiveData<String> mQuietHoursRange = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        int savedTheme = SharedPreferencesManager.getInstance(application).getInt(KEY_APP_THEME, THEME_SYSTEM);
        mTheme.setValue(savedTheme);
        
        String start = SharedPreferencesManager.getInstance(application).getString(KEY_QUIET_HOURS_START, "23:00");
        String end = SharedPreferencesManager.getInstance(application).getString(KEY_QUIET_HOURS_END, "07:00");
        mQuietHoursRange.setValue(start + " - " + end);
    }

    public LiveData<Integer> getTheme() {
        return mTheme;
    }

    public LiveData<String> getQuietHoursRange() {
        return mQuietHoursRange;
    }

    public void setQuietHours(int startH, int startM, int endH, int endM) {
        String start = String.format(java.util.Locale.getDefault(), "%02d:%02d", startH, startM);
        String end = String.format(java.util.Locale.getDefault(), "%02d:%02d", endH, endM);
        
        SharedPreferencesManager.getInstance(getApplication()).setString(KEY_QUIET_HOURS_START, start);
        SharedPreferencesManager.getInstance(getApplication()).setString(KEY_QUIET_HOURS_END, end);
        
        mQuietHoursRange.setValue(start + " - " + end);
    }

    public void setTheme(int theme) {
        if (mTheme.getValue() != null && mTheme.getValue() == theme) return;
        
        mTheme.setValue(theme);
        SharedPreferencesManager.getInstance(getApplication()).setInt(KEY_APP_THEME, theme);
        applyTheme(theme);
    }

    public void applyTheme(int theme) {
        switch (theme) {
            case THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            case THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            default -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}