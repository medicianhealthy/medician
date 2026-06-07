package com.robinzon.medicationwizard.ads.admob;

import android.view.View;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.robinzon.medicationwizard.ads.AdAction;
import com.robinzon.medicationwizard.ads.AdPlacement;
import com.robinzon.medicationwizard.ads.AdType;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;
import com.robinzon.medicationwizard.utils.NetworkUtils;

import java.util.Timer;
import java.util.TimerTask;

public class AdMobAppOpen extends AdMobAd {
    private AppOpenAd mAppOpenAd;

    public AdMobAppOpen(@NonNull String adUnitId, @NonNull AdsManager adsManager, @NonNull AdPlacement placement) {
        super(adUnitId, adsManager, placement);
    }

    @Override
    public AdType getAdType() {
        return AdType.AppOpen;
    }

    @Override
    public void load() {
        log("%s Requesting load.\n%s",getLogTag() , thisToString());
        if (Boolean.TRUE.equals(shouldBeLoaded())) {
            log("%s Preparing for loading.\n%s",getLogTag(), thisToString());
            getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.StartingToLoad);
            setIsLoading(true);
            AppOpenAd.load(getContext().getApplicationContext(),
                    getAdUnitId(),
                    getAdRequest(),
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            super.onAdFailedToLoad(loadAdError);
                            setIsLoading(false);
                            setIsLoaded(false);
                            mAppOpenAd = null;
                            failedToLoad(loadAdError);
                            log("%s Failed to load. Reason is %s.\n%s",getLogTag() ,loadAdError.getMessage(), thisToString());
                            getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.FailedToLoad);
                        }

                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                            super.onAdLoaded(appOpenAd);
                            setIsLoading(false);
                            setIsLoaded(true);
                            mAppOpenAd = appOpenAd;
                            loaded();
                            log("%s Loaded. Adapter is %s.\n%s",getLogTag() , getLastWord(appOpenAd.getResponseInfo().getMediationAdapterClassName()), thisToString());
                            getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.LoadedSuccessfully);
                        }

                    });
        } else {
            log("%s Refusing load. Has network %b. \n%s",
                    getLogTag() ,
                    NetworkUtils.isNetworkAvailable(getContext().getApplicationContext()),
                    thisToString());

        }
    }

    @Override
    public void show() {
        if (shouldShow() && canShow()) {
            setIsShowing(true);
            mAppOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    log("%s Clicked.\n%s",getLogTag() , thisToString());
                    getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.Clicked);
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    mAppOpenAd = null;
                    setIsShowing(false);
                    setIsLoaded(false);
                    setIsLoading(false);
                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            getActivity().runOnUiThread(AdMobAppOpen.this::load);
                            getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.Dismissed);
                        }
                    },500L);

                    log("%s Dismissed.\n%s",getLogTag() , thisToString());
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    mAppOpenAd = null;
                    setIsShowing(false);
                    setIsLoaded(false);
                    setIsLoading(false);
                    log("%s Failed to show. Reason is %s.\n%s",getLogTag() ,adError.getMessage(), thisToString());
                    getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.FailedToShow);
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.Impression);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();
                    setIsShowing(true);
                    setIsLoaded(false);
                    setIsLoading(false);
                    log("%s Showed.\n%s",getLogTag() , thisToString());
                    getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.Showing);
                }
            });
            mAppOpenAd.show(getActivity());
        }
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @NonNull
    private String thisToString() {
        return AdMobAppOpen.this.toString();
    }

    @Override
    public boolean shouldShow() {
        if (com.robinzon.medicationwizard.AppConfig.IS_PREMIUM && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE) {
            return false;
        }
        return getAdsManager().hasCoolDownForFullScreenNonUserInitiatedAd();
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
        return mAppOpenAd;
    }

    @Override
    public void onDestroy() {

    }
}
