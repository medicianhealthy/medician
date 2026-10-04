package com.robinzon.medicationwizard.ads.rootclasses;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.robinzon.medicationwizard.ads.AdPlacement;
import com.robinzon.medicationwizard.ads.AdType;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.NetworkUtils;
import com.robinzon.medicationwizard.utils.TimeManager;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Serves as the abstract base class for all AdMob ad wrappers in the application.
 * It encapsulates common behaviors such as state tracking (loading, loaded, showing), 
 * retry logic upon failures, and expiration rules to ensure ads remain fresh.
 */
public abstract class AdMobAd {
    private final String mAdUnitId;
    private final AdsManager mAdsManager;
    private final AdPlacement mPlacement;
    private boolean mIsLoading;
    private boolean mIsLoaded;
    private boolean mIsShowing;
    private int mLoadRetryAttempts;

    private Timer mReloadTimer;
    private long mLastLoadTime;

    /**
     * Constructs a base AdMob ad instance, storing core identity and management context.
     *
     * @param adUnitId The string identifier for the ad unit. Must not be null.
     * @param adsManager The AdsManager coordinating this object. Must not be null.
     * @param placement The intended placement of this ad in the UI. Must not be null.
     */
    public AdMobAd(final @NonNull String adUnitId,
                   final @NonNull AdsManager adsManager,
                   final @NonNull AdPlacement placement) {
        this.mAdUnitId = adUnitId;
        this.mAdsManager = adsManager;
        this.mPlacement = placement;
    }

    /**
     * Retrieves the host Activity for UI operations and context binding.
     *
     * @return The Activity linked to the AdsManager.
     */
    @NonNull
    public Activity getActivity() {
        return getAdsManager().getActivity();
    }

    /**
     * Retrieves a standard context derived from the parent Activity.
     *
     * @return A Context object suitable for non-UI SDK operations.
     */
    @NonNull
    public Context getContext() {
        return getActivity();
    }

    /**
     * Retrieves the AdMob Unit ID associated with this wrapper.
     *
     * @return The ad unit ID string.
     */
    @NonNull
    public String getAdUnitId() {
        return mAdUnitId;
    }

    /**
     * Retrieves the manager responsible for coordinating ad lifecycles.
     *
     * @return The AdsManager reference.
     */
    @NonNull
    public AdsManager getAdsManager() {
        return mAdsManager;
    }

    /**
     * Gets the intended placement enum for this ad to help with analytics or UI logic.
     *
     * @return The AdPlacement enum value.
     * @noinspection unused
     */
    @NonNull
    public AdPlacement getPlacement() {
        return mPlacement;
    }

    /**
     * Indicates whether the ad is currently making a network request to load content.
     *
     * @return True if loading, otherwise false.
     * @noinspection BooleanMethodIsAlwaysInverted
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Sets the loading state flag for this ad.
     *
     * @param isLoading True to mark as loading, false when finished.
     */
    public void setIsLoading(final boolean isLoading) {
        this.mIsLoading = isLoading;
    }

    /**
     * Indicates whether the ad has successfully downloaded content and is ready to show.
     *
     * @return True if a valid ad is cached, otherwise false.
     */
    public boolean isLoaded() {
        return mIsLoaded;
    }

    /**
     * Updates the internal readiness state of this ad.
     *
     * @param isLoaded True to mark as ready, false if invalidated or shown.
     */
    public void setIsLoaded(final boolean isLoaded) {
        this.mIsLoaded = isLoaded;
    }

    /**
     * Indicates whether this ad is actively rendering on the screen.
     *
     * @return True if the user is currently viewing the ad, otherwise false.
     */
    public boolean isShowing() {
        return mIsShowing;
    }

    /**
     * Updates the internal visibility state.
     *
     * @param isShowing True when presentation begins, false when dismissed.
     */
    public void setIsShowing(final boolean isShowing) {
        this.mIsShowing = isShowing;
    }

    /**
     * Returns the specific enum classification of this ad (e.g., Banner, Rewarded).
     *
     * @return The AdType.
     */
    public abstract AdType getAdType();

    /**
     * Triggers the process of requesting ad content from the network.
     * Must be implemented by subclasses using appropriate SDK loader classes.
     */
    public abstract void load();

    /**
     * Evaluates standard business rules (premium status, network availability, and current state) 
     * to determine if a network load request is permissible.
     *
     * @return True if the load is allowed, false if blocked, or null if context is entirely unavailable.
     */
    @Nullable
    protected Boolean shouldBeLoaded() {
        // Allow Rewarded ads to load even if premium (so users can extend Magic Pass)
        // For other types (Banner, Interstitial), block if premium.
        if (getAdType() != AdType.Rewarded && getAdType() != AdType.RewardedInterstitial) {
            if (com.robinzon.medicationwizard.AppConfig.isPremium(getContext()) && !com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE) {
                return false;
            }
        }

        final Context applicationContext = getContext().getApplicationContext();
        if (null != applicationContext) {
            final boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(applicationContext);
            final boolean isLoading = isLoading();
            if (!isExpired()) {
                return !isLoading && !isLoaded() && isNetworkAvailable;
            } else {
                return !isLoading && isNetworkAvailable;
            }
        } else {
            return null;
        }
    }

    /**
     * Attempts to render the ad to the user interface.
     */
    public abstract void show();

    /**
     * Safety check to ensure the ad is fully loaded, not currently displaying, and holds a valid core object.
     *
     * @return True if it is safe to invoke the SDK's show method.
     */
    protected boolean canShow() {
        return null != getCoreAdObject() && isLoaded() && !isShowing() && !isLoading();
    }

    /**
     * Determines whether the pre-loaded ad content has expired and should be refreshed.
     * AdMob generally prefers ads to be shown within a specific timeframe (often 1 hour).
     *
     * @return True if the ad was loaded more than 58 minutes ago, otherwise false.
     */
    public boolean isExpired() {
        final long timeFromLastLoadInMillis = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal() - mLastLoadTime;
        final float timeFromLastLoadInMinutes = TimeManager.getInstance().toMinutesFromMillis(timeFromLastLoadInMillis);
        return timeFromLastLoadInMinutes > 58;
    }

    /**
     * Checks subclass-specific business logic to verify if the ad is permitted to display.
     *
     * @return True if the ad should be presented.
     */
    public abstract boolean shouldShow();

    /**
     * Hides the ad view, if applicable.
     * @noinspection unused
     */
    public abstract void hide();

    /**
     * Used to inform the SDK that the parent UI has entered a paused state.
     */
    public abstract void onPause();

    /**
     * Used to inform the SDK that the parent UI has resumed foreground activity.
     */
    public abstract void onResume();

    /**
     * Retrieves the builder object used to request an ad from Google.
     *
     * @return The AdRequest object.
     */
    public abstract AdRequest getAdRequest();

    /**
     * Exposes the underlying Google SDK object specific to this ad format.
     *
     * @return The core ad instance (e.g., AdView, InterstitialAd).
     */
    public abstract Object getCoreAdObject();

    /**
     * Yields a short tag based on the subclass name for clear logging output.
     *
     * @return The simple class name string.
     */
    public String getLogTag() {
        return this.getClass().getSimpleName();
    }

    /**
     * Centralized logging wrapper to ensure standardized debug output if logging is globally enabled.
     *
     * @param message The formatting string to log. Must not be null.
     * @param params The format arguments.
     */
    protected void log(final @NonNull String message, final @NonNull Object... params) {
        if (Logger.IS_LOGGING_ENABLED) {
            Logger.log(getLogTag(), message, params);
        }
    }

    /**
     * Provides a multi-line formatted string dump of the wrapper's current state.
     *
     * @return Formatted state data string.
     */
    @NonNull
    @Override
    public String toString() {
        return "AdUnitId='" + mAdUnitId + '\'' + "\n" +
                "Placement=" + mPlacement + "\n" +
                "IsLoading=" + mIsLoading + "\n" +
                "IsLoaded=" + mIsLoaded + "\n" +
                "IsShowing=" + mIsShowing;
    }

    /**
     * Invoked when an ad is successfully downloaded. Resets retry trackers and stops retry timers.
     */
    protected void loaded() {
        if (null != mReloadTimer) {
            mReloadTimer.cancel();
            mLoadRetryAttempts = 0;
        }
    }

    /**
     * Updates the internal timestamp of the last successful load to calculate expiration later.
     * @noinspection unused
     */
    private void setLastLoadTime() {
        mLastLoadTime = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();
    }

    /**
     * Parses the name of the mediation adapter handling the current fill.
     *
     * @param string The full class path of the adapter.
     * @return A simplified adapter name, stripping out packages and common suffixes.
     */
    protected String getLastWord(@Nullable final String string) {
        if (!TextUtils.isEmpty(string)) {
            String[] parts = string.split("\\.");
            return parts[parts.length - 1].replaceFirst("Adapter$", "");
        }
        return "NA";
    }

    /**
     * Coordinates the exponential backoff retry logic when an ad request fails due to common transient errors.
     *
     * @param loadAdError The error descriptor object from AdMob.
     */
    protected void failedToLoad(final LoadAdError loadAdError) {
        final int loadAdErrorCode = loadAdError.getCode();
        if (AdRequest.ERROR_CODE_NO_FILL == loadAdErrorCode ||
                AdRequest.ERROR_CODE_NETWORK_ERROR == loadAdErrorCode ||
                AdRequest.ERROR_CODE_MEDIATION_NO_FILL == loadAdErrorCode ||
                AdRequest.ERROR_CODE_INTERNAL_ERROR == loadAdErrorCode) {
            mLoadRetryAttempts++;
            if (null == mReloadTimer) {
                mReloadTimer = new Timer();
            }
            final long delay = (long) Math.min(Math.pow(2, 7), Math.pow(2, mLoadRetryAttempts + 2)) * 1000;
            mReloadTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    getActivity().runOnUiThread(AdMobAd.this::load);
                }
            }, delay);
        }
    }

    /**
     * Destroys resources, cancels timers, and cleanly tears down the ad.
     */
    public abstract void onDestroy();
}