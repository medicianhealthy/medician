package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Display;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.IAdDisplayingEvent;
import com.robinzon.medicationwizard.ads.IAdLoadingEvents;
import com.robinzon.medicationwizard.ads.rootclasses.Banner;
import com.robinzon.medicationwizard.utils.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AdMobBanner extends Banner {
    private AdView mBanner;
    private BannerReLoader mBannerReLoader;

    @Override
    public void create(Activity activity, int adUnitIdResourceId) {
        setAdUnitId(activity.getString(adUnitIdResourceId));
        mBanner = new AdView(activity);
        mBanner.setAdSize(getAdSize(activity));
        mBanner.setAdUnitId(getAdUnitId());
    }

    @Override
    public void load(final Activity activity) {
        if (Logger.isLoggingEnabled()) {
            Logger.getInstance().log(getClassName(), getAdMobBannerLogs(), "Calling to load banner");
        }
        if (!mIsInLoadingProgress.get() && !mIsLoaded.get()) {
            mBanner.setAdListener(getAdListener());
            mIsInLoadingProgress.set(true);
            if (null != mBannerReLoader) {
                mBannerReLoader.removeMessages(BannerReLoader.RELOAD);
            }
            if (Logger.isLoggingEnabled()) {
                Logger.getInstance().log(getClassName(), getAdMobBannerLogs(), "Starting to load banner...");
            }
            mBanner.loadAd(getAdRequest());
        } else {
            if (Logger.isLoggingEnabled()) {
                Logger.getInstance().log(getClassName(), getAdMobBannerLogs(), "Banner is in loading progress or already loaded");
            }
        }
    }

    private List<String> getAdMobBannerLogs() {
        return new ArrayList<String>(1) {{
            add(AdsManager.LOG_BANNER);
        }};
    }

    @Override
    public void setLoadingEventsListener(IAdLoadingEvents adLoadingEvents) {
        if (Logger.isLoggingEnabled()) {
            Logger.getInstance().log(getClassName(),
                    getAdMobBannerLogs(),
                    "Setting load event listener to [%s]",
                    null != adLoadingEvents ? "a valid listener" : "null");
        }
        mLoadingEventsListener = adLoadingEvents;
    }

    private AdListener getAdListener() {
        return new AdListener() {
            @Override
            public void onAdClicked() {
                if (Logger.isLoggingEnabled()){
                    Logger.getInstance().log(getClassName(), getAdMobBannerLogs(), "Banner clicked");
                }
                super.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                if (Logger.isLoggingEnabled()){
                    Logger.getInstance().log(getClassName(), getAdMobBannerLogs(), "Banner closed");
                }
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mIsLoaded.set(false);
                mIsInLoadingProgress.set(false);
                if (null != mLoadingEventsListener) {
                    mLoadingEventsListener.onAdFailedToLoad(loadAdError.getMessage());
                }
                markLoadFailAttempt();
                activateReLoader();
                if (Logger.isLoggingEnabled()){
                    Logger.getInstance().log(getClassName(),
                            getAdMobBannerLogs(),
                            "Banner failed to load. Reason - [%s]",
                            loadAdError.getMessage());
                }
                super.onAdFailedToLoad(loadAdError);
            }

            @Override
            public void onAdImpression() {
                if (Logger.isLoggingEnabled()){
                    Logger.getInstance().log(getClassName(), getAdMobBannerLogs(), "Banner impression");
                }
                super.onAdImpression();
            }

            @Override
            public void onAdLoaded() {
                mIsLoaded.set(true);
                mIsInLoadingProgress.set(false);
                if (null != mLoadingEventsListener) {
                    mLoadingEventsListener.onAdLoaded();
                }
                setRetryAttemptsToZero();
                if (Logger.isLoggingEnabled()){
                    Logger.getInstance().log(getClassName(), getAdMobBannerLogs(), "Banner loaded");
                }
                super.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                if (Logger.isLoggingEnabled()){
                    Logger.getInstance().log(getClassName(), getAdMobBannerLogs(), "Banner opened");
                }
                super.onAdOpened();
            }
        };
    }

    private void activateReLoader() {
        if (null == mBannerReLoader) {
            mBannerReLoader = new BannerReLoader(AdMobBanner.this);
        }
        mBannerReLoader.removeMessages(BannerReLoader.RELOAD);
        final double delaySeconds = Math.pow(2, Math.min(6, getRetryAttempts()));
        if (Logger.isLoggingEnabled()){
            Logger.getInstance().log(getClassName(),
                    getAdMobBannerLogs(),
                    "Handler is scheduling an attempt to reload in [%.0d] seconds",
                    delaySeconds);
        }
        mBannerReLoader.sendEmptyMessageDelayed(BannerReLoader.RELOAD, TimeUnit.SECONDS.toMillis((long) delaySeconds));
    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    @Override
    public void show(Activity activity, IAdDisplayingEvent adDisplayingEvent) {

    }

    @Override
    public boolean hasAd() {
        return isLoaded();
    }

    @Override
    public boolean isLoaded() {
        return mIsLoaded.get();
    }

    @Override
    public void setIsLoaded(boolean isLoaded) {

    }

    @Override
    public boolean isShowing() {
        return false;
    }

    @Override
    public void setIsShowing(boolean isShowing) {

    }

    @Override
    public void onResume(Activity activity) {
        if (null != mBanner) {
            mBanner.resume();
        }
    }

    @Override
    public void onPause(Activity activity) {
        if (null != mBanner) {
            mBanner.pause();
        }
    }

    @Override
    public void onDestroy(Activity activity) {
        if (null != mBanner) {
            mBanner.destroy();
        }
        if (null != mBannerReLoader) {
            mBannerReLoader.removeMessages(BannerReLoader.RELOAD);
        }
    }

    @Override
    public void onCreate(Activity activity) {
        //Nothing to do
    }

    @Override
    public void createFromLayout(Activity activity, int viewId) {
        mBanner = activity.findViewById(viewId);
        setAdUnitId(activity.getString(R.string.admob_banner_id_test));
    }

    @Override
    public int getBannerHeightInPixels(Activity activity) {
        return AdSize.BANNER.getHeightInPixels(activity);
    }

    static class BannerReLoader extends Handler {
        public static final int RELOAD = 1;
        private final WeakReference<AdMobBanner> mAdMobBanner;

        BannerReLoader(final AdMobBanner reference) {
            mAdMobBanner = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(final Message msg) {
            if (msg.what == RELOAD) {
                final AdMobBanner adMobBanner = mAdMobBanner.get();
                if (null != adMobBanner) {
                    adMobBanner.load(null);
                }
            }
        }
    }

    private AdSize getAdSize(final Activity activity) {
        // Determine the screen width (less decorations) to use for the ad width.
        final Display display = activity.getWindowManager().getDefaultDisplay();
        final DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        final float widthPixels = outMetrics.widthPixels;
        final float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);

        //Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    @Override
    public String getClassName() {
        return AdMobBanner.class.getSimpleName();
    }
}
