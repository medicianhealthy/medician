package com.robinzon.medicationwizard;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.robinzon.medicationwizard.ads.AdAction;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.OnAdActionListener;
import com.robinzon.medicationwizard.ads.admob.AdMobBanner;
import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;
import com.robinzon.medicationwizard.databinding.ActivityMainBinding;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.remoteconfig.FireBaseFetchCallBack;
import com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager;
import com.robinzon.medicationwizard.ui.AddMedicationBottomSheet;
import com.robinzon.medicationwizard.ui.onboarding.OnboardingActivity;
import com.robinzon.medicationwizard.utils.PermissionManager;
import com.robinzon.medicationwizard.utils.Screen;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.utils.Statisticator;

import java.util.Timer;
import java.util.TimerTask;

/**
 * The main entry point and hosting activity for the Medication Wizard application.
 * <p>
 * This activity manages the primary application infrastructure, including:
 * - The global Navigation Drawer and NavHostFragment.
 * - The Floating Action Button (FAB) for adding new medications.
 * - Ad management (AdMob integration and Adaptive Banner height calculation).
 * - System-level permissions (e.g., Notifications).
 * - Session tracking and AppOpen ads.
 * </p>
 */
public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, OnAdActionListener {

    private AppBarConfiguration mAppBarConfiguration;
    private AdsManager mAdsManager;
    private boolean mHasCreated;
    
    /** Multiplier to add safety padding around the adaptive ad banner. */
    public static final float BANNER_HEIGHT_MULTIPLIER = 1.08F;


    /**
     * Initializes the activity, sets up navigation components, and starts ad services.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if onboarding needs to be shown
        if (!SharedPreferencesManager.getInstance(this).getBoolean(OnboardingActivity.KEY_HAS_SEEN_ONBOARDING, false)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        ActivityMainBinding mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // Global FAB listener for adding medications
        mBinding.appBarMain.fab.setOnClickListener(view -> {
            AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "AddMedBottomSheet");
        });
        
        setSupportActionBar(mBinding.appBarMain.toolbar);

        final DrawerLayout drawer = mBinding.drawerLayout;
        final NavigationView navigationView = mBinding.navView;
        
        // Define top-level destinations (no back arrow, only hamburger menu)
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_medications_list, R.id.nav_history, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();
                
        final NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            final NavController navController = navHostFragment.getNavController();
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);
        }

        // Adjust UI elements to avoid overlap with the bottom ad banner
        setBottomMarginToFab();

        mAdsManager = new AdsManager(this);
        RemoteConfigManager.getInstance().fetchConfiguration(new FireBaseFetchCallBack() {
            @Override
            public void onFetchCompleted(boolean isSuccessFull) {
                getAdsManager().initializeAds();
            }
        });

        mHasCreated = true;
        Statisticator.onSessionStarted(this);
        checkExactAlarmPermission();
    }

    /**
     * Checks if the app has permission to schedule exact alarms (Android 12+).
     * If not, redirects the user to the system settings page.
     */
    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = getSystemService(android.app.AlarmManager.class);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Exact Alarms Required")
                        .setMessage("To ensure your medication reminders fire at the exact minute, please allow the app to schedule exact alarms.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Not Now", null)
                        .show();
            }
        }
    }

    /**
     * Dynamically calculates and applies a bottom margin to the FAB.
     * This ensures the FAB is always visible above the anchored adaptive ad banner.
     */
    private void setBottomMarginToFab() {
        final View fab = findViewById(R.id.fab);
        if (fab == null) return;

        int bannerHeightDp = AdMobBanner.getBannerHeightDP(this);
        // Standard M3 padding is 16dp
        int totalMarginDp = (int) (bannerHeightDp * BANNER_HEIGHT_MULTIPLIER) + 16;
        int marginBottomPx = (int) (totalMarginDp * Screen.getDensity(getResources()));
        
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
        layoutParams.setMargins(
                layoutParams.leftMargin,
                layoutParams.topMargin,
                layoutParams.rightMargin,
                marginBottomPx
        );
        fab.setLayoutParams(layoutParams);
    }


    /** @return The global ad manager instance. */
    public AdsManager getAdsManager() {
        return mAdsManager;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /** Handles the 'Up' button or Hamburger menu in the ActionBar. */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != getAdsManager()) {
            getAdsManager().onResume();
        }
        Statisticator.onMoveToForeground(this);
    }

    @Override
    protected void onDestroy() {
        if (null != getAdsManager()) {
            getAdsManager().onDestroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != getAdsManager()) {
            getAdsManager().onPause();
        }
        Statisticator.onMoveToBackground(this);
    }

    /**
     * Controls the visibility of the primary Floating Action Button.
     * Often used by fragments to hide the FAB on read-only screens (like Settings).
     *
     * @param visible True to show the FAB.
     */
    public void setFabVisible(boolean visible) {
        final View fab = findViewById(R.id.fab);
        if (fab != null) {
            fab.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Triggers the display of an AppOpen ad after a slight delay 
     * when the application moves to the foreground.
     */
    public void onMoveToForeground() {
        if (!mHasCreated) {
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(() -> getAdsManager().showAppOpenAd());
                }
            }, 300L);
        }
        mHasCreated = false;
    }

    /**
     * Handles system permission results, specifically updating the 
     * Notification Manager if POST_NOTIFICATIONS is granted.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.REQUEST_PERMISSION_CODE_POST_NOTIFICATIONS) {
            final boolean hasResults = grantResults.length > 0;
            final boolean granted = hasResults && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
            NotificationManager.getInstance(this).setHasGrantedPermission(granted);
        }
    }

    @Override
    public void onAdAction(@NonNull AdMobAd adMobAd, AdAction adAction) {
        // Handle ad-related analytics or tracking here
    }
}