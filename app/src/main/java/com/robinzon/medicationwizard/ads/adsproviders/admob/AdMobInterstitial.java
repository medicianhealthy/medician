package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.IAdDisplayingEvent;
import com.robinzon.medicationwizard.ads.IAdLoadingEvents;
import com.robinzon.medicationwizard.ads.interfaces.IAd;
import com.robinzon.medicationwizard.ads.interfaces.IInterstitialAd;
import com.robinzon.medicationwizard.ads.rootclasses.ISuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.Validator;

public class AdMobInterstitial implements IInterstitialAd, IAd, ISuper {
    com.google.android.gms.ads.interstitial.InterstitialAd mInterstitial;
    private boolean mIsLoaded;
    private String mAdUnit;
    private boolean mIsShowing;

    @Override
    public void create(final Activity activity, final int adUnitResourceId) {
        if (Validator.isValidAndroidResourceId(adUnitResourceId)){
            setAdUnitId(activity.getString(adUnitResourceId));
        }
    }

    @Override
    public void load(Activity activity, IAdLoadingEvents adLoadingEvents) {
        if (Validator.isValidString(getAdUnitId())) {
            Logger.getInstance().logSingleTag(getClassName(),
                    AdsManager.LOG_INTERSTITIAL,
                    "Calling to load interstitial ad. Ad unit is [%s]", getAdUnitId());
            InterstitialAd.load(activity,
                    getAdUnitId(),
                    new AdRequest.Builder().build(),
                    getInterstitialAdLoadCallback(adLoadingEvents));
        } else {
            Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL , "Called to load interstitial but ad unit is null");
        }
    }

    private InterstitialAdLoadCallback getInterstitialAdLoadCallback(IAdLoadingEvents adLoadingEvents){
        return new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd) {
                mInterstitial = interstitialAd;
                setIsLoaded(true);
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial ad loaded");
                if(null != adLoadingEvents){
                    adLoadingEvents.onAdLoaded();
                }
                super.onAdLoaded(interstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitial = null;
                setIsLoaded(false);
                Logger.getInstance().logSingleTag(getClassName(),
                        AdsManager.LOG_INTERSTITIAL,
                        "Interstitial ad failed to load");
                if(null != adLoadingEvents){
                    adLoadingEvents.onAdFailedToLoad(loadAdError.getMessage());
                }
                super.onAdFailedToLoad(loadAdError);
            }
        };
    }

    @Override
    public void show(Activity activity, IAdDisplayingEvent adDisplayingEvent) {
        if (!isShowing()) {
            if (hasAd()) {
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Calling to show interstitial ad");
                mInterstitial.setFullScreenContentCallback(getFullScreenContentCallback());
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Showing interstitial ad");
                mInterstitial.show(activity);
            } else {
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL , "Called to show interstitial but there is no ad");
            }
        } else {
            Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Called to show interstitial but already showing");
        }
    }

    private FullScreenContentCallback getFullScreenContentCallback(){
        return new FullScreenContentCallback() {
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                setIsShowing(false);
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial as failed to show");
                super.onAdFailedToShowFullScreenContent(adError);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                setIsShowing(true);
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial as showing");
                super.onAdShowedFullScreenContent();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                setIsShowing(false);
                Logger.getInstance().logSingleTag(getClassName(),  AdsManager.LOG_INTERSTITIAL, "Interstitial as dismissed");
                super.onAdDismissedFullScreenContent();
            }

            @Override
            public void onAdImpression() {
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial as impression");
                super.onAdImpression();
            }

            @Override
            public void onAdClicked() {
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial as clicked");
                super.onAdClicked();
            }
        };
    }

    @Override
    public void setIsShowing(boolean isShowing) {
        mIsShowing = isShowing;
    }

    @Override
    public boolean hasAd() {
        return null != mInterstitial && isLoaded() ;
    }

    @Override
    public boolean isLoaded() {
        return mIsLoaded;
    }

    @Override
    public void setIsLoaded(boolean isLoaded) {
        mIsLoaded = isLoaded;
    }

    @Override
    public boolean isShowing() {
        return mIsShowing;
    }

    @Override
    public String getAdUnitId() {
        return mAdUnit;
    }

    @Override
    public void setAdUnitId(String adUnitId) {
        if (Validator.Ads.isValidAdMobAdUnitId(adUnitId)) {
            mAdUnit = adUnitId;
        }
    }

    @Override
    public void callOnResume(Activity activity) {

    }

    @Override
    public void callOnPause(Activity activity) {

    }

    @Override
    public void callOnDestroy(Activity activity) {

    }

    @Override
    public void callOnCreate(Activity activity) {

    }

    @Override
    public String getClassName() {
        return "{AdMobInterstitial}";
    }
}
