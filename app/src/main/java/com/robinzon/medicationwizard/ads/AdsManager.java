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

public class AdsManager implements OnAdActionListener{


    private final Activity mActivity;
    private AdMobBanner mMainBanner;
    private AdMobInterstitial mMainInterstitial;
    private AdMobRewarded mMainRewarded;
    private AdMobAppOpen mAppOpenAd;

    private ArrayList<AdMobAd> mAdsCollections;
    private long mFullAdDismissedTimeStamp;
    private long mBannerClickTimeStamp;

    public AdsManager(final @NonNull Activity activity) {
        this.mActivity = activity;
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
            mMainBanner.load();
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
        if (null != mMainInterstitial && hasCoolDownForFullScreenNonUserInitiatedAd()) {
            mMainInterstitial.show();
        }
    }
    /** @noinspection unused*/
    public void showRewarded() {
        if (null != mMainRewarded) {
            mMainRewarded.show();
        }
    }

    public void showAppOpenAd() {
        if (null != mAppOpenAd && hasCoolDownForFullScreenNonUserInitiatedAd()) {
            mAppOpenAd.show();
        }
    }

    @Override
    public void onAdAction(@NonNull AdMobAd adMobAd, AdAction adAction) {
        final AdType adType = adMobAd.getAdType();
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
        return 30L;
    }
}
