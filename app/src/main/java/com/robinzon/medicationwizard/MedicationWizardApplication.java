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
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.robinzon.medicationwizard.billing.BillingManager;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.ui.settings.SettingsViewModel;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.workers.HistoryCleanupWorker;

import java.util.concurrent.TimeUnit;

public class MedicationWizardApplication extends Application
        implements Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private static android.content.Context sContext;
    private Activity mCurrentActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        this.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        
        NotificationManager.createNotificationChannel(this);
        applyTheme();
        applyLanguage();
        scheduleHistoryCleanup();
        BillingManager.getInstance(this); // Initialize billing and check entitlements
    }

    private void applyLanguage() {
        String langCode = SharedPreferencesManager.getInstance(this).getString(SettingsViewModel.KEY_APP_LANGUAGE, "en");
        androidx.core.os.LocaleListCompat locales = androidx.core.os.LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    private void scheduleHistoryCleanup() {
        PeriodicWorkRequest cleanupRequest = new PeriodicWorkRequest.Builder(
                HistoryCleanupWorker.class,
                24, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "HistoryCleanup",
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest);
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
        if (mCurrentActivity == activity) {
            mCurrentActivity = null;
        }
    }

    public static android.content.Context getContext() {
        return sContext;
    }

    /** LifecycleObserver method that shows the app open ad when the app moves to foreground. */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onMoveToForeground() {
        // Start usage tracking
        com.robinzon.medicationwizard.utils.Statisticator.onMoveToForeground(this);
        
        // Record as a new session for ad decision purposes
        com.robinzon.medicationwizard.utils.Statisticator.onSessionStarted(this);

        // Show the ad (if available) when the app moves to foreground.
        if (mCurrentActivity instanceof MainActivity) {
            ((MainActivity) mCurrentActivity).onMoveToForeground();
        }
    }

    /** LifecycleObserver method that handles cleanup when app moves to background. */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    protected void onMoveToBackground() {
        // Finalize and persist usage time
        com.robinzon.medicationwizard.utils.Statisticator.onMoveToBackground(this);
    }
}
