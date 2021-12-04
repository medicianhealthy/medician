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
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.IAdDisplayingEvent;
import com.robinzon.medicationwizard.ads.IAdLoadingEvents;
import com.robinzon.medicationwizard.ads.interfaces.IAd;
import com.robinzon.medicationwizard.ads.interfaces.IRewardedVideo;
import com.robinzon.medicationwizard.ads.rootclasses.ISuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.Validator;

public class AdMobRewardedVideo implements IRewardedVideo, IAd, ISuper {
    private RewardedAd mRewardedAd;
    private int mRewardAmount;
    private String mAdUnit;
    private boolean mIsLoaded;
    private boolean mIsShowing;

    @Override
    public void create(Activity mainActivity, int adUnitIdResourceId) {
        setAdUnitId(mainActivity.getString(adUnitIdResourceId));
    }


    @Override
    public String getClassName() {
        return "{AdMobRewardedVideo}";
    }

    @Override
    public void load(Activity activity, IAdLoadingEvents adLoadingEvents) {
        if (!isLoaded()) {
            if (Validator.isValidString(getAdUnitId())) {
                Logger.getInstance().logSingleTag(getClassName(),
                        AdsManager.LOG_REWARDED_VIDEO,
                        "AdMob Calling to load rv. Ad unit is [%s]", getAdUnitId());
                RewardedAd.load(activity, getAdUnitId(),
                        new AdRequest.Builder().build(),
                        getRewardedAdLoadCallback(adLoadingEvents)
                        );
            } else {
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Called to load rv but ad unit is null");
            }
        } else {
            Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Called to load rv but it is already loaded");
        }
    }

    private RewardedAdLoadCallback getRewardedAdLoadCallback(final IAdLoadingEvents adLoadingEvents){
        return new RewardedAdLoadCallback(){
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                setIsLoaded(true);
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,"Rv ad loaded");
                mRewardedAd = rewardedAd;
                if(null != adLoadingEvents){
                    adLoadingEvents.onAdLoaded();
                }
                super.onAdLoaded(rewardedAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                setIsLoaded(false);
                mRewardedAd = null;
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv Failed to load");
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
                mRewardedAd.setFullScreenContentCallback(getFullScreenContentCallback(adDisplayingEvent));
                mRewardedAd.show(activity, new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                        setRewardAmount(rewardItem.getAmount());
                        Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "User rewarded with [%d]",rewardItem.getAmount());
                    }
                });
            } else {
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO , "Called to show rv but there is no ad");
            }
        } else {
            Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO , "Called to show rv but already showing");
        }
    }

    private FullScreenContentCallback getFullScreenContentCallback(final IAdDisplayingEvent adDisplayingEvent){
        return new FullScreenContentCallback() {
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                setIsShowing(false);
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv failed to show. Reason is [%s]",adError.getMessage());
                if(null != adDisplayingEvent){
                    adDisplayingEvent.onAdFailedToShow();
                }
                super.onAdFailedToShowFullScreenContent(adError);

            }

            @Override
            public void onAdShowedFullScreenContent() {
                setIsShowing(true);
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv show");
                if(null != adDisplayingEvent){
                    adDisplayingEvent.onAdShown();
                }
                super.onAdShowedFullScreenContent();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                setIsShowing(true);
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv Dismissed");
                if(null != adDisplayingEvent){
                    adDisplayingEvent.onAdDismissed();
                }
                super.onAdDismissedFullScreenContent();
            }

            @Override
            public void onAdImpression() {
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv Impression");
                super.onAdImpression();
            }

            @Override
            public void onAdClicked() {
                Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO, "Rv clicked");
                super.onAdClicked();
            }
        };
    }

    @Override
    public void setRewardAmount(int amount) {
        mRewardAmount = amount;
    }

    @Override
    public int getRewardAmount() {
        return mRewardAmount;
    }


    @Override
    public boolean hasAd() {
        return null != mRewardedAd && isLoaded();
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
    public void setIsShowing(boolean isShowing) {
        mIsShowing = isShowing;
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
}
