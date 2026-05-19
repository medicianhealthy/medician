package com.robinzon.medicationwizard;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.robinzon.medicationwizard.ui.settings.SettingsViewModel;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

public class MedicationWizardApplication extends Application
        implements Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private Activity mCurrentActivity;
    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        
        applyTheme();
    }

    private void applyTheme() {
        int theme = SharedPreferencesManager.getInstance(this).getInt(SettingsViewModel.KEY_APP_THEME, SettingsViewModel.THEME_SYSTEM);
        switch (theme) {
            case SettingsViewModel.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            case SettingsViewModel.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            default -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        mCurrentActivity = activity;

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        mCurrentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        mCurrentActivity = activity;

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        mCurrentActivity = activity;

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        mCurrentActivity = activity;
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        mCurrentActivity = activity;
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        mCurrentActivity = activity;
    }

    /** LifecycleObserver method that shows the app open ad when the app moves to foreground. */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
        if (null != mCurrentActivity) {
            ((MainActivity)mCurrentActivity).onMoveToForeground();
        }


    }
}
