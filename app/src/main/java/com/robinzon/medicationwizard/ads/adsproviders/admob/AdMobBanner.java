package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.IAdDisplayingEvent;
import com.robinzon.medicationwizard.ads.IAdLoadingEvents;
import com.robinzon.medicationwizard.ads.interfaces.IAd;
import com.robinzon.medicationwizard.ads.interfaces.IBannerAd;
import com.robinzon.medicationwizard.ads.rootclasses.ISuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.Validator;

public class AdMobBanner implements ISuper, IBannerAd, IAd {
    private AdView mBanner;
    private String mAdUnit;
    private boolean mIsLoaded = false;
    private boolean mIsShowing;

    @Override
    public void createBannerAd(final Activity activity, final int adUnitIdResourceId) {
        if (Validator.isValidAndroidResourceId(adUnitIdResourceId)) {
            setAdUnitId(activity.getString(adUnitIdResourceId));
            if (Validator.isValidString(getAdUnitId())) {
                mBanner = new AdView(activity);
                mBanner.setAdSize(AdSize.BANNER);
                mBanner.setAdUnitId(getAdUnitId());
                Logger.logSingleTag(getClassName(),
                        AdsManager.LOG_BANNER,
                        "Banner ad created. Ad unit is [%s].", getAdUnitId());
            } else {
                Logger.logSingleTag(getClassName(),
                        AdsManager.LOG_BANNER,
                        "Tried to create banner but ad unit id null");
            }
        } else {
            Logger.logSingleTag(getClassName(),
                    AdsManager.LOG_BANNER,
                    "Tried to create banner but resource id for ad unit is null");
        }
    }

    @Override
    public void createBannerAdFromLayout(Activity activity, final int viewId) {
        mBanner = activity.findViewById(viewId);
        setAdUnitId(mBanner.getAdUnitId());
        Logger.logSingleTag(getClassName(),
                AdsManager.LOG_BANNER,
                "Banner ad created from layout. Ad unit is [%s].", getAdUnitId());
    }

    @Override
    public void load(IAdLoadingEvents adLoadingEvents) {
        if(Validator.isValidString(getAdUnitId())) {
            Logger.logSingleTag(getClassName(), AdsManager.LOG_BANNER, "Banner does not have a listener yet. Assigning one");
            mBanner.setAdListener(getBannerAdListener(adLoadingEvents));
            mBanner.loadAd(getBannerAdRequest());
        } else {
            Logger.logSingleTag(getClassName(), AdsManager.LOG_BANNER, "Called to load banner but ad unit id is null");
        }
    }

    @Override
    public void show(Activity activity, IAdDisplayingEvent adDisplayingEvent) {
        //TODO implement showinf banner not from pre defined layout;
    }

    @Override
    public int getBannerHeightInPixels(Activity activity) {
        return AdSize.BANNER.getHeightInPixels(activity);
    }

    @Override
    public boolean hasAd() {
        return null != mBanner && isLoaded();
    }

    @Override
    public boolean isLoaded() {
        return mIsLoaded;
    }

    @Override
    public void setIsLoaded(final boolean isLoaded) {
        mIsLoaded = isLoaded;
    }

    @Override
    public boolean isShowing() {
        //TODO not rigth
        return mIsShowing;
    }

    @Override
    public void setIsShowing(boolean isShowing) {
        mIsShowing = isShowing;
    }

    @Override
    public String getAdUnitId() {
        return mAdUnit;
    }

    @Override
    public void setAdUnitId(final String adUnitId) {
        if (Validator.Ads.isValidAdMobAdUnitId(adUnitId)) {
            mAdUnit = adUnitId;
        }
    }

    @Override
    public void callOnResume(Activity activity) {
        if (Validator.isValidObject(mBanner)) {
            mBanner.resume();
        }
    }

    @Override
    public void callOnPause(Activity activity) {
        if (Validator.isValidObject(mBanner)) {
            mBanner.pause();
        }
    }

    @Override
    public void callOnDestroy(Activity activity) {
        if (Validator.isValidObject(mBanner)) {
            mBanner.destroy();
        }
    }

    @Override
    public void callOnCreate(Activity activity) {

    }

    private AdRequest getBannerAdRequest() {
        return new AdRequest.Builder().build();
    }

    private AdListener getBannerAdListener(IAdLoadingEvents adLoadingEvents) {
        return new AdListener() {
            @Override
            public void onAdClosed() {
                Logger.logSingleTag(getClassName(), AdsManager.LOG_BANNER, "Banner ad closed");
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                setIsLoaded(false);
                Logger.logSingleTag(getClassName(), AdsManager.LOG_BANNER, "Banner ad failed to load. Reason is [%s]", loadAdError.getMessage());
                if (Validator.isValidObject(adLoadingEvents)) {
                    adLoadingEvents.onAdFailedToLoad(loadAdError.getMessage());
                }
                super.onAdFailedToLoad(loadAdError);
            }

            @Override
            public void onAdOpened() {
                Logger.logSingleTag(getClassName(), AdsManager.LOG_BANNER, "Banner ad opened");
                super.onAdOpened();
            }

            @Override
            public void onAdLoaded() {
                setIsLoaded(true);
                Logger.logSingleTag(getClassName(), AdsManager.LOG_BANNER, "Banner ad loaded");
                if (Validator.isValidObject(adLoadingEvents)) {
                    adLoadingEvents.onAdLoaded();
                }
                super.onAdLoaded();
            }

            @Override
            public void onAdClicked() {
                Logger.logSingleTag(getClassName(), AdsManager.LOG_BANNER, "Banner ad clicked");
                super.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                setIsShowing(true);
                Logger.logSingleTag(getClassName(), AdsManager.LOG_BANNER, "Banner ad logged impression");
                super.onAdImpression();
            }
        };
    }

    @Override
    public String getClassName() {
        return "{AdMobBanner}";
    }
}
