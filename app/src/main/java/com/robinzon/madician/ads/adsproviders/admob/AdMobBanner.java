package com.robinzon.madician.ads.adsproviders.admob;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.robinzon.madician.R;
import com.robinzon.madician.ads.AdDisplayingEvent;
import com.robinzon.madician.ads.AdLoadingEvents;
import com.robinzon.madician.ads.AdsManager;
import com.robinzon.madician.ads.interfaces.BannerAdActions;
import com.robinzon.madician.ads.rootclasses.BannerAd;
import com.robinzon.madician.utils.Logger;
import com.robinzon.madician.utils.Validator;

public class AdMobBanner extends BannerAd implements BannerAdActions {
    private AdView mBanner;
    @Override
    public void createBannerAd(final Activity activity, final int adUnitIdResourceId) {
        if(isValidResourceIdForAdUnit(activity, adUnitIdResourceId)) {
            mBanner = new AdView(activity);
            mBanner.setAdSize(AdSize.BANNER);
            mBanner.setAdUnitId(getAdUnitId());
        }
    }

    @Override
    public void createBannerAdFromLayout(Activity activity , final int viewId) {
        mBanner = activity.findViewById(R.id.adView);
    }

    @Override
    public void load(AdLoadingEvents adLoadingEvents) {
        Logger.logSingleTag(getClassName(), AdsManager.LOG_BANNER, "Banner does not have a listener yet. Assigning one");
        mBanner.setAdListener(getBannerAdListener(adLoadingEvents));
        mBanner.loadAd(getBannerAdRequest());
    }

    @Override
    public void show(Activity activity, AdDisplayingEvent adDisplayingEvent) {

    }

    @Override
    public int getBannerHeightInPixels(Activity activity) {
        return AdSize.BANNER.getHeightInPixels(activity);
    }

    @Override
    public boolean hasAd() {
        return null != mBanner;
    }

    private boolean isValidResourceIdForAdUnit(final Activity activity, final int resourceId){
        if (Validator.isValidAndroidResourceId(resourceId)){
            final String adUnitId = activity.getString(resourceId);
            if (adUnitId.startsWith("ca-app-pub-")){
                setAdUnitId(adUnitId);
                return true;
            }
        }
        return false;
    }

    private AdRequest getBannerAdRequest() {
        return new AdRequest.Builder().build();
    }

    private AdListener getBannerAdListener(AdLoadingEvents adLoadingEvents) {
        return new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                Logger.logSingleTag(getClassName(),AdsManager.LOG_BANNER, "Banner ad closed");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                setIsLoaded(false);
                Logger.logSingleTag(getClassName(),AdsManager.LOG_BANNER, "Banner ad failed to load. Reason is [%s]", loadAdError.getMessage());
                if (Validator.isValidObject(adLoadingEvents)) {
                    adLoadingEvents.onAdFailedToLoad(loadAdError.getMessage());
                }
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                Logger.logSingleTag(getClassName(),AdsManager.LOG_BANNER, "Banner ad opened");

            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                setIsLoaded(true);
                Logger.logSingleTag(getClassName(),AdsManager.LOG_BANNER, "Banner ad loaded");
                if (Validator.isValidObject(adLoadingEvents)) {
                    adLoadingEvents.onAdLoaded();
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                Logger.logSingleTag(getClassName(),AdsManager.LOG_BANNER, "Banner ad clicked");
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                Logger.logSingleTag(getClassName(),AdsManager.LOG_BANNER, "Banner ad logged impression");
            }
        };
    }

    @Override
    public String getClassName() {
        return "{AdMobBanner}";
    }
}
