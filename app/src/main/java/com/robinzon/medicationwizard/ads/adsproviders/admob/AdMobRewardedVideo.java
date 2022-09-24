package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.rootclasses.RewardedVideo;

public final class AdMobRewardedVideo extends RewardedVideo {
    private RewardedAd mRewardedVideo;

    public AdMobRewardedVideo(Activity activity, String placement) {
        super(activity, placement);
    }



    @Override
    public void load() {
        if (shouldLoad()) {
            RewardedAd.load(getActivity(), getAdUnitId(), getAdRequest(), getAdLoadCallBack());
        }
    }

    private RewardedAdLoadCallback getAdLoadCallBack() {
        return new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                handleAdCallBacks(EAdCallBacks.FAILED_TO_LOAD);
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                super.onAdLoaded(rewardedAd);
                mRewardedVideo = rewardedAd;
                handleAdCallBacks(EAdCallBacks.LOADED);
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
        if (canShow()){
            mRewardedVideo.show(getActivity(), getOnRewardListener());
        }
    }

    private OnUserEarnedRewardListener getOnRewardListener() {
        return new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                handleAdCallBacks(EAdCallBacks.REWARDED);
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

}
