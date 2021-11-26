package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;

import com.robinzon.medicationwizard.ads.interfaces.IAdsProvider;
import com.robinzon.medicationwizard.utils.Validator;

public class AdMobAdProvider implements IAdsProvider {

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
}

