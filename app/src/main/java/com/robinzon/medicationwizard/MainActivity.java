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
import com.robinzon.medicationwizard.databinding.ActivityMainBinding;
import com.robinzon.medicationwizard.remoteconfig.FireBaseFetchCallBack;
import com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private AdsManager mAdsManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.robinzon.medicationwizard.databinding.ActivityMainBinding mBinding = ActivityMainBinding.inflate(getLayoutInflater());
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
                .setOpenableLayout(drawer)
                .build();
        final NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        mAdsManager = new AdsManager(this);
        RemoteConfigManager.getInstance().fetchConfiguration(new FireBaseFetchCallBack() {
            @Override
            public void onFetchCompleted(boolean isSuccessFull) {
                getAdsManager().initializeAds();
            }
        });

        mBinding.getRoot().findViewById(R.id.text_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAdsManager().showRewarded();
            }
        });

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
        if (null != getAdsManager()) {
            getAdsManager().onPause();
        }
        super.onPause();
    }

    public String getClassName() {
        return MainActivity.class.getSimpleName();
    }



}