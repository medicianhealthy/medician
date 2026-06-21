package com.robinzon.medicationwizard.ads;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.ads.admob.AdMobAppOpen;
import com.robinzon.medicationwizard.ads.admob.AdMobBanner;
import com.robinzon.medicationwizard.ads.admob.AdMobInterstitial;
import com.robinzon.medicationwizard.ads.admob.AdMobRewarded;
import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.NetworkUtils;
import com.robinzon.medicationwizard.utils.TimeManager;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class AdsManager implements OnAdActionListener{


    public interface OnRewardedFinishedListener {
        void onRewarded(boolean success);
    }

    private final Activity mActivity;
    private AdMobBanner mMainBanner;
    private AdMobInterstitial mMainInterstitial;
    private AdMobRewarded mMainRewarded;
    private AdMobAppOpen mAppOpenAd;

    private ArrayList<AdMobAd> mAdsCollections;
    private long mFullAdDismissedTimeStamp;
    private long mBannerClickTimeStamp;
    private final CopyOnWriteArrayList<Runnable> mAdAvailabilityListeners = new CopyOnWriteArrayList<>();

    public AdsManager(final @NonNull Activity activity) {
        this.mActivity = activity;
    }

    public void addAdAvailabilityListener(Runnable listener) {
        mAdAvailabilityListeners.add(listener);
    }

    public void removeAdAvailabilityListener(Runnable listener) {
        mAdAvailabilityListeners.remove(listener);
    }

    private void notifyAvailabilityChanged() {
        mActivity.runOnUiThread(() -> {
            for (Runnable listener : mAdAvailabilityListeners) {
                listener.run();
            }
        });
    }

    public Activity getActivity() {
        return mActivity;
    }

    public void initializeAds() {
        createAds();
        loadAds();
    }

    private void createAds() {
        if (NetworkUtils.isNetworkAvailable(getActivity())) {
            if (null == mMainBanner) {
                mMainBanner = new AdMobBanner(BuildConfig.DEBUG ? getTestAdForAdType(AdType.Banner) : "a",
                        this,
                        AdPlacement.Main);
                getAdsCollection().add(mMainBanner);

            }
            if (null == mMainInterstitial) {
                mMainInterstitial = new AdMobInterstitial(BuildConfig.DEBUG ? getTestAdForAdType(AdType.InterstitialVideo) : "z",
                        this,
                        AdPlacement.Main);
                getAdsCollection().add(mMainInterstitial);
            }
            if (null == mMainRewarded) {
                mMainRewarded = new AdMobRewarded(BuildConfig.DEBUG ? getTestAdForAdType(AdType.Rewarded) : "a",
                        this,
                        AdPlacement.Main);
                getAdsCollection().add(mMainRewarded);
            }
            if (null == mAppOpenAd) {
                mAppOpenAd = new AdMobAppOpen(BuildConfig.DEBUG ? getTestAdForAdType(AdType.AppOpen) : "a",
                        this,
                        AdPlacement.Main);
                getAdsCollection().add(mAppOpenAd);
            }
        }
    }

    public ArrayList<AdMobAd> getAdsCollection() {
        if (null == mAdsCollections) {
            mAdsCollections = new ArrayList<>();
        }
        return mAdsCollections;
    }

    private void loadAds() {
        if (null != mMainBanner) {
            float totalUsageMinutes = com.robinzon.medicationwizard.utils.Statisticator.getTotalUsageMinutes(mActivity);
            int minimumMinutesForBanner = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getMinAppTimeForBannerMins();
            if (totalUsageMinutes >= (float) minimumMinutesForBanner) {
                mMainBanner.load();
            } else {
                com.robinzon.medicationwizard.utils.Logger.log("AdsManager", "Banner load skipped. Usage mins: " + totalUsageMinutes + " < Min: " + minimumMinutesForBanner);
            }
        }
        if (null != mMainInterstitial) {
            mMainInterstitial.load();
        }
        if (null != mMainRewarded) {
            mMainRewarded.load();
        }

        if (null != mAppOpenAd) {
            mAppOpenAd.load();
        }
    }


    /**
     * @noinspection SameParameterValue
     */
    private @NonNull String getTestAdForAdType(@NonNull final AdType adType) {
        return switch (adType) {
            case AppOpen -> "ca-app-pub-3940256099942544/9257395921";
            case AdaptiveBanner -> "ca-app-pub-3940256099942544/9214589741";
            case Banner -> "ca-app-pub-3940256099942544/6300978111";
            case Interstitial -> "ca-app-pub-3940256099942544/1033173712";
            case InterstitialVideo -> "ca-app-pub-3940256099942544/8691691433";
            case Rewarded -> "ca-app-pub-3940256099942544/5224354917";
            case RewardedInterstitial -> "ca-app-pub-3940256099942544/5354046379";
            case NativeAdvanced -> "ca-app-pub-3940256099942544/2247696110";
            case NativeAdvancedVideo -> "ca-app-pub-3940256099942544/1044960115";
        };
    }

    public void onResume() {
        for (AdMobAd ad : getAdsCollection()) {
            if (null != ad) {
                ad.onResume();
            }
        }
    }

    public void onDestroy() {
        for (AdMobAd ad : getAdsCollection()) {
            if (null != ad) {
                ad.onDestroy();
            }
        }
    }

    public void onPause() {
        for (AdMobAd ad : getAdsCollection()) {
            if (null != ad) {
                ad.onPause();
            }
        }
    }

    /** @noinspection unused*/
    public void showInterstitialAd() {
        if (com.robinzon.medicationwizard.AppConfig.isPremium(mActivity) && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE) return;

        if (null != mMainInterstitial && hasCoolDownForFullScreenNonUserInitiatedAd()) {
            if (shouldShowInterstitialBasedOnUsage()) {
                mMainInterstitial.show();
            }
        }
    }

    private boolean shouldShowInterstitialBasedOnUsage() {
        final int sessionCount = com.robinzon.medicationwizard.utils.Statisticator.getSessionCount(mActivity);
        final float usageMinutesForAds = com.robinzon.medicationwizard.utils.Statisticator.getUsageMinutesForAds(mActivity);
        
        com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager remoteConfigManager = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance();
        int minimumSessionsThreshold = remoteConfigManager.getMinSessionsForInterstitial();
        int minimumMinutesThreshold = remoteConfigManager.getMinAppTimeForInterstitialMins();

        // Thresholds from Remote Config: Meets session count AND usage time since last ad
        return sessionCount >= minimumSessionsThreshold && usageMinutesForAds >= (float) minimumMinutesThreshold;
    }
    /** @noinspection unused*/
    public void showRewarded(OnRewardedFinishedListener listener) {
        if (null != mMainRewarded) {
            mMainRewarded.setRewardedFinishedListener(listener);
            mMainRewarded.show();
        } else if (listener != null) {
            listener.onRewarded(false);
        }
    }

    public boolean isRewardedLoaded() {
        return mMainRewarded != null && mMainRewarded.isLoaded();
    }

    public void showAppOpenAd() {
        if (null != mAppOpenAd && hasCoolDownForFullScreenNonUserInitiatedAd()) {
            mAppOpenAd.show();
        }
    }

    @Override
    public void onAdAction(@NonNull AdMobAd adMobAd, AdAction adAction) {
        final AdType adType = adMobAd.getAdType();
        
        if (adAction == AdAction.LoadedSuccessfully || adAction == AdAction.FailedToLoad || adAction == AdAction.Dismissed) {
            notifyAvailabilityChanged();
        }

        final String AD_ACTIONS = "medi_ad_actions";
        final String CLASS_NAME = AdsManager.class.getSimpleName();
        Logger.log(AD_ACTIONS, "%s ad action: %s, " +
                "%s.", CLASS_NAME, adType.name(), adAction.name());
        switch (adType) {
            case AppOpen, RewardedInterstitial, Interstitial, InterstitialVideo, Rewarded -> {
                if (AdAction.Dismissed == adAction) {
                    setFullScreenNonUserInitiatedAdDismissTimeStamp();
                }
            }
            case AdaptiveBanner, Banner -> {
                if (AdAction.Clicked == adAction) {
                    setBannerClickTimeStamp();
                }
                if (AdAction.Created == adAction){
                    ((OnAdActionListener)getActivity()).onAdAction(adMobAd, AdAction.Created);
                }
            }
            default -> {
            }
        }
    }

    private void setFullScreenNonUserInitiatedAdDismissTimeStamp() {
        this.mFullAdDismissedTimeStamp = System.currentTimeMillis();
        // FSA cooldown reset requirement
        com.robinzon.medicationwizard.utils.Statisticator.resetUsageMinutesForAds(mActivity);
    }

    private void setBannerClickTimeStamp() {
        this.mBannerClickTimeStamp = System.currentTimeMillis();
    }

    public long getFullScreenNonUserInitiatedAdDismissTimeStamp() {
        return mFullAdDismissedTimeStamp;
    }

    public long getBannerClickTimeStamp() {
        return mBannerClickTimeStamp;
    }

    public boolean hasCoolDownForFullScreenNonUserInitiatedAd() {
        final long coolDownMillis = TimeManager.getInstance().toMillisFromSeconds(getCoolDownSecondsForFullScreenNonUserInitiatedAd());
        final long now = System.currentTimeMillis();
        final long fullScreenNonUserInitiatedAdDismissTimeStamp = getFullScreenNonUserInitiatedAdDismissTimeStamp();
        final long lastBannerClick = getBannerClickTimeStamp();
        return (now - fullScreenNonUserInitiatedAdDismissTimeStamp) > coolDownMillis &&
                (now - lastBannerClick) >  coolDownMillis;
    }

    @SuppressWarnings("SameReturnValue")
    private long getCoolDownSecondsForFullScreenNonUserInitiatedAd() {
        // Refined Hybrid Cooldown: 120 seconds (2 minutes).
        // A compromise between the aggressive 60-90s of Casual Gaming and the 
        // high-trust expectations of a Medical Utility app.
        return 120L;
    }
}
