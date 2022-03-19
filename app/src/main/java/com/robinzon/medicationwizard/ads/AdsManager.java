package com.robinzon.medicationwizard.ads;

import android.app.Activity;

import com.robinzon.medicationwizard.ads.adsproviders.EAdsProvider;
import com.robinzon.medicationwizard.ads.adsproviders.admob.AdMob;
import com.robinzon.medicationwizard.ads.interfaces.IAdsInitializeCallBack;
import com.robinzon.medicationwizard.ads.interfaces.IAdsProvider;
import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AdsManager extends MedicationWizardSuper {
    public static boolean DISABLE_ADS = false;
    public static final String LOG_BANNER = "mwiz_Banner_Ad";
    public static final String LOG_INTERSTITIAL = "mwiz_Interstitial_Ad";
    public static final String LOG_REWARDED_INTERSTITIAL = "mwiz_Rewarded_Interstitial_Ad";
    public static final String LOG_REWARDED_VIDEO = "mwiz_Rewarded_Video_Ad";

    private final Map<EAdsProvider, IAdsProvider> mAdsProviders =
            Collections.unmodifiableMap(new HashMap<EAdsProvider, IAdsProvider>() {{
                put(EAdsProvider.ADMOB, new AdMob());
            }});

    private IAdsProvider getAdProvider() {
        if (Validator.isValidMap(getAdProvidersList())) {
            final IAdsProvider adProvider = getAdProvidersList().get(EAdsProvider.ADMOB);
            if (Validator.isValidObject(adProvider)) {
                return adProvider;
            }
        }
        return new AdMob();
    }

    private Map<EAdsProvider, IAdsProvider> getAdProvidersList() {
        return mAdsProviders;
    }

    public void initializeAds(final Activity activity, IAdsInitializeCallBack adsInitializeCallBack) {
        getAdProvider().initializeAds(activity, adsInitializeCallBack);
    }

    public void loadBanner(final Activity mainActivity) {
        getAdProvider().loadBanner(mainActivity);
    }

    public void showBanner(final Activity mainActivity) {
        getAdProvider().getBanner().show(mainActivity, null);
    }

    public void loadInterstitial(final Activity mainActivity) {
        getAdProvider().getInterstitial().load(mainActivity);
    }

    public void showInterstitial(final Activity activity) {
        getAdProvider().getInterstitial().show(activity,
                null);
    }

    public void loadRV(Activity mainActivity) {
        Logger.getInstance().log(getClassName(), getRvLogs(),
                "Ads Manger calling to load rv");
        if (!getAdProvider().getRewardedVideo().isLoaded()) {
            Logger.getInstance().log(getClassName(), getRvLogs(),
                    "Rv is not loaded. Loading one");
            getAdProvider().getRewardedVideo().setLoadingEventsListener(new IAdLoadingEvents() {
                @Override
                public void onAdLoaded() {
                    getAdProvider().getRewardedVideo().setIsLoaded(true);
                    Logger.getInstance().log(getClassName(), getRvLogs(),
                            "Rv ad loaded");
                }

                @Override
                public void onAdFailedToLoad(String reason) {
                    getAdProvider().getRewardedVideo().setIsLoaded(false);
                    Logger.getInstance().log(getClassName(), getRvLogs(),
                            "Rv ad failed to load. Reason is[%s]",
                            reason);
                }
            });
            getAdProvider().getRewardedVideo().load(mainActivity);
        }

    }

    private List<String> getRvLogs() {
        return new ArrayList<String>(2) {
            {
                add(LOG_REWARDED_VIDEO);
            }
        };
    }


    public void showRv(Activity activity) {
        getAdProvider().getRewardedVideo().show(activity, null);
    }

    public boolean isRvLoaded() {
        return getAdProvider().getRewardedVideo().isLoaded();
    }

    public void onResume(Activity activity) {
        if (Validator.isValidObject(getAdProvider())) {
            getAdProvider().onResume(activity);
        }
    }

    public void onPause(Activity activity) {
        if (Validator.isValidObject(getAdProvider())) {
            getAdProvider().onPause(activity);
        }
    }

    public void onDestroy(Activity activity) {
        if (Validator.isValidObject(getAdProvider())) {
            getAdProvider().onDestroy(activity);
        }
    }

    public void onCreate(Activity activity) {
        if (Validator.isValidObject(getAdProvider())) {
            getAdProvider().onCreate(activity);
        }
    }

    @Override
    public String getClassName() {
        return AdsManager.class.getSimpleName();
    }

    public boolean hasInterstitialToShow() {
        return getAdProvider().getInterstitial().hasAd();
    }

    public boolean hasRvToShow(){
        return getAdProvider().getRewardedVideo().hasAd();
    }
}
