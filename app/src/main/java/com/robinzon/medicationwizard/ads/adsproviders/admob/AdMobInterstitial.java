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

public final class AdMobInterstitial extends Interstitial {
    private InterstitialAd mInterstitialAd;

    public AdMobInterstitial(Activity act, EAdPlacement placement) {
        super(act, placement);
    }



    @Override
    public void load() {
        load(null);
    }

    @Override
    public void load(final IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        if (shouldLoad()) {
            InterstitialAd.load(getActivity(), getAdUnitId(), getAdRequest(), getLoadCallBack(adsLifeCycleCallBack));
        }
    }

    private InterstitialAdLoadCallback getLoadCallBack(final IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        return new InterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                handleAdCallBacks(EAdCallBacks.FAILED_TO_LOAD, adsLifeCycleCallBack);
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                super.onAdLoaded(interstitialAd);
                handleAdCallBacks(EAdCallBacks.LOADED, adsLifeCycleCallBack);
                mInterstitialAd = interstitialAd;
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
            mInterstitialAd.setFullScreenContentCallback(getFullScreenContentCallBack(adsLifeCycleCallBack));
            mInterstitialAd.show(getActivity());
        }
    }

    private FullScreenContentCallback getFullScreenContentCallBack(IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        return new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                handleAdCallBacks(EAdCallBacks.CLICKED, adsLifeCycleCallBack);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                handleAdCallBacks(EAdCallBacks.DISMISSED, adsLifeCycleCallBack);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                handleAdCallBacks(EAdCallBacks.FAILED_TO_SHOW, adsLifeCycleCallBack);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                handleAdCallBacks(EAdCallBacks.SHOWN, adsLifeCycleCallBack);
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
