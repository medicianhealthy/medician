package com.robinzon.medicationwizard;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.interfaces.AdsInitializeCallBack;
import com.robinzon.medicationwizard.databinding.ActivityMainBinding;
import com.robinzon.medicationwizard.remoteconfig.FireBaseFetchCallBack;
import com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.utils.Validator;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private AdsManager mAdsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!AdsManager.DISABLE_ADS) {
            initAds();
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        SharedPreferencesManager.getInstance(this).getLong("d",0);
    }

    private void initAds() {
        mAdsManager = new AdsManager();
        mAdsManager.onCreate(this);
        Logger.logMultipleTags(getClassName(), AdsManager.LOGS_ADS, "MainActivity calling init ads");
        mAdsManager.initializeAdMobAds(this, new AdsInitializeCallBack(){
            @Override
            public void onAdsInitialized(AdsInitializeState adsInitializeState) {
                Logger.logMultipleTags(getClassName(), AdsManager.LOGS_ADS,
                        "MainActivity got a message thet ads finished initializing. status is[%s]",
                        adsInitializeState.name());
                onAdsFinishedInitializing();
            }
        });
    }

    private void onAdsFinishedInitializing() {
        Logger.logMultipleTags(getClassName(), AdsManager.LOGS_ADS,
                "MainActivity starting to take action that waited for ads to initialize");
        mAdsManager.showBanner(this);
        mAdsManager.loadInterstitial(this);
        findViewById(R.id.text_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAdsManager.isRvLoaded()){
                    mAdsManager.showRv(MainActivity.this);
                }
            }
        });
        Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Main activity calling to load rv");
        mAdsManager.loadRV(this);

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
        if (Validator.isValidObject(mAdsManager)){
            mAdsManager.onResume(this);
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (Validator.isValidObject(mAdsManager)){
            mAdsManager.onDestroy(this);
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (Validator.isValidObject(mAdsManager)){
            mAdsManager.onPause(this);
        }
        super.onPause();
    }

    public String getClassName(){
        return "{MainActivity}";
    }
}