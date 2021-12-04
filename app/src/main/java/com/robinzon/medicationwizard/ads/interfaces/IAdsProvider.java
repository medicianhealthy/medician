package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;

public interface IAdsProvider {
    void onResume(final Activity activity);
    void onPause(final Activity activity);
    void onDestroy(final Activity activity);
    void onCreate(final Activity activity);
    boolean hasBanner();
    IBannerAd getBanner();
    boolean hasInterstitial();
    IInterstitialAd getInterstitial();
    boolean hasRv();
    IRewardedVideo getRewardedVideo();
    void initialize(final Activity activity , final IAdsInitializeCallBack adsInitializeCallBack);
}
