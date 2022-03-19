package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;

public interface IAdsProvider {
    void onResume(final Activity activity);
    void onPause(final Activity activity);
    void onDestroy(final Activity activity);
    void onCreate(final Activity activity);
    IBannerAd getBanner();
    IInterstitialAd getInterstitial();
    IRewardedVideo getRewardedVideo();
    void initializeAds(final Activity activity , final IAdsInitializeCallBack adsInitializeCallBack);
    void initializeSdk(final Activity activity, ISdkInitializeCallBack adsInitializeCallBack);
    boolean isSdkInitialized();
    void loadBanner(final Activity activity);
    void loadInterstitial(final Activity activity);
    void loadRv(final Activity activity);
    void showBanner(final Activity activity);
    void showInterstitial(final Activity activity);
    void showRv(final Activity activity);
}
