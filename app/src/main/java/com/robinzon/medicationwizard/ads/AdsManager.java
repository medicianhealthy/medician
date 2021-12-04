package com.robinzon.medicationwizard.ads;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.adsproviders.EAdsProvider;
import com.robinzon.medicationwizard.ads.adsproviders.admob.AdMobAdProvider;
import com.robinzon.medicationwizard.ads.interfaces.IAdsInitializeCallBack;
import com.robinzon.medicationwizard.ads.interfaces.EAdsInitializeState;
import com.robinzon.medicationwizard.ads.interfaces.IAdsProvider;
import com.robinzon.medicationwizard.ads.rootclasses.ISuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AdsManager implements ISuper {
    public static boolean DISABLE_ADS = false;
    public static final String LOG_BANNER = "mwiz_Banner_Ad";
    public static final String LOG_INTERSTITIAL = "mwiz_Interstitial_Ad";
    public static final String LOG_REWARDED_INTERSTITIAL = "mwiz_Rewarded_Interstitial_Ad";
    public static final String LOG_REWARDED_VIDEO = "mwiz_Rewarded_Video_Ad";
    public static final List<String> LOGS_ADS =
            Collections.unmodifiableList(new ArrayList<String>() {{
                add(LOG_BANNER);
                add(LOG_INTERSTITIAL);
                add(LOG_REWARDED_INTERSTITIAL);
                add(LOG_REWARDED_VIDEO);
            }});

    private final Map<EAdsProvider, IAdsProvider> mAdsProviders =
            Collections.unmodifiableMap(new HashMap<EAdsProvider, IAdsProvider>() {{
                put(EAdsProvider.ADMOB , new AdMobAdProvider());
            }});

    private IAdsProvider getAdProvider() {
        if (Validator.isValidMap(getAdProvidersList())) {
            final IAdsProvider adProvider = getAdProvidersList().get(EAdsProvider.ADMOB);
            if (Validator.isValidObject(adProvider)) {
                return adProvider;
            }
        }
        return new AdMobAdProvider();
    }

    private Map<EAdsProvider, IAdsProvider> getAdProvidersList() {
        return mAdsProviders;
    }

    public void initializeAds(final Activity activity, IAdsInitializeCallBack adsInitializeCallBack) {
        getAdProvider().initialize(activity, adsInitializeCallBack);
    }


    public void showBanner(final Activity mainActivity) {
        Logger.getInstance().logSingleTag(getClassName(), LOG_BANNER, "AdsManger calling to show banner");
        if (getAdProvider().hasBanner()) {
            if (getAdProvider().getBanner().isLoaded()) {
                Logger.getInstance().logSingleTag(getClassName(), LOG_BANNER, "No need to load. showing banner");
                getAdProvider().getBanner().show(mainActivity, null);
            } else {
                Logger.getInstance().logSingleTag(getClassName(), LOG_BANNER, "Banner is " +
                        "not loaded. Calling to load it");
                getAdProvider().getBanner().load(getBannerAdLoadingEvents(mainActivity));
            }
        } else { // Banner object not live yet
            getAdProvider().getBanner().createBannerAdFromLayout(mainActivity, R.id.adView);
            getAdProvider().getBanner().load(getBannerAdLoadingEvents(mainActivity));
        }
    }

    @NonNull
    private IAdLoadingEvents getBannerAdLoadingEvents(Activity mainActivity) {
        return new IAdLoadingEvents() {
            @Override
            public void onAdLoaded() {
                Logger.getInstance().logSingleTag(getClassName(), LOG_BANNER, "Banner ad loaded. showing banner");
                if (!getAdProvider().getBanner().isShowing()) {
                    getAdProvider().getBanner().show(mainActivity, null);
                }
            }

            @Override
            public void onAdFailedToLoad(String reason) {
                Logger.getInstance().logSingleTag(getClassName(), LOG_BANNER, "Banner ad failed to load. Reason is [%s]", reason);

            }
        };
    }

    public void loadInterstitial(final Activity mainActivity) {
        Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Ads Manger calling to load interstitial");
        if (!getAdProvider().hasInterstitial()) {
            Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "There is no interstitial. Creating one");
            getAdProvider().getInterstitial().create(mainActivity, R.string.admob_interstitial_id_test);
        }
        if (!getAdProvider().getInterstitial().isLoaded()) {
            Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial is not loaded. Loading one");
            getAdProvider().getInterstitial().load(mainActivity, new IAdLoadingEvents() {
                @Override
                public void onAdLoaded() {
                    getAdProvider().getInterstitial().setIsLoaded(true);
                    Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial ad loaded");
                }

                @Override
                public void onAdFailedToLoad(String reason) {
                    getAdProvider().getInterstitial().setIsLoaded(false);
                    Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial ad failed to load");
                }
            });
        }
    }

    public boolean isInterstitialLoaded() {
        return getAdProvider().getInterstitial().isLoaded();
    }

    public void showInterstitial(final Activity activity) {
        getAdProvider().getInterstitial().show(activity,
                null);
    }

    public void loadRV(Activity mainActivity) {
        Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                "Ads Manger calling to load rv");
        if (!getAdProvider().hasRv()) {
            Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                    "Don't have an rv. Creating one");
            getAdProvider().getRewardedVideo().create(mainActivity,
                    R.string.admob_rv_id_test);
        }
        if (!getAdProvider().getRewardedVideo().isLoaded()) {
            Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                    "Rv is not loaded. Loading one");
            getAdProvider().getRewardedVideo().load(mainActivity,
                    new IAdLoadingEvents() {
                        @Override
                        public void onAdLoaded() {
                            getAdProvider().getRewardedVideo().setIsLoaded(true);
                            Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                                    "Rv ad loaded");
                        }

                        @Override
                        public void onAdFailedToLoad(String reason) {
                            getAdProvider().getRewardedVideo().setIsLoaded(false);
                            Logger.getInstance().logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                                    "Rv ad failed to load. Reason is[%s]",
                                    reason);
                        }
                    });
        }

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
        return "{AdsManager}";
    }
}
