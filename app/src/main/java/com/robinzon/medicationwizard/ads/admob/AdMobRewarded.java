package com.robinzon.medicationwizard.ads.admob;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.robinzon.medicationwizard.ads.AdAction;
import com.robinzon.medicationwizard.ads.AdPlacement;
import com.robinzon.medicationwizard.ads.AdType;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;
import com.robinzon.medicationwizard.utils.NetworkUtils;

import java.util.Timer;
import java.util.TimerTask;

public class AdMobRewarded extends AdMobAd {

    RewardedAd mRewardedAd;
    private AdsManager.OnRewardedFinishedListener mRewardedFinishedListener;
    private boolean mUserEarnedReward = false;

    public AdMobRewarded(@NonNull String adUnitId, @NonNull AdsManager adsManager, @NonNull AdPlacement placement) {
        super(adUnitId, adsManager, placement);
        log("%s Creating object.\n%s", getLogTag(), thisToString());
    }

    public void setRewardedFinishedListener(AdsManager.OnRewardedFinishedListener listener) {
        this.mRewardedFinishedListener = listener;
    }

    @NonNull
    private String thisToString() {
        return AdMobRewarded.this.toString();
    }

    @Override
    public AdType getAdType() {
        return AdType.Rewarded;
    }

    @Override
    public void load() {
        log("%s Requesting load.\n%s", getLogTag(), thisToString());
        if (Boolean.TRUE.equals(shouldBeLoaded())) {
            getAdsManager().onAdAction(AdMobRewarded.this, AdAction.StartingToLoad);
            setIsLoading(true);
            log("%s Preparing for loading.\n%s", getLogTag(), thisToString());
            final RewardedAdLoadCallback rewardedAdLoadCallback = new RewardedAdLoadCallback() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    // Handle the error.
                    mRewardedAd = null;
                    setIsLoaded(false);
                    setIsLoading(false);
                    failedToLoad(loadAdError);
                    getAdsManager().onAdAction(AdMobRewarded.this, AdAction.FailedToLoad);
                    log("%s Failed to load. Reason is %s.\n%s", getLogTag(), loadAdError.getMessage(), thisToString());
                }

                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    mRewardedAd = ad;
                    setIsLoading(false);
                    setIsLoaded(true);
                    loaded();
                    getAdsManager().onAdAction(AdMobRewarded.this, AdAction.LoadedSuccessfully);
                    log("%s Loaded. Adapter is %s.\n%s", getLogTag(), getLastWord(ad.getResponseInfo().getMediationAdapterClassName()), thisToString());
                }
            };
            log("%s Loading.\n%s", getLogTag(), thisToString());
            RewardedAd.load(getActivity(), getAdUnitId(), getAdRequest(), rewardedAdLoadCallback);
        } else {
            log("%s Refusing load. Has network %b. \n%s",
                    getLogTag(),
                    NetworkUtils.isNetworkAvailable(getContext().getApplicationContext()),
                    thisToString());
        }
    }

    @Override
    public void show() {
        if (shouldShow() && canShow()) {
            mUserEarnedReward = false;
            mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    // Called when a click is recorded for an ad.
                    log("%s Clicked.\n%s", getLogTag(), thisToString());
                    getAdsManager().onAdAction(AdMobRewarded.this, AdAction.Clicked);
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    mRewardedAd = null;
                    setIsShowing(false);
                    setIsLoaded(false);
                    setIsLoading(false);

                    if (mRewardedFinishedListener != null) {
                        mRewardedFinishedListener.onRewarded(mUserEarnedReward ? AdsManager.RewardedStatus.SUCCESS : AdsManager.RewardedStatus.DISMISSED_EARLY);
                        mRewardedFinishedListener = null;
                    }

                    getAdsManager().onAdAction(AdMobRewarded.this, AdAction.Dismissed);

                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            getActivity().runOnUiThread(AdMobRewarded.this::load);
                        }
                    }, 500L);

                    log("%s Dismissed.\n%s", getLogTag(), thisToString());

                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    // Called when ad fails to show.
                    mRewardedAd = null;
                    setIsShowing(false);
                    setIsLoaded(false);
                    setIsLoading(false);
                    if (mRewardedFinishedListener != null) {
                        mRewardedFinishedListener.onRewarded(AdsManager.RewardedStatus.NOT_READY);
                        mRewardedFinishedListener = null;
                    }
                    log("%s Failed to show. Reason is %s.\n%s", getLogTag(), adError.getMessage(), thisToString());
                    getAdsManager().onAdAction(AdMobRewarded.this, AdAction.FailedToShow);
                }

                @Override
                public void onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    getAdsManager().onAdAction(AdMobRewarded.this, AdAction.Impression);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    setIsShowing(true);
                    setIsLoaded(false);
                    setIsLoading(false);
                    log("%s Showed.\n%s", getLogTag(), thisToString());
                    getAdsManager().onAdAction(AdMobRewarded.this, AdAction.Showing);
                }
            });
            mRewardedAd.show(getActivity(), new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    mUserEarnedReward = true;
                    log("%s Rewarded.\n%s", getLogTag(), thisToString());
                    getAdsManager().onAdAction(AdMobRewarded.this, AdAction.Rewarding);
                }
            });
        } else {
            // AD CANNOT BE SHOWN: Notify listener so UI can respond (e.g. show "Not Ready" toast)
            if (mRewardedFinishedListener != null) {
                mRewardedFinishedListener.onRewarded(AdsManager.RewardedStatus.NOT_READY);
                mRewardedFinishedListener = null;
            }
        }
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public boolean shouldShow() {
        // user initiated reward should show even if premium to allow extension 
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
        return mRewardedAd;
    }


    @Override
    public void onDestroy() {

    }

}
