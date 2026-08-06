package com.robinzon.medicationwizard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.robinzon.medicationwizard.ads.AdAction;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.OnAdActionListener;
import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;
import com.robinzon.medicationwizard.backup.GoogleAccountManager;
import com.robinzon.medicationwizard.databinding.ActivityMainBinding;
import com.robinzon.medicationwizard.notifications.ConsentManager;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.managers.MagicManager;
import com.robinzon.medicationwizard.managers.EngagementManager;
import com.robinzon.medicationwizard.remoteconfig.FireBaseFetchCallBack;
import com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager;
import com.robinzon.medicationwizard.ui.AddMedicationBottomSheet;
import com.robinzon.medicationwizard.ui.settings.FeatureRationalBottomSheet;
import com.robinzon.medicationwizard.ui.magics.MagicEarnBottomSheet;
import com.robinzon.medicationwizard.ui.onboarding.OnboardingActivity;
import com.robinzon.medicationwizard.ui.settings.SettingsViewModel;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.PermissionManager;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

/**
 * The main entry point and hosting activity for the Medication Wizard application.
 */
public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, OnAdActionListener {

    private AppBarConfiguration appBarConfiguration;
    private AdsManager adsManager;
    private NavController navController;
    private long lastBackPressedTime;
    private final Handler adCheckHandler = new Handler(Looper.getMainLooper());
    private Runnable adCheckRunnable;
    private boolean mIsFabVisible = true;
    private boolean isSpeedDialExpanded = false;
    private SharedPreferences.OnSharedPreferenceChangeListener magicBalanceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SharedPreferencesManager.getInstance(this).getBoolean(OnboardingActivity.KEY_HAS_SEEN_ONBOARDING, false)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        ActivityMainBinding mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        // Listen for Extra Slot unlock from Marketplace
        getSupportFragmentManager().setFragmentResultListener("feature_unlocked", this, (key, bundle) -> {
            String type = bundle.getString("feature_type");
            if (AppConfig.FeaturePassType.EXTRA_MED_SLOT.name().equals(type)) {
                // Delay slightly for transition
                new Handler(Looper.getMainLooper()).postDelayed(() -> 
                    new AddMedicationBottomSheet().show(getSupportFragmentManager(), "AddMedBS"), 
                    300);
            }
        });

        loadCheats();

        mainBinding.appBarMain.fab.setOnClickListener(view -> {
            toggleSpeedDial(mainBinding);
        });

        mainBinding.appBarMain.fabLogDose.setOnClickListener(v -> {
            toggleSpeedDial(mainBinding);
            openLogDoseSheet();
        });

        mainBinding.appBarMain.fabAddNewMed.setOnClickListener(v -> {
            toggleSpeedDial(mainBinding);
            openAddMedSheet();
        });

        setSupportActionBar(mainBinding.appBarMain.toolbar);

        final DrawerLayout drawer = mainBinding.drawerLayout;
        final NavigationView navigationView = mainBinding.navView;

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_medications_list, R.id.nav_history, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();

        final NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            // Bind Toolbar to NavController with drawer support (for hamburger icon)
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            // Bind NavigationView to NavController
            NavigationUI.setupWithNavController(navigationView, navController);

            navigationView.setNavigationItemSelectedListener(item -> {
                int selectedItemId = item.getItemId();
                if (selectedItemId == R.id.nav_home) {
                    // Start destination needs careful handling if we're already there or not
                    if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() == R.id.nav_home) {
                        drawer.closeDrawer(GravityCompat.START);
                        return true;
                    }
                    navController.popBackStack(R.id.nav_home, false);
                } else {
                    NavigationUI.onNavDestinationSelected(item, navController);
                }
                drawer.closeDrawer(GravityCompat.START);
                return true;
            });

            refreshNavHeader();

            magicBalanceListener = (sharedPreferences, key) -> {
                if (AppConfig.KEY_MAGIC_BALANCE.equals(key)) {
                    runOnUiThread(this::refreshNavHeader);
                }
            };
            SharedPreferencesManager.getInstance(this).registerListener(magicBalanceListener);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                invalidateOptionsMenu();
            });

            // Handle back button for Drawer closing, Home navigation, and Double-back exit
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (drawer.isDrawerOpen(GravityCompat.START)) {
                        // 1. Close drawer if open
                        drawer.closeDrawer(GravityCompat.START);
                    } else if (navController.popBackStack()) {
                        // 2. Go back naturally in the fragment stack (e.g. from sub-screens)
                    } else if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.nav_home) {
                        // 3. Root screen that isn't Home: go to Home ("All roads lead to Home")
                        navController.navigate(R.id.nav_home);
                    } else {
                        // 4. Already at Home: guard against accidental exit
                        if (lastBackPressedTime + 2000 > com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal()) {
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.back_to_exit, Toast.LENGTH_SHORT).show();
                            lastBackPressedTime = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();
                        }
                    }
                }
            });
        }

        adsManager = new AdsManager(this);

        checkDailyMagicBonus();

        ConsentManager.gatherConsent(this, () -> {
            Logger.log("Ads", "Consent gathered, fetching Remote Config");
            RemoteConfigManager.getInstance().fetchConfiguration(new FireBaseFetchCallBack() {
                @Override
                public void onFetchCompleted(boolean isSuccessFull) {
                    Logger.log("Ads", "Remote Config fetch completed. Success: %b", isSuccessFull);
                    getAdsManager().initializeAds();
                    startAdCheckTimer();
                }
            });
        });

        checkExactAlarmPermission();
    }

    /**
     * Periodically checks for ad availability and eligibility.
     */
    private void startAdCheckTimer() {
        if (adCheckRunnable != null) return;
        adCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (null != adsManager) {
                    adsManager.loadAds(); // Check for banner/interstitial eligibility based on real-time usage
                }
                adCheckHandler.postDelayed(this, 10000L);
            }
        };
        adCheckHandler.postDelayed(adCheckRunnable, 10000L); // Check every 10 seconds
    }

    private void stopAdCheckTimer() {
        if (adCheckRunnable != null) {
            adCheckHandler.removeCallbacks(adCheckRunnable);
            adCheckRunnable = null;
        }
    }

    /**
     * Refreshes the navigation drawer header with the current user's profile info.
     */
    public void refreshNavHeader() {
        final NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            updateNavHeader(navigationView);
        }
    }

    /**
     * Checks if the app has permission to schedule exact alarms (required for Android 12+).
     */
    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = getSystemService(android.app.AlarmManager.class);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(this);
                dialog.setTitle(getString(R.string.alarm_permission_title));
                dialog.setMessage(getString(R.string.alarm_permission_message));
                dialog.setPositiveButton(getString(R.string.action_settings), (d, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                });
                dialog.setNegativeButton(getString(R.string.button_not_now), null);
                dialog.show();
            }
        }
    }

    private void updateNavHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            android.widget.ImageView profileImage = headerView.findViewById(R.id.imageView);
            android.widget.TextView profileName = headerView.findViewById(R.id.textView);

            // 1. Magic Balance & Listener (For all users)
            android.widget.TextView magicBalance = headerView.findViewById(R.id.txt_magic_balance_header);
            if (magicBalance != null) {
                magicBalance.setText(getString(R.string.magic_balance_format, MagicManager.getInstance(this).getMagicBalance()));
            }
            View magicBtn = headerView.findViewById(R.id.btn_magic_header);
            if (magicBtn != null) {
                magicBtn.setOnClickListener(v -> {
                    DrawerLayout drawer = findViewById(R.id.drawer_layout);
                    if (drawer != null) drawer.closeDrawer(GravityCompat.START);
                    new MagicEarnBottomSheet().show(getSupportFragmentManager(), "MagicEarnBS");
                });
            }

            // 2. Profile Info (Depending on Premium/Cloud status)
            if (com.robinzon.medicationwizard.AppConfig.isPremium(this) && com.robinzon.medicationwizard.AppConfig.CLOUD_BACKUP_ENABLED) {
                GoogleAccountManager accountManager = GoogleAccountManager.getInstance(this);
                if (accountManager.isSignedIn()) {
                    String name = accountManager.getAccountName();
                    if (name != null) profileName.setText(name);

                    String photoUrl = accountManager.getAccountPhotoUrl();
                    if (photoUrl != null) {
                        com.bumptech.glide.Glide.with(this)
                                .load(photoUrl)
                                .circleCrop()
                                .placeholder(R.mipmap.ic_launcher)
                                .into(profileImage);
                    }
                } else {
                    profileName.setText(R.string.nav_header_subtitle);
                    profileImage.setImageResource(R.mipmap.ic_launcher);
                }
            } else {
                profileName.setText(R.string.nav_header_subtitle);
                profileImage.setImageResource(R.mipmap.ic_launcher);
            }
        }
    }

    public AdsManager getAdsManager() {
        return adsManager;
    }

    /**
     * Increments the global interaction score and triggers an interstitial if the threshold is met.
     *
     * @param score The amount of points to add (e.g., 1.5 for main items, 1.0 for sub-items).
     */
    public void addInteractionScore(float score) {
        if (addInteractionScoreOnly(score)) {
            // Threshold met, trigger interstitial show
            Logger.log("Ads", "Score threshold reached, requesting interstitial");
            adsManager.showInterstitialAd();
        }
    }

    /**
     * Adds the interaction score without immediately triggering an ad.
     * @return True if the threshold was met.
     */
    public boolean addInteractionScoreOnly(float score) {
        return com.robinzon.medicationwizard.utils.Statisticator.addInteractionScoreAndCheck(this, score);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            EngagementManager.recordInteraction(this);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (navController != null && navController.getCurrentDestination() != null) {
            int currentDestinationId = navController.getCurrentDestination().getId();

            // Hide the settings icon ONLY if we're already on the settings screen
            android.view.MenuItem settingsMenuItem = menu.findItem(R.id.nav_settings);
            if (settingsMenuItem != null) {
                settingsMenuItem.setVisible(currentDestinationId != R.id.nav_settings);
            }

            // Show Top Bar '+' icon ONLY if the Floating Action Button is hidden AND there are existing medications
            android.view.MenuItem addMedItem = menu.findItem(R.id.action_add_med);
            if (addMedItem != null) {
                boolean hasMeds = com.robinzon.medicationwizard.entities.Medication.hasMedications(this);
                addMedItem.setVisible(!mIsFabVisible && hasMeds);
            }

            // Hide Premium icon if already purchased
            android.view.MenuItem premiumItem = menu.findItem(R.id.action_premium);
            if (premiumItem != null) {
                premiumItem.setVisible(!com.robinzon.medicationwizard.AppConfig.isPremiumPurchased(this));
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        int itemId = item.getItemId();

        // Fix for navigation crash: home button should not be passed to onNavDestinationSelected
        if (itemId == android.R.id.home) {
            return super.onOptionsItemSelected(item);
        }

        if (itemId == R.id.action_premium) {
            new com.robinzon.medicationwizard.ui.settings.PremiumBottomSheet().show(getSupportFragmentManager(), "PremiumMain");
            return true;
        }

        if (itemId == R.id.action_add_med) {
            showAddChoiceDialog();
            return true;
        }

        // Standard NavigationUI handling for items matching destination IDs (like nav_settings)
        if (NavigationUI.onNavDestinationSelected(item, navController)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // NEW: Ensure any active reminder sound is stopped when user enters the app
        com.robinzon.medicationwizard.reminders.ReminderAlertManager.getInstance().stopAlarm();

        // Ensure theme and other feature passes haven't expired
        SettingsViewModel.enforceEntitlements(this);

        if (null != getAdsManager()) {
            getAdsManager().onResume();
        }
        startAdCheckTimer();
    }

    @Override
    protected void onDestroy() {
        if (null != getAdsManager()) {
            getAdsManager().onDestroy();
        }
        stopAdCheckTimer();
        if (magicBalanceListener != null) {
            SharedPreferencesManager.getInstance(this).unregisterListener(magicBalanceListener);
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != getAdsManager()) {
            getAdsManager().onPause();
        }
        stopAdCheckTimer();
    }

    public void setFabVisible(boolean visible) {
        mIsFabVisible = visible;
        final View fab = findViewById(R.id.fab);
        if (fab != null) {
            fab.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        invalidateOptionsMenu();
    }

    public void onMoveToForeground() {
        adCheckHandler.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed() && getAdsManager() != null) {
                getAdsManager().showAppOpenAd();
            }
        }, 300L);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.REQUEST_PERMISSION_CODE_POST_NOTIFICATIONS) {
            final boolean hasResults = grantResults.length > 0;
            final boolean granted = hasResults && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
            NotificationManager.getInstance(this).setHasGrantedPermission(granted);

            // Aggressive UI refresh: Iterate all possible fragment managers to find the Settings screen
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    final NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.nav_host_fragment_content_main);
                    if (navHostFragment != null) {
                        for (androidx.fragment.app.Fragment fragment : navHostFragment.getChildFragmentManager().getFragments()) {
                            refreshFragmentNotificationUi(fragment);
                        }
                    }
                } catch (Exception e) {
                    Logger.log("MainActivity", "Error during post-permission UI refresh: " + e.getMessage());
                }
            }, 800);
        }
    }

    private void refreshFragmentNotificationUi(androidx.fragment.app.Fragment fragment) {
        if (fragment instanceof com.robinzon.medicationwizard.ui.settings.SettingsFragment) {
            ((com.robinzon.medicationwizard.ui.settings.SettingsFragment) fragment).updateNotificationStatus();
        }

        // REFINED: Also trigger refresh for the Dashboard if it's visible to ensure
        // first-med addition is reflected if background updates were interrupted.
        if (fragment instanceof com.robinzon.medicationwizard.ui.todaysmedications.TodaysMedicationsFragment) {
            // This will trigger the reactive LiveData to re-evaluate the UI state
            fragment.onResume();
        }

        // Recursively search child fragments
        for (androidx.fragment.app.Fragment child : fragment.getChildFragmentManager().getFragments()) {
            refreshFragmentNotificationUi(child);
        }
    }

    @Override
    public void onAdAction(@NonNull AdMobAd adMobAd, AdAction adAction) {
    }

    private void loadCheats() {
        SharedPreferencesManager sharedPreferencesManager = SharedPreferencesManager.getInstance(this);
        AppConfig.IS_PREMIUM = sharedPreferencesManager.getBoolean(AppConfig.KEY_CHEAT_PREMIUM, AppConfig.IS_PREMIUM);
        AppConfig.FORCED_ADS_VISIBLE = sharedPreferencesManager.getBoolean(AppConfig.KEY_CHEAT_SHOW_ADS, AppConfig.FORCED_ADS_VISIBLE);
    }

    private void toggleSpeedDial(ActivityMainBinding binding) {
        isSpeedDialExpanded = !isSpeedDialExpanded;
        if (isSpeedDialExpanded) {
            expandFab(binding);
        } else {
            collapseFab(binding);
        }
    }

    private void expandFab(ActivityMainBinding binding) {
        binding.appBarMain.layoutFabLogDose.setVisibility(View.VISIBLE);
        binding.appBarMain.layoutFabAddMed.setVisibility(View.VISIBLE);

        binding.appBarMain.fab.animate().rotation(45f).setDuration(200).start();

        binding.appBarMain.layoutFabLogDose.setAlpha(0f);
        binding.appBarMain.layoutFabLogDose.setTranslationY(20f);
        binding.appBarMain.layoutFabLogDose.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(200)
                .start();

        binding.appBarMain.layoutFabAddMed.setAlpha(0f);
        binding.appBarMain.layoutFabAddMed.setTranslationY(20f);
        binding.appBarMain.layoutFabAddMed.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(200)
                .start();
    }

    private void collapseFab(ActivityMainBinding binding) {
        binding.appBarMain.fab.animate().rotation(0f).setDuration(200).start();

        binding.appBarMain.layoutFabLogDose.animate()
                .alpha(0f)
                .translationY(20f)
                .setDuration(200)
                .withEndAction(() -> binding.appBarMain.layoutFabLogDose.setVisibility(View.GONE))
                .start();

        binding.appBarMain.layoutFabAddMed.animate()
                .alpha(0f)
                .translationY(20f)
                .setDuration(200)
                .withEndAction(() -> binding.appBarMain.layoutFabAddMed.setVisibility(View.GONE))
                .start();
    }

    private void openLogDoseSheet() {
        new com.robinzon.medicationwizard.ui.LogDoseBottomSheet().show(getSupportFragmentManager(), "LogDoseBS");
    }

    private void openAddMedSheet() {
        if (!AppConfig.isPremiumPurchased(this)) {
            int currentCount = com.robinzon.medicationwizard.entities.Medication.getSavedMedications(this).size();
            int unlockedSlots = SharedPreferencesManager.getInstance(this).getInt(AppConfig.KEY_MEDS_SLOTS_UNLOCKED, 0);
            int allowedLimit = AppConfig.FREE_MED_LIMIT + unlockedSlots;

            if (currentCount >= allowedLimit) {
                FeatureRationalBottomSheet.newInstance(AppConfig.FeaturePassType.EXTRA_MED_SLOT)
                        .show(getSupportFragmentManager(), "ExtraSlotRational");
                return;
            }
        }
        new com.robinzon.medicationwizard.ui.AddMedicationBottomSheet().show(getSupportFragmentManager(), "AddMedBS");
    }

    private void checkDailyMagicBonus() {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(this);
        long lastLogin = sp.getLong("magic_last_login", 0);
        long now = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();

        java.util.Calendar lastCal = java.util.Calendar.getInstance();
        lastCal.setTimeInMillis(lastLogin);
        java.util.Calendar nowCal = java.util.Calendar.getInstance();
        nowCal.setTimeInMillis(now);

        boolean isNewDay = lastLogin == 0 ||
                lastCal.get(java.util.Calendar.DAY_OF_YEAR) != nowCal.get(java.util.Calendar.DAY_OF_YEAR) ||
                lastCal.get(java.util.Calendar.YEAR) != nowCal.get(java.util.Calendar.YEAR);

        if (isNewDay) {
            MagicManager.getInstance(this).addMagics(1);
            sp.setLong("magic_last_login", now);
            // Optionally show a toast or dialog
            Toast.makeText(this, getString(R.string.magic_earned_toast, 1) + " (Daily Bonus)", Toast.LENGTH_LONG).show();
        }
    }

    public void shareToWhatsApp() {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(this);
        long lastShare = sp.getLong("magic_last_share", 0);
        long now = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();

        java.util.Calendar lastCal = java.util.Calendar.getInstance();
        lastCal.setTimeInMillis(lastShare);
        java.util.Calendar nowCal = java.util.Calendar.getInstance();
        nowCal.setTimeInMillis(now);

        boolean alreadySharedToday = lastShare != 0 &&
                lastCal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR) &&
                lastCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR);

        if (alreadySharedToday) {
            Toast.makeText(this, R.string.magic_share_limit_toast, Toast.LENGTH_LONG).show();
            return;
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_message));
        sendIntent.setType("text/plain");
        sendIntent.setPackage("com.whatsapp");

        try {
            startActivity(sendIntent);
            // Grant magics on return (simulated verification)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                MagicManager.getInstance(this).addMagics(2);
                sp.setLong("magic_last_share", now);
                Toast.makeText(this, getString(R.string.magic_earned_toast, 2), Toast.LENGTH_SHORT).show();
            }, 1500);
        } catch (android.content.ActivityNotFoundException e) {
            // Fallback to general share
            sendIntent.setPackage(null);
            startActivity(Intent.createChooser(sendIntent, getString(R.string.magic_earn_share_title)));
        }
    }

    private void showAddChoiceDialog() {
        String[] options = {getString(R.string.action_log_dose), getString(R.string.action_add_new_med)};
        com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(this);
        dialog.setTitle(getString(R.string.dialog_add_choice_title));
        dialog.setItems(options, (d, which) -> {
            if (which == 0) openLogDoseSheet();
            else openAddMedSheet();
        });
        dialog.show();
    }
}
