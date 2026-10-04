package com.robinzon.medicationwizard.ads;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;
import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.R;
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

/**
 * Manages the initialization, loading, and display of all advertisement units.
 * This class orchestrates the lifecycle of various ad formats, enforcing business rules 
 * such as premium status checks, session minimums, and display cooldowns.
 */
public class AdsManager implements OnAdActionListener, NetworkMonitor.NetworkStatusListener {


    private final Activity activity;
    private final CopyOnWriteArrayList<Runnable> adAvailabilityListeners = new CopyOnWriteArrayList<>();
    private AdMobBanner mainBanner;
    private AdMobInterstitial mainInterstitial;
    private AdMobRewarded mainRewarded;
    private AdMobAppOpen appOpenAd;
    private ArrayList<AdMobAd> adsCollection;
    private long fullAdDismissedTimeStamp;
    private long bannerClickTimeStamp;
    
    /**
     * Instantiates the AdsManager with the given Activity context.
     *
     * @param activity The Activity context used for loading and displaying ads. Must not be null.
     */
    public AdsManager(final @NonNull Activity activity) {
        this.activity = activity;
    }

    /**
     * Registers a listener to be notified when the availability of ads changes.
     *
     * @param listener The callback to invoke on availability changes.
     */
    public void addAdAvailabilityListener(Runnable listener) {
        adAvailabilityListeners.add(listener);
    }

    /**
     * Unregisters a previously added ad availability listener.
     *
     * @param listener The callback to remove.
     */
    public void removeAdAvailabilityListener(Runnable listener) {
        adAvailabilityListeners.remove(listener);
    }

    /**
     * Notifies all registered listeners on the UI thread that ad availability has changed.
     */
    private void notifyAvailabilityChanged() {
        activity.runOnUiThread(() -> {
            for (Runnable listener : adAvailabilityListeners) {
                listener.run();
            }
        });
    }

    /**
     * Retrieves the current Activity context attached to this manager.
     *
     * @return The currently active Activity context.
     */
    public Activity getActivity() {
        return activity;
    }

    /**
     * Performs one-time setup of the Mobile Ads SDK, ad units, and initiates the first load requests.
     */
    public void initializeAds() {
        MobileAds.initialize(activity, initializationStatus ->
                Logger.log("Ads", "MobileAds initialized."));
        NetworkMonitor.getInstance(activity).addListener(this);
        createAds();
        loadAds();
    }

    /**
     * Instantiates the AdMob wrapper classes for each specific ad placement and adds them to the collection.
     */
    private void createAds() {
        if (null == mainBanner) {
            mainBanner = new AdMobBanner(BuildConfig.DEBUG ? getTestAdForAdType(AdType.Banner) : activity.getString(R.string.admob_banner_id_live),
                    this,
                    AdPlacement.Main);
            getAdsCollection().add(mainBanner);

        }
        if (null == mainInterstitial) {
            mainInterstitial = new AdMobInterstitial(BuildConfig.DEBUG ? getTestAdForAdType(AdType.InterstitialVideo) : activity.getString(R.string.admob_interstitial_id_live),
                    this,
                    AdPlacement.Main);
            getAdsCollection().add(mainInterstitial);
        }
        if (null == mainRewarded) {
            mainRewarded = new AdMobRewarded(BuildConfig.DEBUG ? getTestAdForAdType(AdType.Rewarded) : activity.getString(R.string.admob_rv_id_live),
                    this,
                    AdPlacement.Main);
            getAdsCollection().add(mainRewarded);
        }
        if (null == appOpenAd) {
            appOpenAd = new AdMobAppOpen(BuildConfig.DEBUG ? getTestAdForAdType(AdType.AppOpen) : activity.getString(R.string.admob_app_open_id_live),
                    this,
                    AdPlacement.Main);
            getAdsCollection().add(appOpenAd);
        }
    }

    /**
     * Retrieves the internal collection of managed ad wrappers.
     *
     * @return The list of currently managed AdMobAd instances.
     */
    public ArrayList<AdMobAd> getAdsCollection() {
        if (null == adsCollection) {
            adsCollection = new ArrayList<>();
        }
        return adsCollection;
    }

    /**
     * Initiates load requests for all ad units, checking premium status and usage thresholds first.
     * Premium users (without forced ads) bypass ad loading entirely.
     */
    public void loadAds() {
        if (com.robinzon.medicationwizard.AppConfig.isPremium(activity) && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE) {
            if (mainBanner != null) mainBanner.resetContainer();
            return;
        }

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
     * Returns the appropriate Google-provided test ad unit ID for the given AdType.
     *
     * @param adType The type of ad requested. Must not be null.
     * @return A valid test ad unit ID string.
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

    /**
     * Called when the parent Activity resumes. Triggers a reload of ads and propagates the event.
     */
    public void onResume() {
        loadAds(); // Check if we should load banners or reload failed ads
        for (AdMobAd ad : getAdsCollection()) {
            if (null != ad) {
                ad.onResume();
            }
        }
    }

    /**
     * Called when the parent Activity is destroyed. Cleans up network listeners and propagates the event.
     */
    public void onDestroy() {
        NetworkMonitor.getInstance(activity).removeListener(this);
        for (AdMobAd ad : getAdsCollection()) {
            if (null != ad) {
                ad.onDestroy();
            }
        }
    }

    /**
     * Called when the parent Activity pauses. Propagates the pause event to all managed ad units.
     */
    public void onPause() {
        for (AdMobAd ad : getAdsCollection()) {
            if (null != ad) {
                ad.onPause();
            }
        }
    }

    /**
     * Attaches the main banner ad to a specified FrameLayout container in the UI.
     *
     * @param container The FrameLayout where the banner should be injected.
     */
    public void attachBannerToContainer(android.widget.FrameLayout container) {
        if (mainBanner != null) {
            mainBanner.attachToContainer(container);
        }
    }

    /**
     * Detaches the main banner ad from its current container and resets its visual state.
     */
    public void restoreBannerToDefault() {
        if (mainBanner != null) {
            mainBanner.resetContainer();
        }
    }

    /**
     * Triggers a full-screen interstitial ad display if both usage requirements and time-based cooldowns are satisfied.
     * Ignored for premium users unless forced ads are active.
     */
    public void showInterstitialAd() {
        if (com.robinzon.medicationwizard.AppConfig.isPremium(activity) && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE)
            return;

        if (null != mainInterstitial && hasCoolDownForFullScreenNonUserInitiatedAd()) {
            if (shouldShowInterstitialBasedOnUsage()) {
                mainInterstitial.show();
            }
        }
    }

    /**
     * Shows an interstitial ad bypassing the minimum session/usage barriers,
     * but strictly respecting the time-based cooldown logic.
     */
    public void showInterstitialAdWithCooldownOnly() {
        if (com.robinzon.medicationwizard.AppConfig.isPremium(activity) && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE)
            return;

        if (null != mainInterstitial && hasCoolDownForFullScreenNonUserInitiatedAd()) {
            mainInterstitial.show();
        }
    }

    /**
     * Determines if the user has reached the minimum activity levels required to see interstitial ads.
     * Checks the session count and total app usage minutes against remote configuration thresholds.
     *
     * @return True if the usage thresholds are met, false otherwise.
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

    /**
     * Displays a rewarded video ad to the user if one is loaded and ready.
     * Fallbacks to firing NOT_READY if unavailable.
     *
     * @param listener Callback to receive the completion status of the rewarded ad event.
     */
    public void showRewarded(OnRewardedFinishedListener listener) {
        if (null != mainRewarded && mainRewarded.isLoaded()) {
            mainRewarded.setRewardedFinishedListener(listener);
            mainRewarded.show();
        } else if (listener != null) {
            listener.onRewarded(RewardedStatus.NOT_READY);
        }
    }

    /**
     * Checks if a rewarded video ad is successfully loaded and ready for immediate display.
     *
     * @return True if ready, false otherwise.
     */
    public boolean isRewardedLoaded() {
        return mainRewarded != null && mainRewarded.isLoaded();
    }

    /**
     * Checks if a rewarded video ad is currently in the process of being fetched from the server.
     *
     * @return True if fetching, false otherwise.
     */
    public boolean isRewardedLoading() {
        return mainRewarded != null && mainRewarded.isLoading();
    }

    /**
     * Attempts to display an App Open ad, subject to usage thresholds and display cooldowns.
     * Ignored for premium users.
     */
    public void showAppOpenAd() {
        if (com.robinzon.medicationwizard.AppConfig.isPremium(activity) && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE)
            return;

        if (null != appOpenAd && hasCoolDownForFullScreenNonUserInitiatedAd()) {
            if (shouldShowAppOpenBasedOnUsage()) {
                appOpenAd.show();
            }
        }
    }

    /**
     * Determines whether an App Open ad should be displayed based on global configuration and user activity levels.
     *
     * @return True if the App Open ad usage criteria are met, false otherwise.
     */
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

    /**
     * Centralized callback processor for all ad lifecycle actions. Updates internal availability metrics
     * and cooldown timestamps based on the ad's behavior.
     *
     * @param adMobAd The ad object that triggered the action. Must not be null.
     * @param adAction The specific action that occurred.
     */
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
                if (AdAction.Dismissed == adAction || AdAction.FailedToShow == adAction) {
                    setFullScreenNonUserInitiatedAdDismissTimeStamp();
                }
            }
            case AdaptiveBanner, Banner -> {
                if (AdAction.Clicked == adAction) {
                    setBannerClickTimeStamp();
                }
                if (AdAction.Created == adAction) {
                    ((OnAdActionListener) getActivity()).onAdAction(adMobAd, AdAction.Created);
                }
            }
            default -> {
            }
        }
    }

    /**
     * Records the exact time a full-screen non-user initiated ad was dismissed, for cooldown calculation.
     * Also resets the internal usage timer to delay the next automated ad popup.
     */
    private void setFullScreenNonUserInitiatedAdDismissTimeStamp() {
        this.fullAdDismissedTimeStamp = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();
        // FSA cooldown reset requirement
        com.robinzon.medicationwizard.utils.Statisticator.resetUsageMinutesForAds(activity);
    }

    /**
     * Records the exact time a banner ad was clicked, applying a cooldown to full-screen ads to avoid spamming the user.
     */
    private void setBannerClickTimeStamp() {
        this.bannerClickTimeStamp = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();
    }

    /**
     * Retrieves the timestamp of the last dismissed full-screen non-user initiated ad.
     *
     * @return Time in milliseconds.
     */
    public long getFullScreenNonUserInitiatedAdDismissTimeStamp() {
        return fullAdDismissedTimeStamp;
    }

    /**
     * Retrieves the timestamp of the last clicked banner ad.
     *
     * @return Time in milliseconds.
     */
    public long getBannerClickTimeStamp() {
        return bannerClickTimeStamp;
    }

    /**
     * Evaluates if sufficient time has elapsed since the last aggressive ad interaction
     * to safely show another full-screen, non-user-initiated ad.
     *
     * @return True if the cooldown period has expired, false if the ad should be suppressed.
     */
    public boolean hasCoolDownForFullScreenNonUserInitiatedAd() {
        final long coolDownMillis = TimeManager.getInstance().toMillisFromSeconds(getCoolDownSecondsForFullScreenNonUserInitiatedAd());
        final long now = TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();
        final long lastFullAdDismiss = getFullScreenNonUserInitiatedAdDismissTimeStamp();
        final long lastBannerClick = getBannerClickTimeStamp();
        return (now - lastFullAdDismiss) > coolDownMillis &&
                (now - lastBannerClick) > coolDownMillis;
    }

    /**
     * Retrieves the remote-configured duration required between full-screen ad presentations.
     *
     * @return Cooldown time in seconds.
     */
    private long getCoolDownSecondsForFullScreenNonUserInitiatedAd() {
        // Use the value defined in Remote Config (Server or Local Cache)
        return com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getAdInterstitialCoolDownSeconds();
    }

    /**
     * Handles changes in network connectivity. Cancels loaders when offline and aggressively requests reloads when back online.
     *
     * @param isAvailable Boolean indicating if an active internet connection is present.
     */
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

    /**
     * Represents the outcomes of a rewarded video ad display.
     */
    public enum RewardedStatus {
        /** Indicates the user fully watched the ad and earned the reward. */
        SUCCESS,
        /** Indicates the user closed the ad prematurely and receives no reward. */
        DISMISSED_EARLY,
        /** Indicates the ad was not loaded or could not be played. */
        NOT_READY
    }

    /**
     * Callback interface to listen for the termination and reward status of a Rewarded Ad.
     */
    public interface OnRewardedFinishedListener {
        /**
         * Called when the rewarded ad flow is complete, regardless of the outcome.
         *
         * @param status The completion status enum indicating SUCCESS, DISMISSED_EARLY, or NOT_READY.
         */
        void onRewarded(RewardedStatus status);
    }
}