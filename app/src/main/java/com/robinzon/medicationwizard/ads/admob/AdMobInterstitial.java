package com.robinzon.medicationwizard.ads.admob;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.robinzon.medicationwizard.ads.AdAction;
import com.robinzon.medicationwizard.ads.AdPlacement;
import com.robinzon.medicationwizard.ads.AdType;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;
import com.robinzon.medicationwizard.utils.NetworkUtils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Handles the logic for requesting, loading, and presenting full-screen Interstitial Video ads.
 */
public class AdMobInterstitial extends AdMobAd {
    private InterstitialAdLoadCallback mAdLoadCallBack;
    private InterstitialAd mInterstitialAd;

    /**
     * Initializes a new wrapper for an AdMob interstitial unit.
     *
     * @param adUnitId The unique identifier for this interstitial unit. Must not be null.
     * @param adsManager The central manager coordinating this ad's lifecycle. Must not be null.
     * @param placement The UI context where this ad is intended to appear. Must not be null.
     */
    public AdMobInterstitial(@NonNull String adUnitId, @NonNull AdsManager adsManager, @NonNull AdPlacement placement) {
        super(adUnitId, adsManager, placement);
        log("%s Creating object.\n%s", getLogTag(), thisToString());
    }

    /**
     * Converts the current state of this ad wrapper to a string for debugging.
     *
     * @return State representation.
     */
    @NonNull
    private String thisToString() {
        return AdMobInterstitial.this.toString();
    }

    /**
     * Identifies the type of ad this class handles.
     *
     * @return Always returns AdType.InterstitialVideo.
     */
    @Override
    public AdType getAdType() {
        return AdType.InterstitialVideo;
    }

    /**
     * Triggers a network request to load an interstitial ad if it's currently allowed by app logic and network state.
     */
    @Override
    public void load() {
        log("%s Requesting load.\n%s", getLogTag(), thisToString());
        if (Boolean.TRUE.equals(shouldBeLoaded())) {
            log("%s Preparing for loading.\n%s", getLogTag(), thisToString());
            setIsLoading(true);
            log("%s Loading.\n%s", getLogTag(), thisToString());
            InterstitialAd.load(getActivity(), getAdUnitId(), getAdRequest(), getAdLoadCallBack());
        } else {
            log("%s Refusing load. Has network %b. \n%s",
                    getLogTag(),
                    NetworkUtils.isNetworkAvailable(getContext().getApplicationContext()),
                    thisToString());
        }
    }

    /**
     * Retrieves or instantiates the callback handler for interstitial ad loading events.
     *
     * @return The configured InterstitialAdLoadCallback instance.
     */
    private InterstitialAdLoadCallback getAdLoadCallBack() {
        if (null == mAdLoadCallBack) {
            mAdLoadCallBack = new InterstitialAdLoadCallback() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    setIsLoaded(false);
                    setIsLoading(false);
                    mInterstitialAd = null;
                    failedToLoad(loadAdError);
                    log("%s Failed to load. Reason is %s.\n%s", getLogTag(), loadAdError.getMessage(), thisToString());
                }

                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    super.onAdLoaded(interstitialAd);
                    setIsLoading(false);
                    setIsLoaded(true);
                    mInterstitialAd = interstitialAd;
                    loaded();
                    log("%s Loaded. Adapter is %s.\n%s", getLogTag(), getLastWord(interstitialAd.getResponseInfo().getMediationAdapterClassName()), thisToString());
                }
            };
        }
        return mAdLoadCallBack;

    }

    /**
     * Checks if the ad is ready and attempts to display it as a full-screen overlay over the parent Activity.
     * Wires up content callbacks to manage user interaction and dismiss events.
     */
    @Override
    public void show() {
        if (canShow() && shouldShow()) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    // Called when a click is recorded for an ad.
                    log("%s Clicked.\n%s", getLogTag(), thisToString());
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    setIsShowing(false);
                    setIsLoaded(false);
                    setIsLoading(false);
                    mInterstitialAd = null;

                    getAdsManager().onAdAction(AdMobInterstitial.this, AdAction.Dismissed);

                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            getActivity().runOnUiThread(AdMobInterstitial.this::load);
                        }
                    }, 500L);

                    log("%s Dismissed.\n%s", getLogTag(), thisToString());
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    // Called when ad fails to show.
                    mInterstitialAd = null;
                    setIsShowing(false);
                    setIsLoaded(false);
                    setIsLoading(false);
                    log("%s Failed to show. Reason is %s.\n%s", getLogTag(), adError.getMessage(), thisToString());
                }

                @Override
                public void onAdImpression() {
                    // Called when an impression is recorded for an ad.
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    setIsShowing(true);
                    log("%s Showed.\n%s", getLogTag(), thisToString());
                }
            });
            mInterstitialAd.show(getActivity());
        }
    }

    /**
     * Identifies whether the ad has expired while waiting to be shown.
     *
     * @return Always returns false currently.
     */
    @Override
    public boolean isExpired() {
        return false;
    }

    /**
     * Evaluates if this interstitial is allowed to interrupt the user right now based on premium status and cooldown settings.
     *
     * @return True if allowed to show, otherwise false.
     */
    @Override
    public boolean shouldShow() {
        if (com.robinzon.medicationwizard.AppConfig.isPremium(getActivity()) && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE) {
            return false;
        }
        return getAdsManager().hasCoolDownForFullScreenNonUserInitiatedAd();
    }

    /**
     * Placeholder method to hide the ad view.
     */
    @Override
    public void hide() {

    }

    /**
     * Called when the parent activity pauses. (No specific logic needed for interstitials).
     */
    @Override
    public void onPause() {

    }

    /**
     * Called when the parent activity resumes. (No specific logic needed for interstitials).
     */
    @Override
    public void onResume() {

    }

    /**
     * Creates a new AdRequest bundle for fetching the interstitial content.
     *
     * @return An AdRequest instance.
     */
    @Override
    public AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    /**
     * Provides the underlying Google InterstitialAd instance.
     *
     * @return The core InterstitialAd.
     */
    @Override
    public Object getCoreAdObject() {
        return mInterstitialAd;
    }

    /**
     * Handles teardown and cleanup logic when the parent is destroyed.
     */
    @Override
    public void onDestroy() {

    }
}