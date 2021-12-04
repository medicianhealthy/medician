package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.interfaces.EAdsInitializeState;
import com.robinzon.medicationwizard.ads.interfaces.IAdsInitializeCallBack;
import com.robinzon.medicationwizard.ads.interfaces.IAdsProvider;
import com.robinzon.medicationwizard.ads.rootclasses.ISuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.Validator;

import java.util.Map;

public class AdMobAdProvider implements IAdsProvider, ISuper {

    private final AdMobBanner mBannerAd;
    private final AdMobInterstitial mInterstitial;
    private final AdMobRewardedVideo mRewardedVideo;
    public static final String AD_INIT_PREFIX = "ca-app-pub-";

    public AdMobAdProvider() {
        mInterstitial = new AdMobInterstitial();
        mBannerAd = new AdMobBanner();
        mRewardedVideo = new AdMobRewardedVideo();
    }

    @Override
    public AdMobBanner getBanner() {
        return mBannerAd;
    }

    @Override
    public boolean hasInterstitial(){
        return Validator.isValidObject(getInterstitial()) &&
                getInterstitial().hasAd();
    }

    @Override
    public AdMobInterstitial getInterstitial() {
        return mInterstitial;
    }

    @Override
    public boolean hasRv(){
        return Validator.isValidObject(getRewardedVideo()) &&
                getRewardedVideo().hasAd();
    }

    @Override
    public AdMobRewardedVideo getRewardedVideo() {
        return mRewardedVideo;
    }

    @Override
    public void initialize(final Activity activity, final IAdsInitializeCallBack adsInitializeCallBack) {
        MobileAds.initialize(activity, initializationStatus -> {
            Logger.getInstance().logMultipleTags(getClassName(), AdsManager.LOGS_ADS, "Initialization of AdMob ads completed.");
            final Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
            byte networksReadyCounter = 0;
            for (String key : statusMap.keySet()) {
                final AdapterStatus adapterStatus = statusMap.get(key);
                if (Validator.isValidObject(adapterStatus)) {
                    if (AdapterStatus.State.READY == adapterStatus.getInitializationState()) {
                        networksReadyCounter++;
                    }
                    Logger.getInstance().logMultipleTags(getClassName(), AdsManager.LOGS_ADS,
                            "Initialization of [%s,%s] status is [%s]",
                            key,
                            adapterStatus.getDescription(),
                            adapterStatus.getInitializationState() == AdapterStatus.State.READY ? "ready" : "not ready");
                }
            }
            if (null != adsInitializeCallBack) {
                if (networksReadyCounter == statusMap.size()) {
                    adsInitializeCallBack.onAdsInitialized(EAdsInitializeState.ALL_NETWORKS_READY);
                } else if (0 == networksReadyCounter) {
                    adsInitializeCallBack.onAdsInitialized(EAdsInitializeState.NO_NETWORKS_ARE_READY);
                } else {
                    adsInitializeCallBack.onAdsInitialized(EAdsInitializeState.SOME_NETWORKS_READY);
                }
            }
        });
    }

    @Override
    public void onResume(Activity activity) {

    }

    @Override
    public void onPause(Activity activity) {

    }

    @Override
    public void onDestroy(Activity activity) {

    }

    @Override
    public void onCreate(Activity activity) {

    }

    @Override
    public boolean hasBanner() {
        return Validator.isValidObject(getBanner()) && getBanner().hasAd();
    }

    @Override
    public String getClassName() {
        return "{AdMobAdProvider}";
    }
}

