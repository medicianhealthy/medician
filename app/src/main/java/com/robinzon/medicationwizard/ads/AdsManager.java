package com.robinzon.medicationwizard.ads;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.ads.admob.AdMobBanner;
import com.robinzon.medicationwizard.ads.admob.AdMobInterstitial;
import com.robinzon.medicationwizard.ads.admob.AdMobRewarded;
import com.robinzon.medicationwizard.utils.NetworkUtils;

public class AdsManager {



    private final Activity mActivity;
    private AdMobBanner mMainBanner;
    private AdMobInterstitial mMainInterstitial;
    private AdMobRewarded mMainRewarded;

    public AdsManager(final @NonNull Activity activity) {
        this.mActivity = activity;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public void initializeAds() {
        MobileAds.initialize(getActivity(), new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                createAds();
                loadAds();
            }
        });
    }

    private void createAds() {
        if (NetworkUtils.isNetworkAvailable(getActivity())) {
            mMainBanner = new AdMobBanner(BuildConfig.DEBUG ? getTestAdForAdType(AdType.AdaptiveBanner) : "a",
                    this,
                    AdPlacement.Main);
            mMainInterstitial = new AdMobInterstitial(BuildConfig.DEBUG ? getTestAdForAdType(AdType.InterstitialVideo) : "z" ,
                    this ,
                    AdPlacement.Main);
            mMainRewarded = new AdMobRewarded(BuildConfig.DEBUG ? getTestAdForAdType(AdType.Rewarded) : "a" ,
                    this,
                    AdPlacement.Main);
        }
    }

    private void loadAds() {
        if (null != mMainBanner) {
            mMainBanner.load();
        }
        if (null != mMainInterstitial) {
            mMainInterstitial.load();
        }
        if (null != mMainRewarded) {
            mMainRewarded.load();
        }
    }


    /** @noinspection SameParameterValue*/
    private @NonNull String getTestAdForAdType(@NonNull final AdType adType) {
        switch (adType) {
            case AppOpen:
                return "ca-app-pub-3940256099942544/9257395921";
            case AdaptiveBanner:
                return "ca-app-pub-3940256099942544/9214589741";
            case Banner:
                return "ca-app-pub-3940256099942544/6300978111";
            case Interstitial:
                return "ca-app-pub-3940256099942544/1033173712";
            case InterstitialVideo:
                return "ca-app-pub-3940256099942544/8691691433";
            case Rewarded:
                return "ca-app-pub-3940256099942544/5224354917";
            case RewardedInterstitial:
                return "ca-app-pub-3940256099942544/5354046379";
            case NativeAdvanced:
                return "ca-app-pub-3940256099942544/2247696110";
            case NativeAdvancedVideo:
                return "ca-app-pub-3940256099942544/1044960115";
            default:
                throw new IllegalArgumentException();
        }
    }

    public void onResume() {
        if (null != mMainBanner) {
            mMainBanner.onResume();
        }
    }

    public void onDestroy() {

    }

    public void onPause() {
        if (null != mMainBanner) {
            mMainBanner.onPause();
        }

    }

    public void showInterstitialAd() {
        if (null != mMainInterstitial) {
            mMainInterstitial.show();
        }
    }

    public void showRewarded() {
        if (null != mMainRewarded) {
            mMainRewarded.show();
        }
    }
}
