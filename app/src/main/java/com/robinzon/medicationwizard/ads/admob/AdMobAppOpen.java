package com.robinzon.medicationwizard.ads.admob;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.robinzon.medicationwizard.ads.AdAction;
import com.robinzon.medicationwizard.ads.AdPlacement;
import com.robinzon.medicationwizard.ads.AdType;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;
import com.robinzon.medicationwizard.utils.NetworkUtils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages the loading and display of App Open ads.
 * App Open ads are displayed when the user brings the app to the foreground, providing an immediate monetization opportunity.
 */
public class AdMobAppOpen extends AdMobAd {
    private AppOpenAd mAppOpenAd;

    /**
     * Constructs a new App Open ad instance.
     *
     * @param adUnitId The unique identifier for this ad unit. Must not be null.
     * @param adsManager The central manager coordinating this ad's lifecycle. Must not be null.
     * @param placement The UI context where this ad is intended to appear. Must not be null.
     */
    public AdMobAppOpen(@NonNull String adUnitId, @NonNull AdsManager adsManager, @NonNull AdPlacement placement) {
        super(adUnitId, adsManager, placement);
    }

    /**
     * Retrieves the specific type of this ad unit.
     *
     * @return Always returns AdType.AppOpen.
     */
    @Override
    public AdType getAdType() {
        return AdType.AppOpen;
    }

    /**
     * Requests an ad payload from the AdMob servers if the network is available and business rules permit.
     * Handles async loading callbacks to update the AdsManager on success or failure.
     */
    @Override
    public void load() {
        log("%s Requesting load.\n%s", getLogTag(), thisToString());
        if (Boolean.TRUE.equals(shouldBeLoaded())) {
            log("%s Preparing for loading.\n%s", getLogTag(), thisToString());
            getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.StartingToLoad);
            setIsLoading(true);
            AppOpenAd.load(getContext().getApplicationContext(),
                    getAdUnitId(),
                    getAdRequest(),
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            super.onAdFailedToLoad(loadAdError);
                            setIsLoading(false);
                            setIsLoaded(false);
                            mAppOpenAd = null;
                            failedToLoad(loadAdError);
                            log("%s Failed to load. Reason is %s.\n%s", getLogTag(), loadAdError.getMessage(), thisToString());
                            getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.FailedToLoad);
                        }

                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                            super.onAdLoaded(appOpenAd);
                            setIsLoading(false);
                            setIsLoaded(true);
                            mAppOpenAd = appOpenAd;
                            loaded();
                            log("%s Loaded. Adapter is %s.\n%s", getLogTag(), getLastWord(appOpenAd.getResponseInfo().getMediationAdapterClassName()), thisToString());
                            getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.LoadedSuccessfully);
                        }

                    });
        } else {
            log("%s Refusing load. Has network %b. \n%s",
                    getLogTag(),
                    NetworkUtils.isNetworkAvailable(getContext().getApplicationContext()),
                    thisToString());

        }
    }

    /**
     * Renders the App Open ad to the user in a full-screen view if it is ready and permitted by display rules.
     * Assigns callbacks to handle user interactions and cleanup when the ad is closed.
     */
    @Override
    public void show() {
        if (shouldShow() && canShow()) {
            setIsShowing(true);
            mAppOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    log("%s Clicked.\n%s", getLogTag(), thisToString());
                    getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.Clicked);
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    mAppOpenAd = null;
                    setIsShowing(false);
                    setIsLoaded(false);
                    setIsLoading(false);

                    getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.Dismissed);

                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            getActivity().runOnUiThread(AdMobAppOpen.this::load);
                        }
                    }, 500L);

                    log("%s Dismissed.\n%s", getLogTag(), thisToString());
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    mAppOpenAd = null;
                    setIsShowing(false);
                    setIsLoaded(false);
                    setIsLoading(false);
                    log("%s Failed to show. Reason is %s.\n%s", getLogTag(), adError.getMessage(), thisToString());
                    getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.FailedToShow);
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.Impression);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();
                    setIsShowing(true);
                    setIsLoaded(false);
                    setIsLoading(false);
                    log("%s Showed.\n%s", getLogTag(), thisToString());
                    getAdsManager().onAdAction(AdMobAppOpen.this, AdAction.Showing);
                }
            });
            mAppOpenAd.show(getActivity());
        }
    }

    /**
     * Indicates whether the currently cached ad has passed its expiration window.
     *
     * @return False as App Open Ads manage their own implicit expiration logic in most configurations, or if expiration logic isn't yet enforced.
     */
    @Override
    public boolean isExpired() {
        return false;
    }

    /**
     * Helper string to log detailed state for debugging.
     *
     * @return A formatted string detailing the current state of this ad wrapper.
     */
    @NonNull
    private String thisToString() {
        return AdMobAppOpen.this.toString();
    }

    /**
     * Determines if the ad is permitted to be shown right now, incorporating premium restrictions and cooldowns.
     *
     * @return True if the ad should be shown; false if it must be blocked.
     */
    @Override
    public boolean shouldShow() {
        if (com.robinzon.medicationwizard.AppConfig.isPremium(getActivity())
                && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE) {
            return false;
        }
        return getAdsManager().hasCoolDownForFullScreenNonUserInitiatedAd();
    }

    /**
     * Hides the ad, if applicable. (Not typically used for App Open ads).
     */
    @Override
    public void hide() {

    }

    /**
     * Called when the parent activity is paused. (No action needed for App Open ads).
     */
    @Override
    public void onPause() {

    }

    /**
     * Called when the parent activity is resumed. (No action needed for App Open ads).
     */
    @Override
    public void onResume() {

    }

    /**
     * Builds and returns a fresh AdRequest payload.
     *
     * @return A constructed AdRequest for network fetching.
     */
    @Override
    public AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    /**
     * Retrieves the underlying SDK-specific ad object.
     *
     * @return The raw AppOpenAd instance.
     */
    @Override
    public Object getCoreAdObject() {
        return mAppOpenAd;
    }

    /**
     * Cleans up resources when the ad instance is destroyed.
     */
    @Override
    public void onDestroy() {

    }
}