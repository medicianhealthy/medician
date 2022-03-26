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
import com.robinzon.medicationwizard.ads.rootclasses.RewardedVideo;
import com.robinzon.medicationwizard.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class AdMobRewardedVideo extends RewardedVideo {
    private RewardedAd mRewardedVideo;
    private long mLastSuccessfulLoadTimeStamp;

    @Override
    public void create(Activity activity, int adUnitIdResourceId) {
        final String adUnitResolved = activity.getString(adUnitIdResourceId);
        if (Logger.isLoggingEnabled()) {
            Logger.getInstance().log(getClassName(), getAdMobRvLogTags(), "RV Setting ad unit to [%s]", adUnitResolved);
        }
        setAdUnitId(adUnitResolved);
    }

    private List<String> getAdMobRvLogTags() {
        return new ArrayList<String>() {{
            add(AdsManager.LOG_REWARDED_VIDEO);
        }};
    }

    @Override
    public void load(Activity activity) {
        if (Logger.isLoggingEnabled()) {
            Logger.getInstance().log(getClassName(), getAdMobRvLogTags(), "Calling to load rv");
        }
        if (!mIsLoaded.get() && mIsInLoadingProgress.compareAndSet(false, true)) {
            if (Logger.isLoggingEnabled()) {
                Logger.getInstance().log(getClassName(), getAdMobRvLogTags(), "Starting to load rv");
            }
            RewardedAd.load(activity, getAdUnitId(), getAdRequest(), getRewardedAdLoadCallBack());
        } else {
            if (Logger.isLoggingEnabled()) {
                Logger.getInstance().log(getClassName(),
                        getAdMobRvLogTags(),
                        "Can not load rv. It's already loaded or in loading progress");
            }
        }
    }

    private RewardedAdLoadCallback getRewardedAdLoadCallBack() {
        return new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                setIsLoaded(false);
                mIsInLoadingProgress.set(false);
                markLoadFailAttempt();
                mRewardedVideo = null;
                if (Logger.isLoggingEnabled()) {
                    Logger.getInstance().log(getClassName(),
                            getAdMobRvLogTags(),
                            "Rv failed to load. Reason is [%s]",loadAdError.getMessage());
                }
                super.onAdFailedToLoad(loadAdError);
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                setIsLoaded(true);
                mIsInLoadingProgress.set(false);
                setRetryAttemptsToZero();
                mRewardedVideo = rewardedAd;
                if (Logger.isLoggingEnabled()) {
                    Logger.getInstance().log(getClassName(),
                            getAdMobRvLogTags(),
                            "Rv ad loaded successfully");
                }
                setLastSuccessfulLoadTimeStamp();
                super.onAdLoaded(rewardedAd);
            }
        };
    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    @Override
    public void show(Activity activity, IAdDisplayingEvent adDisplayingEvent) {
        if (Logger.isLoggingEnabled()) {
            Logger.getInstance().log(getClassName(),
                    getAdMobRvLogTags(),
                    "Calling to show rv");
        }
        if (mIsLoaded.get() && !mIsShowing.get()) {
            if (null == mRewardedVideo.getFullScreenContentCallback()) {
                mRewardedVideo.setFullScreenContentCallback(getFullScreenContentCallBack(activity));
            }
            mRewardedVideo.show(activity, getOnUserEarnedRewardListener(activity));
        } else {
            if (Logger.isLoggingEnabled()) {
                Logger.getInstance().log(getClassName(),
                        getAdMobRvLogTags(),
                        "Can not show rv. It's not loaded or already showing");
            }
        }
    }

    private FullScreenContentCallback getFullScreenContentCallBack(Activity activity) {
        return new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                if (Logger.isLoggingEnabled()) {
                    Logger.getInstance().log(getClassName(),
                            getAdMobRvLogTags(),
                            "Rv clicked");
                }
                super.onAdClicked();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                mIsShowing.set(false);
                if (Logger.isLoggingEnabled()) {
                    Logger.getInstance().log(getClassName(),
                            getAdMobRvLogTags(),
                            "Rv closed");
                }
                setIsLoaded(false);
                load(activity);
                super.onAdDismissedFullScreenContent();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                mIsShowing.set(false);
                if (Logger.isLoggingEnabled()) {
                    Logger.getInstance().log(getClassName(),
                            getAdMobRvLogTags(),
                            "Rv failed to show. Reason is [%s]",adError.getMessage());
                }
                super.onAdFailedToShowFullScreenContent(adError);
            }

            @Override
            public void onAdImpression() {
                if (Logger.isLoggingEnabled()) {
                    Logger.getInstance().log(getClassName(),
                            getAdMobRvLogTags(),
                            "Rv impression");
                }
                super.onAdImpression();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                mIsShowing.set(true);
                if (Logger.isLoggingEnabled()) {
                    Logger.getInstance().log(getClassName(),
                            getAdMobRvLogTags(),
                            "Rv showed");
                }
                super.onAdShowedFullScreenContent();
            }
        };
    }

    private OnUserEarnedRewardListener getOnUserEarnedRewardListener(final Activity activity) {
        return new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                if (Logger.isLoggingEnabled()) {
                    Logger.getInstance().log(getClassName(),
                            getAdMobRvLogTags(),
                            "Rv rewarded item [%s] in an amount of [%d]",rewardItem.getType(), rewardItem.getAmount());
                }

            }
        };
    }

    @Override
    public boolean hasAd() {
        return null != mRewardedVideo && mIsLoaded.get() && !isExpired();
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
