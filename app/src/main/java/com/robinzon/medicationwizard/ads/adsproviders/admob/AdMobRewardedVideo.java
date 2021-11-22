package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.robinzon.medicationwizard.ads.AdDisplayingEvent;
import com.robinzon.medicationwizard.ads.AdLoadingEvents;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.interfaces.RewardedVideoAdActions;
import com.robinzon.medicationwizard.ads.rootclasses.RewardedVideoAd;
import com.robinzon.medicationwizard.utils.Logger;

public class AdMobRewardedVideo extends RewardedVideoAd implements RewardedVideoAdActions {
    private RewardedAd mRewardedAd;
    @Override
    public void create(Activity mainActivity, int adUnitIdResourceId) {
        setAdUnitId(mainActivity.getString(adUnitIdResourceId));
    }

    @Override
    public String getClassName() {
        return "{AdMobRewardedVideo}";
    }

    @Override
    public void load(Activity activity, AdLoadingEvents adLoadingEvents) {
        Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "AdMob Calling to load rv");
        RewardedAd.load(activity, getAdUnitId(),
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback(){
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        super.onAdLoaded(rewardedAd);
                        Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,"Rv ad loaded");
                        mRewardedAd = rewardedAd;
                        if(null != adLoadingEvents){
                            adLoadingEvents.onAdLoaded();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        mRewardedAd = null;
                        Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv Failed to load");
                        if(null != adLoadingEvents){
                            adLoadingEvents.onAdFailedToLoad(loadAdError.getMessage());
                        }
                    }
                });
    }

    @Override
    public void show(Activity activity, AdDisplayingEvent adDisplayingEvent) {
        mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv failed to show. Reason is [%s]",adError.getMessage());
                if(null != adDisplayingEvent){
                    adDisplayingEvent.onAdFailedToShow();
                }

            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv show");
                if(null != adDisplayingEvent){
                    adDisplayingEvent.onAdShown();
                }
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv Dismissed");
                if(null != adDisplayingEvent){
                    adDisplayingEvent.onAdDismissed();
                }
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv Impression");
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv clicked");
            }
        });
        mRewardedAd.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                setRewardAmount(rewardItem.getAmount());
                Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "User rewarded with [%d]",rewardItem.getAmount());
            }
        });
    }

    @Override
    public boolean hasAd() {
        return null != mRewardedAd;
    }
}
