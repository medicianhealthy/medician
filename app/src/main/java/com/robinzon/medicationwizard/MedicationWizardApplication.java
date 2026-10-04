package com.robinzon.medicationwizard;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.robinzon.medicationwizard.billing.BillingManager;
import com.robinzon.medicationwizard.managers.EngagementManager;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.ui.settings.SettingsViewModel;
import com.robinzon.medicationwizard.utils.NetworkMonitor;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.workers.HistoryCleanupWorker;

import java.util.concurrent.TimeUnit;

public class MedicationWizardApplication extends Application
        implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private static MedicationWizardApplication sInstance;
    private Activity mCurrentActivity;

    /**
     * @return The global application context.
     */
    /**
     * Retrieves the global application context.
     *
     * @return The global application context instance.
     */
    public static android.content.Context getContext() {
        return sInstance.getApplicationContext();
    }

    /**
     * Initializes the notification channel and default app settings on startup.
     */
    /**
     * Initializes the application, setting up critical managers, notification channels, and global settings on startup.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        this.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        NotificationManager.createNotificationChannel(this);
        BillingManager.getInstance(this); // Initialize billing and check entitlements FIRST
        applyTheme();
        applyLanguage();
        scheduleHistoryCleanup();
        EngagementManager.recordInteraction(this);
        NetworkMonitor.getInstance(this).start();
    }

    /**
     * Re-applies the saved language preference to the current application context.
     * <p>
     * If no preference is saved, the app follows the system language.
     * </p>
     */
    private void applyLanguage() {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(this);
        if (sp.containsKey(SettingsViewModel.KEY_APP_LANGUAGE)) {
            String langCode = sp.getString(SettingsViewModel.KEY_APP_LANGUAGE, "en");
            androidx.core.os.LocaleListCompat locales = androidx.core.os.LocaleListCompat.forLanguageTags(langCode);
            AppCompatDelegate.setApplicationLocales(locales);
        }
    }

    /**
     * Enqueues a periodic WorkManager task to regularly clean up old medication history.
     */
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

    /**
     * Re-applies the saved theme preference (Light, Dark, or System) globally across the application.
     */
    private void applyTheme() {
        SettingsViewModel.enforceEntitlements(this);

        int theme = SharedPreferencesManager.getInstance(this).getInt(SettingsViewModel.KEY_APP_THEME, SettingsViewModel.THEME_SYSTEM);
        switch (theme) {
            case SettingsViewModel.THEME_LIGHT ->
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            case SettingsViewModel.THEME_DARK ->
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            default ->
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
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

    /**
     * Executes logic when the application process moves to the foreground.
     * <p>
     * Triggers usage tracking and displays App Open ads if eligible.
     * </p>
     *
     * @param owner The lifecycle owner representing the application process.
     */
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        // Ensure theme/settings are correct if a pass expired while the app was in the background
        SettingsViewModel.enforceEntitlements(this);

        // Start usage tracking
        com.robinzon.medicationwizard.utils.Statisticator.onMoveToForeground(this);

        // Record as a new session for ad decision purposes
        com.robinzon.medicationwizard.utils.Statisticator.onSessionStarted(this);

        // Show the ad (if available) when the app moves to foreground.
        if (mCurrentActivity instanceof MainActivity main) {
            main.onMoveToForeground();
        }
    }

    /**
     * Executes logic when the application process moves to the background.
     * <p>
     * Finalizes and persists usage statistics.
     * </p>
     *
     * @param owner The lifecycle owner representing the application process.
     */
    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // Finalize and persist usage time
        com.robinzon.medicationwizard.utils.Statisticator.onMoveToBackground(this);
    }
}
