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
import com.robinzon.medicationwizard.ads.IAdDisplayingEvent;
import com.robinzon.medicationwizard.ads.IAdLoadingEvents;
import com.robinzon.medicationwizard.ads.rootclasses.RewardedVideo;

public class AdMobRewardedVideo extends RewardedVideo {
    private RewardedAd mRewardedVideo;

    @Override
    public void create(Activity activity, int adUnitIdResourceId) {
        setAdUnitId(activity.getString(adUnitIdResourceId));
    }

    @Override
    public void load(Activity activity) {
        if (!mIsLoaded.get() && mIsInLoadingProgress.compareAndSet(false, true)) {
            RewardedAd.load(activity, getAdUnitId(), getAdRequest(), getRewardedAdLoadCallBack());
        }
    }

    private RewardedAdLoadCallback getRewardedAdLoadCallBack() {
        return new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                setIsLoaded(false);
                mIsInLoadingProgress.set(false);
                mRetryAttempts++;
                mRewardedVideo = null;
                super.onAdFailedToLoad(loadAdError);
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                setIsLoaded(true);
                mIsInLoadingProgress.set(false);
                mRetryAttempts = 0;
                super.onAdLoaded(rewardedAd);
            }
        };
    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    @Override
    public void show(Activity activity, IAdDisplayingEvent adDisplayingEvent) {
        if (mIsLoaded.get() && !mIsShowing.get()) {
            if (null == mRewardedVideo.getFullScreenContentCallback()) {
                mRewardedVideo.setFullScreenContentCallback(getFullScreenContentCallBack());
            }
            mRewardedVideo.show(activity, getOnUserEarnedRewardListener());
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
                mIsShowing.set(false);
                super.onAdDismissedFullScreenContent();
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

    private OnUserEarnedRewardListener getOnUserEarnedRewardListener() {
        return new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {

            }
        };
    }

    @Override
    public boolean hasAd() {
        return null != mRewardedVideo && mIsLoaded.get();
    }

    @Override
    public boolean isLoaded() {
        return mIsLoaded.get();
    }

    @Override
    public void setIsLoaded(boolean isLoaded) {
        mIsLoaded.set(isLoaded);
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
    public void setRewardAmount(int amount) {

    }

    @Override
    public int getRewardAmount() {
        return 0;
    }

    @Override
    public String getClassName() {
        return AdMobRewardedVideo.class.getSimpleName();
    }
}
