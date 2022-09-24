package com.robinzon.medicationwizard.ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.ads.adsproviders.admob.AdMobBanner;
import com.robinzon.medicationwizard.ads.adsproviders.admob.AdMobInterstitial;
import com.robinzon.medicationwizard.ads.adsproviders.admob.AdMobRewardedVideo;
import com.robinzon.medicationwizard.ads.interfaces.IAd;
import com.robinzon.medicationwizard.ads.interfaces.IBanner;
import com.robinzon.medicationwizard.ads.interfaces.IInterstitial;
import com.robinzon.medicationwizard.ads.interfaces.IRewardedVideo;
import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;

import java.util.HashSet;
import java.util.Set;


public class AdsManager extends MedicationWizardSuper {
    public static final String LOG_BANNER = "mwiz_Banner_Ad";
    public static final String LOG_INTERSTITIAL = "mwiz_Interstitial_Ad";
    public static final String LOG_REWARDED_INTERSTITIAL = "mwiz_Rewarded_Interstitial_Ad";
    public static final String LOG_REWARDED_VIDEO = "mwiz_Rewarded_Video_Ad";
    public static final String RCKEY_ADS_TIMER_BANNER_GRACE_MINUTES = "ads_timer_banner_grace_minutes";
    public static final String RCKEY_ADS_TIMER_INTER_GRACE_MINUTES = "ads_timer_inter_grace_minutes";
    public static final String RCKEY_ADS_TIMER_RV_GRACE_MINUTES = "ads_timer_rv_grace_minutes";
    public static final int TICK_INTERVAL_SECONDS = 8;
    private Ticker mTicker;
    private IBanner mBanner;
    private IInterstitial mInterstitial;
    private IRewardedVideo mRewardedVideo;

    private final Set<IAd> mAds = new HashSet<>();


    private void createAds(Activity activity) {
        mBanner = new AdMobBanner(activity,
                BuildConfig.DEBUG ? AdsUnitProvider.BANNER_AD_PLACEMENT_TEST :AdsUnitProvider.BANNER_AD_PLACEMENT_MAIN);
        mInterstitial = new AdMobInterstitial(activity,
                BuildConfig.DEBUG ? AdsUnitProvider.INTERSTITIAL_AD_PLACEMENT_TEST : AdsUnitProvider.INTERSTITIAL_AD_PLACEMENT_ADD_MED);
        mRewardedVideo = new AdMobRewardedVideo(activity ,
                BuildConfig.DEBUG ? AdsUnitProvider.REWARDED_VIDEO_AD_PLACEMENT_TEST :AdsUnitProvider.REWARDED_VIDEO_AD_PLACEMENT_MED_COLOR);
        mAds.add(mBanner);
        mAds.add(mInterstitial);
        mAds.add(mRewardedVideo);
    }

    public void initializeAds(final Activity activity) {
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                createAds(activity);
                loadAds(activity);
            }
        });
    }

    private void loadAds(Activity activity) {
        for (IAd ad : mAds) {
            if (null != ad) {
                ad.load();
            }
        }
    }

    public void showInterstitial() {
       mInterstitial.show();
    }

    public void showRv() {
        mRewardedVideo.show();
    }



    public void onResume() {
        for (IAd ad : mAds) {
            if (null != ad){
                ad.onResume();
            }
        }

    }

    public void onPause() {
        for (IAd ad : mAds) {
            if (null != ad){
                ad.onPause();
            }
        }
    }



    public void onDestroy() {
        for (IAd ad : mAds) {
            if (null != ad){
                ad.onDestroy();
            }
        }
    }

    @Override
    public String getClassName() {
        return AdsManager.class.getSimpleName();
    }




    private static void tick() {

    }

    private Ticker getTicker() {
        if (null == mTicker) {
            mTicker = new Ticker();
        }
        return mTicker;
    }

    private static class Ticker extends Handler {
        public static final int MESSAGE_TICK = 1;
        public void handleMessage(final Message message) {
            switch (message.what) {
                case MESSAGE_TICK:
                    AdsManager.tick();
                    break;
                default:
                    break;
            }
        }
    }




}
