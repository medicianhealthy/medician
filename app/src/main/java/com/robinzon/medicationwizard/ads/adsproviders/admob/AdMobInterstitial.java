package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.robinzon.medicationwizard.ads.IAdDisplayingEvent;
import com.robinzon.medicationwizard.ads.IAdLoadingEvents;
import com.robinzon.medicationwizard.ads.rootclasses.Interstitial;

public class AdMobInterstitial extends Interstitial {
    private InterstitialAd mInterstitialAd;

    @Override
    public void create(Activity activity, int adUnitIdResourceId) {
        setAdUnitId(activity.getString(adUnitIdResourceId));
    }

    @Override
    public void load(final Activity activity) {
        if (mIsInLoadingProgress.compareAndSet(false, true)) {
            InterstitialAd.load(activity, getAdUnitId(), getAdRequest(), getInterstitialAdLoadCallBack());
        }
    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    private InterstitialAdLoadCallback getInterstitialAdLoadCallBack() {
        return new InterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                mInterstitialAd = null;
                mIsLoaded.set(false);
                mIsInLoadingProgress.set(false);
                mRetryAttempts++;
                if (null != mLoadingEventsListener){
                    mLoadingEventsListener.onAdFailedToLoad(loadAdError.getMessage());
                }
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                super.onAdLoaded(interstitialAd);
                mInterstitialAd = interstitialAd;
                mIsLoaded.set(true);
                mIsInLoadingProgress.set(false);
                mRetryAttempts = 0;
                if (null != mLoadingEventsListener){
                    mLoadingEventsListener.onAdLoaded();
                }
            }
        };
    }

    @Override
    public void show(Activity activity, IAdDisplayingEvent adDisplayingEvent) {
        if (null != mInterstitialAd && mIsLoaded.get() && !mIsShowing.get()) {
            if (null == mInterstitialAd.getFullScreenContentCallback()) {
                mInterstitialAd.setFullScreenContentCallback(getFullScreenContentCallBack());
            }
            mInterstitialAd.show(activity);
        }
    }

    private FullScreenContentCallback getFullScreenContentCallBack() {
        return new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                mIsShowing.set(false);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                mIsShowing.set(false);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                mIsShowing.set(true);
            }
        };
    }

    @Override
    public boolean hasAd() {
        return null != mInterstitialAd && mIsLoaded.get();
    }

    @Override
    public boolean isLoaded() {
        return mIsLoaded.get();
    }

    @Override
    public void setIsLoaded(boolean isLoaded) {

    }

    @Override
    public boolean isShowing() {
        return mIsShowing.get();
    }

    @Override
    public void setIsShowing(boolean isShowing) {
        mIsShowing.set(isShowing);
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
    public void setLoadingEventsListener(IAdLoadingEvents loadingEvents) {
        mLoadingEventsListener = loadingEvents;
    }

    @Override
    public String getClassName() {
        return AdMobInterstitial.class.getSimpleName();
    }
}
