package com.robinzon.medicationwizard.ads.rootclasses;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.AdsUnitProvider;
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.EAdType;
import com.robinzon.medicationwizard.ads.interfaces.IAd;
import com.robinzon.medicationwizard.ads.interfaces.IAdsLifeCycleCallBack;
import com.robinzon.medicationwizard.utils.TimeInterval;

import java.lang.ref.WeakReference;

public abstract class Ad extends MedicationWizardSuper implements IAd {

    private final Activity mActivity;
    private WeakReference<String> mAdUnitId = new WeakReference<>(null);
    private boolean mIsLoaded;
    private boolean mIsInLoadingProgress;
    private boolean mIsShowing;
    private final EAdPlacement mPlacement;
    private AdReloadWorker mAdReloadWorker;
    private byte mRetryAttempts;


    public Ad(Activity mActivity, EAdPlacement placement) {
        this.mActivity = mActivity;
        this.mPlacement = placement;
    }

    @Override
    public String getAdUnitId() {
        if (null == mAdUnitId.get()) {
            final String adUnit = AdsUnitProvider.getAdUnit(getActivity(), getAdType(), getPlacement());
            if (!TextUtils.isEmpty(adUnit)) {
                mAdUnitId = new WeakReference<>(adUnit);
            }
        }
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
        return !isShowing() && isLoaded();
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
        return !isLoaded() && !isInLoadingProgress();
    }

    @Override
    public void handleAdCallBacks(EAdCallBacks adCallback, IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        handleAdCallBacks(adCallback, adsLifeCycleCallBack, null);
    }

    @Override
    public void handleAdCallBacks(EAdCallBacks adCallback, IAdsLifeCycleCallBack adsLifeCycleCallBack, final AdError adError) {
        logAdMessageOnAdCallBack(adCallback, adError);
        switch (adCallback) {
            case STARTING_TO_LOAD:
                setIsInLoadingProgress(true);
                break;
            case LOADED:
                setIsLoaded(true);
                setIsInLoadingProgress(false);
                mRetryAttempts = 0;
                if (null != mAdReloadWorker) {
                    mAdReloadWorker.removeMessages(AdReloadWorker.MESSAGE_RELOAD);
                    mAdReloadWorker = null;
                }
                break;
            case FAILED_TO_LOAD:
                setIsLoaded(false);
                setIsInLoadingProgress(false);
                activateReloadOnFailedLoad();
                break;
            case SHOWN:
                setIsShowing(true);
                break;
            case FAILED_TO_SHOW:
            case DISMISSED:
                setIsShowing(false);
                if (EAdType.BANNER != getAdType()) {
                    setIsLoaded(false);
                    load();
                }
                break;
            case CLICKED:
                break;
            default:
                break;
        }

        final boolean hasAnAdsLifeCycleCallBack = (null != adsLifeCycleCallBack);
        if (hasAnAdsLifeCycleCallBack) {
            adsLifeCycleCallBack.onInterstitialLifeCycleStageChanged(this, adCallback, adError);
        }
    }

    private void logAdMessageOnAdCallBack(EAdCallBacks adCallback, AdError adError) {
        if (null != adError && !TextUtils.isEmpty(adError.getMessage())) {
            logMessage("Handling ad call back {%s}. Error is {%s}", adCallback.name(), adError.getMessage());
        } else {
            logMessage("Handling ad call back {%s}", adCallback.name());
        }
    }

    private void activateReloadOnFailedLoad() {
        mRetryAttempts++;
        if (null == mAdReloadWorker) {
            mAdReloadWorker = new AdReloadWorker(Looper.myLooper(), this);
        }
        final byte power = (byte) Math.min(mRetryAttempts , 6);
        final short deltaInSecondsToNextLoadAttempt = (short) Math.pow(2, power);
        final long deltaInMillisToNextLoadAttempt = TimeInterval.MilliSeconds.getFromSeconds(deltaInSecondsToNextLoadAttempt);
        mAdReloadWorker.sendEmptyMessageDelayed(AdReloadWorker.MESSAGE_RELOAD,deltaInMillisToNextLoadAttempt);
        logMessage("Scheduling a new reload attempt in [%d] seconds", deltaInSecondsToNextLoadAttempt);
    }


    @Override
    public Activity getActivity() {
        return this.mActivity;
    }

    @Override
    public AdsManager getAdsManager() {
        return ((MainActivity) getActivity()).getAdsManager();
    }

    @Override
    public EAdPlacement getPlacement() {
        return mPlacement;
    }

    public static final class AdReloadWorker extends Handler {
        private final WeakReference<IAd> mAd;
        public static final int MESSAGE_RELOAD = 1;

        public AdReloadWorker(@NonNull Looper looper, IAd ad) {
            super(looper);
            mAd = new WeakReference<>(ad);
        }

        @Override
        public void handleMessage(@NonNull Message message) {
            super.handleMessage(message);
            if (MESSAGE_RELOAD == message.what) {
                final IAd ad = mAd.get();
                if (null != ad) {
                    ad.load();
                }
            }
        }
    }
}
