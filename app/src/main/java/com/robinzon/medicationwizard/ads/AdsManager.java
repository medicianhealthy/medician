package com.robinzon.medicationwizard.ads;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.ads.adsproviders.admob.AdMobBanner;
import com.robinzon.medicationwizard.ads.adsproviders.admob.AdMobInterstitial;
import com.robinzon.medicationwizard.ads.adsproviders.admob.AdMobRewardedVideo;
import com.robinzon.medicationwizard.ads.interfaces.IAd;
import com.robinzon.medicationwizard.ads.interfaces.IAdsLifeCycleCallBack;
import com.robinzon.medicationwizard.ads.interfaces.IBanner;
import com.robinzon.medicationwizard.ads.interfaces.IInterstitial;
import com.robinzon.medicationwizard.ads.interfaces.IRewardedVideo;
import com.robinzon.medicationwizard.ads.rootclasses.EAdPlacement;
import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;

import java.util.HashMap;
import java.util.Map;


public class AdsManager extends MedicationWizardSuper implements IAdsLifeCycleCallBack {
    public static final String RCKEY_ADS_TIMER_BANNER_GRACE_MINUTES = "ads_timer_banner_grace_minutes";
    public static final String RCKEY_ADS_TIMER_INTER_GRACE_MINUTES = "ads_timer_inter_grace_minutes";
    public static final String RCKEY_ADS_TIMER_RV_GRACE_MINUTES = "ads_timer_rv_grace_minutes";
    private IBanner mBanner;
    private IInterstitial mInterstitial;
    private IRewardedVideo mRewardedVideo;


    private final Map<EAdType, Map<EAdPlacement, IAd>> mAds = new HashMap<>();


    private void createAds(Activity activity) {
        mBanner = new AdMobBanner(activity,
                BuildConfig.DEBUG ? EAdPlacement.BANNER_AD_PLACEMENT_TEST : EAdPlacement.BANNER_AD_PLACEMENT_MAIN);
        mInterstitial = new AdMobInterstitial(activity,
                BuildConfig.DEBUG ? EAdPlacement.INTERSTITIAL_AD_PLACEMENT_TEST : EAdPlacement.INTERSTITIAL_AD_PLACEMENT_ADD_MED);
        mRewardedVideo = new AdMobRewardedVideo(activity,
                BuildConfig.DEBUG ? EAdPlacement.REWARDED_VIDEO_AD_PLACEMENT_TEST : EAdPlacement.REWARDED_VIDEO_AD_PLACEMENT_MED_COLOR);

        mAds.put(EAdType.BANNER, new HashMap<EAdPlacement, IAd>() {{
            put((BuildConfig.DEBUG ? EAdPlacement.BANNER_AD_PLACEMENT_TEST : EAdPlacement.BANNER_AD_PLACEMENT_MAIN), mBanner);
        }});
        mAds.put(EAdType.INTERSTITIAL, new HashMap<EAdPlacement, IAd>() {{
            put((BuildConfig.DEBUG ? EAdPlacement.INTERSTITIAL_AD_PLACEMENT_TEST : EAdPlacement.INTERSTITIAL_AD_PLACEMENT_ADD_MED), mInterstitial);
        }});
        mAds.put(EAdType.REWARDED_VIDEO, new HashMap<EAdPlacement, IAd>() {{
            put((BuildConfig.DEBUG ? EAdPlacement.REWARDED_VIDEO_AD_PLACEMENT_TEST : EAdPlacement.REWARDED_VIDEO_AD_PLACEMENT_MED_COLOR), mRewardedVideo);
        }});

    }

    public void initializeAds(final Activity activity) {
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                createAds(activity);
                loadAds();
            }
        });
    }

    private void loadAds() {
        for (Map<EAdPlacement, IAd> map : mAds.values()) {
            for (IAd ad : map.values()) {
                if (null != ad) {
                    ad.load(this);
                }
            }
        }
    }

    public void showInterstitial() {
        mInterstitial.show(this);
    }

    public void showRv() {
        mRewardedVideo.show(this);
    }


    public void onResume() {
        for (Map<EAdPlacement, IAd> map : mAds.values()) {
            for (IAd ad : map.values()) {
                if (null != ad) {
                    ad.onResume();
                }
            }
        }
    }

    public void onPause() {
        for (Map<EAdPlacement, IAd> map : mAds.values()) {
            for (IAd ad : map.values()) {
                if (null != ad) {
                    ad.onPause();
                }
            }
        }
    }


    public void onDestroy() {
        for (Map<EAdPlacement, IAd> map : mAds.values()) {
            for (IAd ad : map.values()) {
                if (null != ad) {
                    ad.onDestroy();
                }
            }
        }
    }

    @Override
    public String getClassName() {
        return AdsManager.class.getSimpleName();
    }


    @Override
    public void onInterstitialLifeCycleStageChanged(IAd ad, EAdCallBacks adCallBack, AdError adError) {
        logMessageOnAdLifeCycleChanges(ad, adCallBack, adError);
    }

    private void logMessageOnAdLifeCycleChanges(IAd ad, EAdCallBacks adCallBack, AdError adError) {
        if (null != ad && null != adCallBack) {
            final StringBuilder builder = new StringBuilder();
            builder.append(String.format("Ads call back receiver - {%s} Ad lifecycle stage changed to {%s}.", ad.getAdType().getName(), adCallBack.name()));
            if (null != adError) {
                builder.append(String.format("Error is {%s}", adError.getMessage()));
            }
            logMessage(builder.toString());
        }
    }
}
