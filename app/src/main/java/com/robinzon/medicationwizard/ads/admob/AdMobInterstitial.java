package com.robinzon.medicationwizard.ads.admob;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.robinzon.medicationwizard.ads.AdPlacement;
import com.robinzon.medicationwizard.ads.AdType;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;
import com.robinzon.medicationwizard.utils.NetworkUtils;

import java.util.Timer;
import java.util.TimerTask;

public class AdMobInterstitial extends AdMobAd {
    private InterstitialAdLoadCallback mAdLoadCallBack;
    private InterstitialAd mInterstitialAd;

    public AdMobInterstitial(@NonNull String adUnitId, @NonNull AdsManager adsManager, @NonNull AdPlacement placement) {
        super(adUnitId, adsManager, placement);
        log("%s Creating object.\n%s",getLogTag() , thisToString());
    }

    @NonNull
    private String thisToString() {
        return AdMobInterstitial.this.toString();
    }

    @Override
    public AdType getAdType() {
        return AdType.InterstitialVideo;
    }

    @Override
    public void load() {
        log("%s Requesting load.\n%s",getLogTag() , thisToString());
        if (shouldBeLoaded()) {
            log("%s Preparing for loading.\n%s",getLogTag(), thisToString());
            setIsLoaded(true);
            log("%s Loading.\n%s",getLogTag() , thisToString());
            InterstitialAd.load(getActivity(), getAdUnitId() , getAdRequest() , getAdLoadCallBack());
        } else {
            log("%s Refusing load. Has network %b. \n%s",
                    getLogTag() ,
                    NetworkUtils.isNetworkAvailable(getContext().getApplicationContext()),
                    thisToString());
        }
    }

    private InterstitialAdLoadCallback getAdLoadCallBack() {
        if (null == mAdLoadCallBack) {
            mAdLoadCallBack = new InterstitialAdLoadCallback(){
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    setIsLoaded(false);
                    setIsLoading(false);
                    mInterstitialAd = null;
                    failedToLoad(loadAdError);
                    log("%s Failed to load. Reason is %s.\n%s",getLogTag() ,loadAdError.getMessage(), thisToString());
                }

                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    super.onAdLoaded(interstitialAd);
                    setIsLoading(false);
                    setIsLoaded(true);
                    mInterstitialAd = interstitialAd;
                    loaded();
                    log("%s Loaded. Adapter is %s.\n%s",getLogTag() , getLastWord(interstitialAd.getResponseInfo().getMediationAdapterClassName()), thisToString());
                }
            };
        }
        return mAdLoadCallBack;
    }

    @Override
    public void show() {
        if (canShow() && shouldShow()) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                @Override
                public void onAdClicked() {
                    // Called when a click is recorded for an ad.
                    log("%s Clicked.\n%s",getLogTag() , thisToString());
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    setIsShowing(false);
                    setIsLoaded(false);
                    setIsLoading(false);
                    mInterstitialAd = null;
                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    load();
                                }
                            });
                        }
                    },500L);

                    log("%s Dismissed.\n%s",getLogTag() , thisToString());
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when ad fails to show.
                    mInterstitialAd = null;
                    setIsShowing(false);
                    setIsLoaded(false);
                    setIsLoading(false);
                    log("%s Failed to show. Reason is %s.\n%s",getLogTag() ,adError.getMessage(), thisToString());
                }

                @Override
                public void onAdImpression() {
                    // Called when an impression is recorded for an ad.
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    setIsShowing(true);
                    log("%s Showed.\n%s",getLogTag() , thisToString());
                }
            });
            mInterstitialAd.show(getActivity());
        }
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public boolean shouldShow() {
        return true;
    }

    @Override
    public void hide() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    @Override
    public Object getCoreAdObject() {
        return mInterstitialAd;
    }
}
