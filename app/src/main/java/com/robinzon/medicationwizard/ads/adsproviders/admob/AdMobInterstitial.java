package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.EAdType;
import com.robinzon.medicationwizard.ads.interfaces.IAdsLifeCycleCallBack;
import com.robinzon.medicationwizard.ads.rootclasses.EAdPlacement;
import com.robinzon.medicationwizard.ads.rootclasses.Interstitial;
import com.robinzon.medicationwizard.utils.Logger;

import java.util.ArrayList;

public final class AdMobInterstitial extends Interstitial {
    private InterstitialAd mInterstitialAd;

    public AdMobInterstitial(Activity act, EAdPlacement placement) {
        super(act, placement);
        setLogTags(new ArrayList<String>(1) {{
            add(getClassName());
        }});
    }

    @Override
    public void load() {
        load(null);
    }

    @Override
    public void load(final IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        if (shouldLoad()) {
            logMessage("Got a call to load interstitial. Starting to load now");
            InterstitialAd.load(getActivity(), getAdUnitId(), getAdRequest(), getLoadCallBack(adsLifeCycleCallBack));
        } else if (Logger.isLoggingEnabled()){
            logMessageOnInterstitialShouldNotBeLoaded();
        }
    }

    private InterstitialAdLoadCallback getLoadCallBack(final IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        return new InterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                AdMobInterstitial.super.handleAdCallBacks(EAdCallBacks.FAILED_TO_LOAD, adsLifeCycleCallBack, loadAdError);
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                super.onAdLoaded(interstitialAd);
                mInterstitialAd = interstitialAd;
                AdMobInterstitial.super.handleAdCallBacks(EAdCallBacks.LOADED, adsLifeCycleCallBack);
            }
        };
    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    @Override
    public boolean canShow() {
        return null != mInterstitialAd && super.canShow();
    }

    @Override
    public void show() {
        show(null);
    }

    @Override
    public void show(IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        if (canShow()) {
            logMessage("Got a call to show interstitial. Showing now");
            mInterstitialAd.setFullScreenContentCallback(getFullScreenContentCallBack(adsLifeCycleCallBack));
            mInterstitialAd.show(getActivity());
        } else if (Logger.isLoggingEnabled()) {
            logMessageOnInterstitialCantBeShown();
        }
    }

    private FullScreenContentCallback getFullScreenContentCallBack(IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        return new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AdMobInterstitial.super.handleAdCallBacks(EAdCallBacks.CLICKED, adsLifeCycleCallBack);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                AdMobInterstitial.super.handleAdCallBacks(EAdCallBacks.DISMISSED, adsLifeCycleCallBack);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                AdMobInterstitial.super.handleAdCallBacks(EAdCallBacks.FAILED_TO_SHOW, adsLifeCycleCallBack, adError);
            }


            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                AdMobInterstitial.super.handleAdCallBacks(EAdCallBacks.SHOWN, adsLifeCycleCallBack);
            }
        };
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onCreate() {

    }

    @Override
    public EAdType getAdType() {
        return EAdType.INTERSTITIAL;
    }

    @Override
    public Object getAdCoreObject() {
        return mInterstitialAd;
    }
}
