package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.interfaces.EAdsInitializeState;
import com.robinzon.medicationwizard.ads.interfaces.IAdsInitializeCallBack;
import com.robinzon.medicationwizard.ads.interfaces.IBannerAd;
import com.robinzon.medicationwizard.ads.interfaces.IInterstitialAd;
import com.robinzon.medicationwizard.ads.interfaces.IRewardedVideo;
import com.robinzon.medicationwizard.ads.interfaces.ISdkInitializeCallBack;
import com.robinzon.medicationwizard.ads.rootclasses.AdProvider;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdMob extends AdProvider {

    public static final String AD_INIT_PREFIX = "ca-app-pub-";

    @Override
    public void onResume(Activity activity) {
        if (null != getBanner()) {
            mBanner.onResume(activity);
        }
        if (null != getInterstitial()) {
            mInterstitial.onResume(activity);
        }
        if (null != getRewardedVideo()) {
            mRewardedVideo.onResume(activity);
        }
    }

    @Override
    public void onPause(Activity activity) {
        if (null != getBanner()) {
            mBanner.onPause(activity);
        }
        if (null != getInterstitial()) {
            mInterstitial.onPause(activity);
        }
        if (null != getRewardedVideo()) {
            mRewardedVideo.onPause(activity);
        }
    }

    @Override
    public void onDestroy(Activity activity) {
        if (null != getBanner()) {
            mBanner.onDestroy(activity);
        }
        if (null != getInterstitial()) {
            mInterstitial.onDestroy(activity);
        }
        if (null != getRewardedVideo()) {
            mRewardedVideo.onDestroy(activity);
        }
    }

    @Override
    public void onCreate(Activity activity) {
        if (null != getBanner()) {
            mBanner.onCreate(activity);
        }
        if (null != getInterstitial()) {
            mInterstitial.onCreate(activity);
        }
        if (null != getRewardedVideo()) {
            mRewardedVideo.onCreate(activity);
        }
    }


    @Override
    public IBannerAd getBanner() {
        return mBanner;
    }


    @Override
    public IInterstitialAd getInterstitial() {
        return mInterstitial;
    }


    @Override
    public IRewardedVideo getRewardedVideo() {
        return mRewardedVideo;
    }

    @Override
    public void initializeAds(Activity activity, IAdsInitializeCallBack adsInitializeCallBack) {
        if (!isSdkInitialized()) {
            initializeSdk(activity, new ISdkInitializeCallBack() {
                @Override
                public void onSdkInitialize(EAdsInitializeState state, boolean result) {
                    if (result) {
                        mIsSdkInitialized = true;
                        createAds(activity);
                        adsInitializeCallBack.onAdsInitialized(EAdsInitializeState.SUCCESSFULLY);
                    }
                }
            });
        } else {
            createAds(activity);
            adsInitializeCallBack.onAdsInitialized(EAdsInitializeState.SUCCESSFULLY);
        }
    }

    private void createAds(Activity activity) {
        if (null == getBanner()) {
            mBanner = new AdMobBanner();
            mBanner.createFromLayout(activity, R.id.adView);
        }
        if (null == getInterstitial()) {
            mInterstitial = new AdMobInterstitial();
            mInterstitial.create(activity, R.string.admob_interstitial_id_test);
        }
        if (null == getRewardedVideo()){
            mRewardedVideo = new AdMobRewardedVideo();
            mRewardedVideo.create(activity, R.string.admob_rewarded_interstitial_id_test);
        }
    }

    @Override
    public void initializeSdk(Activity activity, ISdkInitializeCallBack sdkInitializeCallBack) {
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                Logger.getInstance().log(AdMob.class.getSimpleName(), getAllAdsLogs(), "Initialization of AdMob ads completed.");
                final Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                boolean isOneOfAdaptersReady = false;
                for (String key : statusMap.keySet()) {
                    final AdapterStatus adapterStatus = statusMap.get(key);
                    if (Validator.isValidObject(adapterStatus)) {
                        if (AdapterStatus.State.READY == adapterStatus.getInitializationState()) {
                            isOneOfAdaptersReady = true;
                        }
                        Logger.getInstance().log(AdMob.class.getSimpleName(), getAllAdsLogs(),
                                "Initialization of [%s,%s] status is [%s]",
                                key,
                                adapterStatus.getDescription(),
                                adapterStatus.getInitializationState() == AdapterStatus.State.READY ? "ready" : "not ready");
                    }
                }
                if (null != sdkInitializeCallBack) {
                    mIsSdkInitialized = isOneOfAdaptersReady;
                    sdkInitializeCallBack.onSdkInitialize(mIsSdkInitialized ? EAdsInitializeState.SUCCESSFULLY :EAdsInitializeState.FAILED,
                            mIsSdkInitialized);
                }
            }
        });
    }

    private List<String> getAllAdsLogs() {
        return new ArrayList<String>(){{
            add(AdsManager.LOG_BANNER);
            add(AdsManager.LOG_REWARDED_VIDEO);
            add(AdsManager.LOG_INTERSTITIAL);
            add(AdsManager.LOG_REWARDED_INTERSTITIAL);
        }};
    }

    @Override
    public boolean isSdkInitialized() {
        return mIsSdkInitialized;
    }

    @Override
    public void loadBanner(Activity activity) {
        getBanner().load(activity);
    }

    @Override
    public void loadInterstitial(Activity activity) {
        getInterstitial().load(activity);
    }

    @Override
    public void loadRv(Activity activity) {
        getRewardedVideo().load(activity);
    }

    @Override
    public void showBanner(Activity activity) {
        getBanner().show(activity, null);
    }

    @Override
    public void showInterstitial(Activity activity) {
        getInterstitial().show(activity, null);
    }

    @Override
    public void showRv(Activity activity) {
        getRewardedVideo().show(activity, null);
    }

    @Override
    public String getClassName() {
        return AdMob.class.getSimpleName();
    }
}
