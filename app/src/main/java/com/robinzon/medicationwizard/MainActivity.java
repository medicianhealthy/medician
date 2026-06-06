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
import androidx.core.view.GravityCompat;
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
import com.robinzon.medicationwizard.notifications.ConsentManager;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.remoteconfig.FireBaseFetchCallBack;
import com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager;
import com.robinzon.medicationwizard.ui.AddMedicationBottomSheet;
import com.robinzon.medicationwizard.ui.onboarding.OnboardingActivity;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.PermissionManager;
import com.robinzon.medicationwizard.utils.Screen;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.utils.Statisticator;

import java.util.Timer;
import java.util.TimerTask;

import com.robinzon.medicationwizard.backup.GoogleAccountManager;

/**
 * The main entry point and hosting activity for the Medication Wizard application.
 */
public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, OnAdActionListener {

    private AppBarConfiguration mAppBarConfiguration;
    private AdsManager mAdsManager;
    private NavController mNavController;
    private boolean mHasCreated;
    
    public static final float BANNER_HEIGHT_MULTIPLIER = 1.08F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!SharedPreferencesManager.getInstance(this).getBoolean(OnboardingActivity.KEY_HAS_SEEN_ONBOARDING, false)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        ActivityMainBinding mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.appBarMain.fab.setOnClickListener(view -> {
            AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "AddMedBottomSheet");
        });
        
        setSupportActionBar(mBinding.appBarMain.toolbar);

        final DrawerLayout drawer = mBinding.drawerLayout;
        final NavigationView navigationView = mBinding.navView;
        
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_medications_list, R.id.nav_history, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();
                
        final NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            mNavController = navHostFragment.getNavController();
            // Bind Toolbar to NavController with drawer support (for hamburger icon)
            NavigationUI.setupActionBarWithNavController(this, mNavController, mAppBarConfiguration);
            // Bind NavigationView to NavController
            NavigationUI.setupWithNavController(navigationView, mNavController);
            
            navigationView.setNavigationItemSelectedListener(item -> {
                boolean handled = NavigationUI.onNavDestinationSelected(item, mNavController);
                if (!handled && item.getItemId() == R.id.nav_home) {
                    // If we're already on home, just close the drawer
                    drawer.closeDrawer(GravityCompat.START);
                    return true;
                }
                if (handled) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                return handled;
            });

            refreshNavHeader();
        }

        mAdsManager = new AdsManager(this);
        
        ConsentManager.gatherConsent(this, () -> RemoteConfigManager.getInstance().fetchConfiguration(new FireBaseFetchCallBack() {
            @Override
            public void onFetchCompleted(boolean isSuccessFull) {
                getAdsManager().initializeAds();
            }
        }));

        mHasCreated = true;
        Statisticator.onSessionStarted(this);
        checkExactAlarmPermission();
    }

    public void refreshNavHeader() {
        final NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            updateNavHeader(navigationView);
        }
    }

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

    private void updateNavHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        android.widget.ImageView profileImage = headerView.findViewById(R.id.imageView);
        android.widget.TextView profileName = headerView.findViewById(R.id.textView);

        if (!com.robinzon.medicationwizard.AppConfig.IS_PREMIUM) {
            profileName.setText(R.string.nav_header_subtitle);
            profileImage.setImageResource(R.mipmap.ic_launcher);
            return;
        }

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
    }

    public AdsManager getAdsManager() {
        return mAdsManager;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mNavController != null) {
            int currentId = mNavController.getCurrentDestination() != null ? mNavController.getCurrentDestination().getId() : -1;
            
            // Hide the settings icon if we're already on the settings screen
            android.view.MenuItem settingsItem = menu.findItem(R.id.nav_settings);
            if (settingsItem != null) {
                settingsItem.setVisible(currentId != R.id.nav_settings);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(mNavController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_settings) {
            mNavController.navigate(R.id.nav_settings);
            return true;
        } else if (id == R.id.action_premium) {
            new com.robinzon.medicationwizard.ui.settings.PremiumBottomSheet().show(getSupportFragmentManager(), "PremiumMain");
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    public void setFabVisible(boolean visible) {
        final View fab = findViewById(R.id.fab);
        if (fab != null) {
            fab.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

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
    }
}