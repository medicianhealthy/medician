package com.robinzon.madician.ads.adsproviders.admob;

import android.app.Activity;

import com.robinzon.madician.ads.rootclasses.AdsProvider;
import com.robinzon.madician.utils.Validator;

public class AdMobAdProvider extends AdsProvider {

    protected AdMobBanner mBannerAd;
    protected AdMobInterstitial mInterstitial;
    protected AdMobRewardedVideo mRewardedVideo;

    public AdMobAdProvider() {
        mInterstitial = new AdMobInterstitial();
        mBannerAd = new AdMobBanner();
        mRewardedVideo = new AdMobRewardedVideo();
    }

    public boolean hasBanner(){
        return Validator.isValidObject(getBanner()) &&
                getBanner().hasAd() ;
    }

    public AdMobBanner getBanner() {
        return mBannerAd;
    }

    public boolean hasInterstitial(){
        return Validator.isValidObject(getInterstitial()) &&
                getInterstitial().hasAd();
    }

    public AdMobInterstitial getInterstitial() {
        return mInterstitial;
    }

    public boolean hasRv(){
        return Validator.isValidObject(getRewardedVideo()) &&
                getRewardedVideo().hasAd();
    }

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
}

