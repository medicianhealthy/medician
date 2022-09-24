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
import com.robinzon.medicationwizard.ads.rootclasses.Interstitial;

public final class AdMobInterstitial extends Interstitial {
    private InterstitialAd mInterstitialAd;

    public AdMobInterstitial(Activity act, String placement) {
        super(act, placement);
    }



    @Override
    public void load() {
        if (shouldLoad()) {
            InterstitialAd.load(getActivity(), getAdUnitId(), getAdRequest(), getLoadCallBack());
        }
    }

    private InterstitialAdLoadCallback getLoadCallBack() {
        return new InterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                handleAdCallBacks(EAdCallBacks.FAILED_TO_LOAD);
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                super.onAdLoaded(interstitialAd);
                handleAdCallBacks(EAdCallBacks.LOADED);
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
        if (canShow()) {
            mInterstitialAd.setFullScreenContentCallback(getFullScreenContentCallBack());
            mInterstitialAd.show(getActivity());
        }
    }

    private FullScreenContentCallback getFullScreenContentCallBack() {
        return new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                handleAdCallBacks(EAdCallBacks.CLICKED);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                handleAdCallBacks(EAdCallBacks.DISMISSED);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                handleAdCallBacks(EAdCallBacks.FAILED_TO_SHOW);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                handleAdCallBacks(EAdCallBacks.SHOWN);
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
}
