package com.robinzon.medicationwizard;

import android.os.Bundle;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.rootclasses.ISuper;
import com.robinzon.medicationwizard.databinding.ActivityMainBinding;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.utils.Validator;


public class MainActivity extends AppCompatActivity implements ISuper {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding mBinding;
    private AdsManager mAdsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!AdsManager.DISABLE_ADS) {
            initAds();
        }
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        setSupportActionBar(mBinding.appBarMain.toolbar);
        mBinding.appBarMain.fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());
        final DrawerLayout drawer = mBinding.drawerLayout;
        final NavigationView navigationView = mBinding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setDrawerLayout(drawer)
                .build();
        final NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        SharedPreferencesManager.getInstance(this).getLong("d",0);
    }

    private void initAds() {
        mAdsManager = new AdsManager();
        getAdsManager().onCreate(this);
        Logger.getInstance().logMultipleTags(getClassName(), AdsManager.LOGS_ADS, "MainActivity calling init ads");
        getAdsManager().initializeAds(this, adsInitializeState -> {
            Logger.getInstance().logMultipleTags(getClassName(), AdsManager.LOGS_ADS,
                    "MainActivity got a message that ads finished initializing. status is[%s]",
                    adsInitializeState.name());
            onAdsFinishedInitializing();
        });
    }

    private void onAdsFinishedInitializing() {
        Logger.getInstance().logMultipleTags(getClassName(), AdsManager.LOGS_ADS,
                "MainActivity starting to take action that waited for ads to initialize");
        getAdsManager().showBanner(this);
        getAdsManager().loadInterstitial(this);
        findViewById(R.id.text_home).setOnClickListener(v -> {
            if(getAdsManager().isInterstitialLoaded()){
                getAdsManager().showInterstitial(MainActivity.this);
            }
        });
        Logger.getInstance().logSingleTag(getClassName(),
                AdsManager.LOG_REWARDED_VIDEO,
                "Main activity calling to load rv");
        getAdsManager().loadRV(this);

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
        if (Validator.isValidObject(getAdsManager())){
            getAdsManager().onResume(this);
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (Validator.isValidObject(getAdsManager())){
            getAdsManager().onDestroy(this);
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (Validator.isValidObject(getAdsManager())){
            getAdsManager().onPause(this);
        }
        super.onPause();
    }

    @Override
    public String getClassName(){
        return "{MainActivity}";
    }

    public AdsManager getAdsManager() {
        return mAdsManager;
    }
}