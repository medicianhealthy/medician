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


    public AdMobAd(final @NonNull String adUnitId,
                   final @NonNull AdsManager adsManager,
                   final @NonNull AdPlacement placement) {
        this.mAdUnitId = adUnitId;
        this.mAdsManager = adsManager;
        this.mPlacement = placement;
    }

    @NonNull
    public Activity getActivity() {
        return getAdsManager().getActivity();
    }

    @NonNull
    public Context getContext() {
        return getActivity();
    }

    @NonNull
    public String getAdUnitId() {
        return mAdUnitId;
    }

    @NonNull
    public AdsManager getAdsManager() {
        return mAdsManager;
    }

    /**
     * @return The placement of this specific ad
     * @noinspection unused
     */
    @NonNull
    public AdPlacement getPlacement() {
        return mPlacement;
    }

    /**
     * @noinspection BooleanMethodIsAlwaysInverted
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    public void setIsLoading(final boolean isLoading) {
        this.mIsLoading = isLoading;
    }

    public boolean isLoaded() {
        return mIsLoaded;
    }

    public void setIsLoaded(final boolean isLoaded) {
        this.mIsLoaded = isLoaded;
    }

    public boolean isShowing() {
        return mIsShowing;
    }

    public void setIsShowing(final boolean isShowing) {
        this.mIsShowing = isShowing;
    }

    // Getters
    public abstract AdType getAdType();

    //Info


    //Actions
    public abstract void load();

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

    public abstract void show();

    protected boolean canShow() {
        return null != getCoreAdObject() && isLoaded() && !isShowing() && !isLoading();
    }

    public boolean isExpired() {
        final long timeFromLastLoadInMillis = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal() - mLastLoadTime;
        final float timeFromLastLoadInMinutes = TimeManager.getInstance().toMinutesFromMillis(timeFromLastLoadInMillis);
        return timeFromLastLoadInMinutes > 58;
    }

    public abstract boolean shouldShow();

    /**
     * @noinspection unused
     */
    public abstract void hide();

    public abstract void onPause();

    public abstract void onResume();

    public abstract AdRequest getAdRequest();

    public abstract Object getCoreAdObject();

    public String getLogTag() {
        return this.getClass().getSimpleName();
    }

    protected void log(final @NonNull String message, final @NonNull Object... params) {
        if (Logger.IS_LOGGING_ENABLED) {
            Logger.log(getLogTag(), message, params);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "AdUnitId='" + mAdUnitId + '\'' + "\n" +
                "Placement=" + mPlacement + "\n" +
                "IsLoading=" + mIsLoading + "\n" +
                "IsLoaded=" + mIsLoaded + "\n" +
                "IsShowing=" + mIsShowing;
    }

    protected void loaded() {
        if (null != mReloadTimer) {
            mReloadTimer.cancel();
            mLoadRetryAttempts = 0;
        }

    }

    /**
     * @noinspection unused
     */
    private void setLastLoadTime() {
        mLastLoadTime = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();
    }

    protected String getLastWord(@Nullable final String string) {
        if (!TextUtils.isEmpty(string)) {
            String[] parts = string.split("\\.");
            return parts[parts.length - 1].replaceFirst("Adapter$", "");
        }
        return "NA";
    }

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

    public abstract void onDestroy();
}
