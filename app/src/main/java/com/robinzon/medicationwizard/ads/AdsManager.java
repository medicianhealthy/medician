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
import com.robinzon.medicationwizard.utils.NetworkMonitor;
import com.robinzon.medicationwizard.utils.TimeManager;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class AdsManager implements OnAdActionListener, NetworkMonitor.NetworkStatusListener{


    public interface OnRewardedFinishedListener {
        void onRewarded(boolean success);
    }

    private final Activity activity;
    private AdMobBanner mainBanner;
    private AdMobInterstitial mainInterstitial;
    private AdMobRewarded mainRewarded;
    private AdMobAppOpen appOpenAd;

    private ArrayList<AdMobAd> adsCollection;
    private long fullAdDismissedTimeStamp;
    private long bannerClickTimeStamp;
    private final CopyOnWriteArrayList<Runnable> adAvailabilityListeners = new CopyOnWriteArrayList<>();

    public AdsManager(final @NonNull Activity activity) {
        this.activity = activity;
    }

    public void addAdAvailabilityListener(Runnable listener) {
        adAvailabilityListeners.add(listener);
    }

    public void removeAdAvailabilityListener(Runnable listener) {
        adAvailabilityListeners.remove(listener);
    }

    private void notifyAvailabilityChanged() {
        activity.runOnUiThread(() -> {
            for (Runnable listener : adAvailabilityListeners) {
                listener.run();
            }
        });
    }

    /**
     * @return The currently active Activity context.
     */
    public Activity getActivity() {
        return activity;
    }

    /**
     * Performs one-time setup of ad units and initiates the first load requests.
     */
    public void initializeAds() {
        NetworkMonitor.getInstance(activity).addListener(this);
        createAds();
        loadAds();
    }

    /**
     * Instantiates the AdMob wrapper classes for each placement.
     */
    private void createAds() {
        if (null == mainBanner) {
            mainBanner = new AdMobBanner(BuildConfig.DEBUG ? getTestAdForAdType(AdType.Banner) : "a",
                    this,
                    AdPlacement.Main);
            getAdsCollection().add(mainBanner);

        }
        if (null == mainInterstitial) {
            mainInterstitial = new AdMobInterstitial(BuildConfig.DEBUG ? getTestAdForAdType(AdType.InterstitialVideo) : "z",
                    this,
                    AdPlacement.Main);
            getAdsCollection().add(mainInterstitial);
        }
        if (null == mainRewarded) {
            mainRewarded = new AdMobRewarded(BuildConfig.DEBUG ? getTestAdForAdType(AdType.Rewarded) : "a",
                    this,
                    AdPlacement.Main);
            getAdsCollection().add(mainRewarded);
        }
        if (null == appOpenAd) {
            appOpenAd = new AdMobAppOpen(BuildConfig.DEBUG ? getTestAdForAdType(AdType.AppOpen) : "a",
                    this,
                    AdPlacement.Main);
            getAdsCollection().add(appOpenAd);
        }
    }

    /**
     * @return The internal list of managed ad wrappers.
     */
    public ArrayList<AdMobAd> getAdsCollection() {
        if (null == adsCollection) {
            adsCollection = new ArrayList<>();
        }
        return adsCollection;
    }

    /**
     * Initiates load requests for all ad units, subject to usage thresholds for banners.
     */
    public void loadAds() {
        if (null != mainBanner && !mainBanner.isLoaded()) {
            float totalUsageMinutes = com.robinzon.medicationwizard.utils.Statisticator.getTotalUsageMinutes(activity);
            int minimumMinutesForBanner = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getMinAppTimeForBannerMins();
            if (totalUsageMinutes >= (float) minimumMinutesForBanner) {
                mainBanner.load();
            } else {
                com.robinzon.medicationwizard.utils.Logger.log("AdsManager", "Banner load skipped. Usage mins: " + totalUsageMinutes + " < Min: " + minimumMinutesForBanner);
            }
        }
        if (null != mainInterstitial && !mainInterstitial.isLoaded()) {
            mainInterstitial.load();
        }
        if (null != mainRewarded && !mainRewarded.isLoaded()) {
            mainRewarded.load();
        }

        if (null != appOpenAd && !appOpenAd.isLoaded()) {
            appOpenAd.load();
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
        loadAds(); // Check if we should load banners or reload failed ads
        for (AdMobAd ad : getAdsCollection()) {
            if (null != ad) {
                ad.onResume();
            }
        }
    }

    public void onDestroy() {
        NetworkMonitor.getInstance(activity).removeListener(this);
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

    public void attachBannerToContainer(android.widget.FrameLayout container) {
        if (mainBanner != null) {
            mainBanner.attachToContainer(container);
        }
    }

    public void restoreBannerToDefault() {
        if (mainBanner != null) {
            mainBanner.resetContainer();
        }
    }

    /** @noinspection unused*/
    /**
     * Triggers a full-screen interstitial ad if both usage and time-based cooldowns are satisfied.
     */
    public void showInterstitialAd() {
        if (com.robinzon.medicationwizard.AppConfig.isPremium(activity) && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE) return;

        if (null != mainInterstitial && hasCoolDownForFullScreenNonUserInitiatedAd()) {
            if (shouldShowInterstitialBasedOnUsage()) {
                mainInterstitial.show();
            }
        }
    }

    /**
     * Shows an interstitial ad bypassing the minimum session/usage barriers,
     * but still strictly respecting the time-based cooldown.
     */
    public void showInterstitialAdWithCooldownOnly() {
        if (com.robinzon.medicationwizard.AppConfig.isPremium(activity) && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE) return;

        if (null != mainInterstitial && hasCoolDownForFullScreenNonUserInitiatedAd()) {
            mainInterstitial.show();
        }
    }

    /**
     * Determines if the user has reached the minimum activity levels required for interstitials.
     * Checks session count and ad-specific usage minutes.
     */
    private boolean shouldShowInterstitialBasedOnUsage() {
        final int sessionCount = com.robinzon.medicationwizard.utils.Statisticator.getSessionCount(activity);
        final float usageMinutesForAds = com.robinzon.medicationwizard.utils.Statisticator.getUsageMinutesForAds(activity);
        
        com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager remoteConfigManager = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance();
        int minimumSessionsThreshold = remoteConfigManager.getMinSessionsForInterstitial();
        int minimumMinutesThreshold = remoteConfigManager.getMinAppTimeForInterstitialMins();

        // Standard Hybrid Trigger: Show if minimum session count OR usage time since last ad is met
        return sessionCount >= minimumSessionsThreshold || usageMinutesForAds >= (float) minimumMinutesThreshold;
    }
    /** @noinspection unused*/
    /**
     * Displays a rewarded video ad to the user.
     *
     * @param listener Callback to receive the result of the reward event.
     */
    public void showRewarded(OnRewardedFinishedListener listener) {
        if (null != mainRewarded) {
            mainRewarded.setRewardedFinishedListener(listener);
            mainRewarded.show();
        } else if (listener != null) {
            listener.onRewarded(false);
        }
    }

    /**
     * @return True if a rewarded video ad is currently loaded and ready to play.
     */
    public boolean isRewardedLoaded() {
        return mainRewarded != null && mainRewarded.isLoaded();
    }

    /**
     * @return True if a rewarded video ad is currently being fetched from the server.
     */
    public boolean isRewardedLoading() {
        return mainRewarded != null && mainRewarded.isLoading();
    }

    /**
     * Attempts to display an App Open ad, checking for usage thresholds and cooldowns.
     */
    public void showAppOpenAd() {
        if (com.robinzon.medicationwizard.AppConfig.isPremium(activity) && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE) return;

        if (null != appOpenAd && hasCoolDownForFullScreenNonUserInitiatedAd()) {
            if (shouldShowAppOpenBasedOnUsage()) {
                appOpenAd.show();
            }
        }
    }

    private boolean shouldShowAppOpenBasedOnUsage() {
        com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager rcm = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance();
        
        if (!rcm.shouldShowAppOpen()) return false;

        final int sessionCount = com.robinzon.medicationwizard.utils.Statisticator.getSessionCount(activity);
        final float totalUsageMinutes = com.robinzon.medicationwizard.utils.Statisticator.getTotalUsageMinutes(activity);
        
        int minSessions = rcm.getMinSessionsAppOpen();
        int minUsageMins = rcm.getMinAppTimeAppOpenMins();

        // Standard Hybrid Trigger: Show if minimum session count OR total usage time is met
        return sessionCount >= minSessions || totalUsageMinutes >= (float) minUsageMins;
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
        this.fullAdDismissedTimeStamp = System.currentTimeMillis();
        // FSA cooldown reset requirement
        com.robinzon.medicationwizard.utils.Statisticator.resetUsageMinutesForAds(activity);
    }

    private void setBannerClickTimeStamp() {
        this.bannerClickTimeStamp = System.currentTimeMillis();
    }

    public long getFullScreenNonUserInitiatedAdDismissTimeStamp() {
        return fullAdDismissedTimeStamp;
    }

    public long getBannerClickTimeStamp() {
        return bannerClickTimeStamp;
    }

    public boolean hasCoolDownForFullScreenNonUserInitiatedAd() {
        final long coolDownMillis = TimeManager.getInstance().toMillisFromSeconds(getCoolDownSecondsForFullScreenNonUserInitiatedAd());
        final long now = System.currentTimeMillis();
        final long lastFullAdDismiss = getFullScreenNonUserInitiatedAdDismissTimeStamp();
        final long lastBannerClick = getBannerClickTimeStamp();
        return (now - lastFullAdDismiss) > coolDownMillis &&
                (now - lastBannerClick) >  coolDownMillis;
    }

    private long getCoolDownSecondsForFullScreenNonUserInitiatedAd() {
        // Use the value defined in Remote Config (Server or Local Cache)
        return com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getAdInterstitialCoolDownSeconds();
    }

    @Override
    public void onNetworkChanged(boolean isAvailable) {
        if (!isAvailable) {
            Logger.log("AdsManager", "Network lost. Cancelling all ad load states.");
            for (AdMobAd ad : getAdsCollection()) {
                if (ad != null) {
                    ad.setIsLoading(false);
                }
            }
        } else {
            Logger.log("AdsManager", "Network restored. Triggering ad loads.");
            loadAds();
        }
    }
}
