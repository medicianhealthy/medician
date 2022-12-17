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
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.interfaces.IAdsLifeCycleCallBack;
import com.robinzon.medicationwizard.ads.rootclasses.EAdPlacement;
import com.robinzon.medicationwizard.ads.rootclasses.RewardedVideo;

import java.util.ArrayList;

public final class AdMobRewardedVideo extends RewardedVideo {
    private RewardedAd mRewardedVideo;

    public AdMobRewardedVideo(Activity activity, EAdPlacement placement) {
        super(activity, placement);
        setLogTags(new ArrayList<String>(1){{
            add(getClassName());
        }});
    }


    @Override
    public void load(IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        if (shouldLoad()) {
            RewardedAd.load(getActivity(), getAdUnitId(), getAdRequest(), getAdLoadCallBack(adsLifeCycleCallBack));
        }
    }

    @Override
    public void load() {
       load(null);
    }



    private RewardedAdLoadCallback getAdLoadCallBack(IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        return new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                AdMobRewardedVideo.super.handleAdCallBacks(EAdCallBacks.FAILED_TO_LOAD, adsLifeCycleCallBack, loadAdError);
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                super.onAdLoaded(rewardedAd);
                mRewardedVideo = rewardedAd;
                AdMobRewardedVideo.super.handleAdCallBacks(EAdCallBacks.LOADED, adsLifeCycleCallBack);
            }
        };
    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    @Override
    public boolean canShow() {
        return null != mRewardedVideo && super.canShow();
    }

    @Override
    public void show() {
        show(null);
    }

    @Override
    public void show(IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        if (canShow()){
            if (null == mRewardedVideo.getFullScreenContentCallback()){
                mRewardedVideo.setFullScreenContentCallback(getFullScreenContentCallBack(adsLifeCycleCallBack));
            }
            mRewardedVideo.show(getActivity(), getOnRewardListener(adsLifeCycleCallBack));
        }
    }

    private FullScreenContentCallback getFullScreenContentCallBack(IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        return new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                AdMobRewardedVideo.super.handleAdCallBacks(EAdCallBacks.CLICKED, adsLifeCycleCallBack);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                AdMobRewardedVideo.super.handleAdCallBacks(EAdCallBacks.DISMISSED, adsLifeCycleCallBack);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                AdMobRewardedVideo.super.handleAdCallBacks(EAdCallBacks.DISMISSED, adsLifeCycleCallBack, adError);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                AdMobRewardedVideo.super.handleAdCallBacks(EAdCallBacks.SHOWN, adsLifeCycleCallBack);
            }
        };
    }

    private OnUserEarnedRewardListener getOnRewardListener(final IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        return new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                AdMobRewardedVideo.super.handleAdCallBacks(EAdCallBacks.REWARDED, adsLifeCycleCallBack);
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
    public Object getAdCoreObject() {
        return mRewardedVideo;
    }

}
