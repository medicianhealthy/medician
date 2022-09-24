package com.robinzon.medicationwizard.ads.rootclasses;

import android.app.Activity;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.EAdType;
import com.robinzon.medicationwizard.ads.interfaces.IAdInterface;

import java.lang.ref.WeakReference;

public abstract class Ad extends MedicationWizardSuper implements IAdInterface {

    private final Activity mActivity;
    private WeakReference<String> mAdUnitId = new WeakReference<String>(null);
    private boolean mIsLoaded;
    private boolean mIsInLoadingProgress;
    private boolean mIsShowing;
    private int mRetryAttempts;
    private long mLastSuccessfulLoadTimeStamp;
    private int mExpirationTimeInMinutes;

    protected Ad(Activity mActivity) {
        this.mActivity = mActivity;
    }

    @Override
    public String getAdUnitId() {
        return mAdUnitId.get();
    }

    @Override
    public void setIsInLoadingProgress(boolean isLoading) {
        mIsInLoadingProgress = isLoading;
    }

    @Override
    public boolean isInLoadingProgress() {
        return mIsInLoadingProgress;
    }

    @Override
    public boolean isLoaded() {
        return mIsLoaded;
    }

    @Override
    public void setIsLoaded(boolean isLoaded) {
        mIsLoaded = isLoaded;
    }

    @Override
    public boolean canShow() {
        return !isShowing() && isLoaded() && !isInLoadingProgress();
    }

    @Override
    public boolean isShowing() {
        return mIsShowing;
    }

    @Override
    public void setIsShowing(boolean isShowing) {
        mIsShowing = isShowing;
    }

    @Override
    public boolean shouldLoad() {
        return !isLoaded() && !isInLoadingProgress() ||
                isLoaded() && isExpired();
    }

    @Override
    public void handleAdCallBacks(EAdCallBacks adCallback) {
        switch (adCallback) {
            case STARTING_TO_LOAD:
                setIsInLoadingProgress(true);
                break;
            case LOADED:
                setIsLoaded(true);
                setIsInLoadingProgress(false);
                break;
            case FAILED_TO_LOAD:
                setIsLoaded(false);
                setIsInLoadingProgress(false);
                handleReloaderOnFaild();
                break;
            case SHOWN:
                setIsShowing(true);
                if (EAdType.BANNER != getAdType()) {
                    setIsLoaded(false);
                    load();
                }
                break;
            case FAILED_TO_SHOW:
                break;
            case DISMISSED:
                setIsShowing(false);
                break;
            case REWARDED:
                break;
            case CLICKED:
                break;
        }
    }

    @Override
    public void handleReloaderOnFaild() {

    }

    @Override
    public void handleReloaderOnSuccess() {
    }

    @Override
    public boolean isExpired() {
        final int expirationInMinutes = getExpirationTimeInMinutes();
        final long currentTimeInMillis = System.currentTimeMillis();
        final int deltaInMinutes = (int) ((currentTimeInMillis - mLastSuccessfulLoadTimeStamp) / 1000 / 60);
        return deltaInMinutes > expirationInMinutes;
    }

    @Override
    public int getExpirationTimeInMinutes() {
        return this.mExpirationTimeInMinutes;
    }

    @Override
    public Activity getActivity() {
        return this.mActivity;
    }

    @Override
    public AdsManager getAdsManager() {
        return ((MainActivity) getActivity()).getAdsManager();
    }
}
