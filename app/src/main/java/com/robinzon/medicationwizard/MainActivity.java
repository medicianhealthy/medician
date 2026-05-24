package com.robinzon.medicationwizard;

import android.content.pm.PackageManager;
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
import com.robinzon.medicationwizard.utils.PermissionManager;
import com.robinzon.medicationwizard.utils.Screen;
import com.robinzon.medicationwizard.utils.Statisticator;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, OnAdActionListener {

    private AppBarConfiguration mAppBarConfiguration;
    private AdsManager mAdsManager;
    private boolean mHasCreated;
    public static final float BANNER_HEIGHT_MULTIPLIER = 1.08F;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.robinzon.medicationwizard.databinding.ActivityMainBinding mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 1. Create a new instance of our bottom sheet
                AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();

                // 2. Show it! The string tag is just for Android's internal fragment manager.
                bottomSheet.show(getSupportFragmentManager(), "AddMedBottomSheet");
            }
        });
        setSupportActionBar(mBinding.appBarMain.toolbar);
//        mBinding.appBarMain.fab.setOnClickListener(view ->
//                //NotificationManager.getInstance(this).requestPermissionIfNeeded()
//                // 1. Create a new instance of our bottom sheet
//                AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();
//
//        // 2. Show it! The string tag is just for Android's internal fragment manager.
//        bottomSheet.show(getSupportFragmentManager(), "AddMedBottomSheet");
//        );
        final DrawerLayout drawer = mBinding.drawerLayout;
        final NavigationView navigationView = mBinding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_medications_list, R.id.nav_history, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();
        // 1. Find the NavHostFragment
        final NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main); // Make sure this ID matches your layout!

        // 2. Get the NavController from the fragment
        if (navHostFragment != null) {
            final NavController navController = navHostFragment.getNavController();
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);
        }

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
    }

    private void setBottomMarginToFab() {
        final View fab = findViewById(R.id.fab);
        if (fab == null) return;

        // 1. Get the real banner height
        int bannerHeightDp = AdMobBanner.getBannerHeightDP(this);
        
        // 2. Add extra room for the FAB (M3 recommendation is 16dp margin)
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


    public AdsManager getAdsManager() {
        return mAdsManager;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

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

    public String getClassName() {
        return MainActivity.class.getSimpleName();
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getAdsManager().showAppOpenAd();
                        }
                    });
                }
            }, 300L);
        }
        mHasCreated = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.REQUEST_PERMISSION_CODE_POST_NOTIFICATIONS) {
            final boolean permissionsArrayValid = permissions.length > 0;
            final boolean permissionHasGranted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
            final boolean granted = permissionsArrayValid && permissionHasGranted;
            NotificationManager.getInstance(this).setHasGrantedPermission(granted);
        }
    }

    @Override
    public void onAdAction(@NonNull AdMobAd adMobAd, AdAction adAction) {

    }


}