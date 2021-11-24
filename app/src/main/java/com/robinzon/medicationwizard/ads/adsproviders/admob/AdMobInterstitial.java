package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.AdDisplayingEvent;
import com.robinzon.medicationwizard.ads.AdLoadingEvents;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.interfaces.InterstitialAdActions;
import com.robinzon.medicationwizard.ads.rootclasses.InterstitialAd;
import com.robinzon.medicationwizard.utils.Logger;

public class AdMobInterstitial extends InterstitialAd implements InterstitialAdActions {
    com.google.android.gms.ads.interstitial.InterstitialAd mInterstitial;
    @Override
    public void create(Activity activity, int adUnitResourceId) {
        setAdUnitId(activity.getString(adUnitResourceId));
    }

    @Override
    public String getClassName() {
        return "{AdMobInterstitial}";
    }

    @Override
    public void load(Activity activity, AdLoadingEvents adLoadingEvents) {
        Logger.logSingleTag(getClassName(),
                AdsManager.LOG_INTERSTITIAL,
                "Calling to load interstitial ad. Ad unit is [%s]",
                isTestAdUnitID(activity) ? "TEST - ".concat(getAdUnitId()) : getAdUnitId());
        com.google.android.gms.ads.interstitial.InterstitialAd.load(activity,
                getAdUnitId(),
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd) {
                        super.onAdLoaded(interstitialAd);
                         mInterstitial = interstitialAd;
                        Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial ad loaded");
                        if(null != adLoadingEvents){
                            adLoadingEvents.onAdLoaded();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        mInterstitial = null;
                        Logger.logSingleTag(getClassName(),
                                AdsManager.LOG_INTERSTITIAL,
                                "Interstitial ad failed to load");
                        if(null != adLoadingEvents){
                            adLoadingEvents.onAdFailedToLoad(loadAdError.getMessage());
                        }
                    }
                });
    }

    @Override
    public void show(Activity activity, AdDisplayingEvent adDisplayingEvent) {
        Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Calling to show interstitial ad");
        mInterstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial as failed to show");
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial as showing");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                Logger.logSingleTag(getClassName(),  AdsManager.LOG_INTERSTITIAL, "Interstitial as dismissed");
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial as impression");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial as clicked");
            }
        });
        Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Showing interstitial ad");
        mInterstitial.show(activity);
    }

    @Override
    public boolean hasAd() {
        return null != mInterstitial;
    }

    @Override
    public boolean isTestAdUnitID(Context context) {
        return getAdUnitId().contentEquals(context.getString(R.string.admob_interstitial_id_test));
    }
}
