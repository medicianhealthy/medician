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
    public static final String LOG_BANNER = "madician_ad_banner";
    public static final String LOG_INTERSTITIAL = "madician_ad_inter";
    public static final String LOG_REWARDED_INTERSTITIAL = "madician_ad_rewarded_inter";
    public static final String LOG_REWARDED_VIDEO = "madician_ad_rv";
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

    public void initializeAdMobAds(final Activity activity, IAdsInitializeCallBack adsInitializeCallBack) {
        if (!Validator.isValidObject(activity)) {
            return;
        }
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                Logger.logMultipleTags(getClassName(), LOGS_ADS, "Initialization of AdMob ads completed.");
                final Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                byte networksReadyCounter = 0;
                for (String key : statusMap.keySet()) {
                    final AdapterStatus adapterStatus = statusMap.get(key);
                    if (Validator.isValidObject(adapterStatus)) {
                        if (AdapterStatus.State.READY == adapterStatus.getInitializationState()) {
                            networksReadyCounter++;
                        }
                        Logger.logMultipleTags(getClassName(), LOGS_ADS,
                                "Initialization of [%s,%s] status is [%s]",
                                key,
                                adapterStatus.getDescription(),
                                adapterStatus.getInitializationState() == AdapterStatus.State.READY ? "ready" : "not ready");
                    }
                }
                if (null != adsInitializeCallBack) {
                    if (networksReadyCounter == statusMap.size()) {
                        adsInitializeCallBack.onAdsInitialized(EAdsInitializeState.ALL_NETWORKS_READY);
                    } else if (0 == networksReadyCounter) {
                        adsInitializeCallBack.onAdsInitialized(EAdsInitializeState.NO_NETWORKS_ARE_READY);
                    } else {
                        adsInitializeCallBack.onAdsInitialized(EAdsInitializeState.SOME_NETWORKS_READY);
                    }
                }
            }
        });
    }


    public void showBanner(final Activity mainActivity) {
        Logger.logSingleTag(getClassName(), LOG_BANNER, "AdsManger calling to show banner");
        if (getAdProvider().hasBanner()) {
            if (getAdProvider().getBanner().isLoaded()) {
                Logger.logSingleTag(getClassName(), LOG_BANNER, "No need to load. showing banner");
                getAdProvider().getBanner().show(mainActivity, null);
            } else {
                Logger.logSingleTag(getClassName(), LOG_BANNER, "Banner is " +
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
                Logger.logSingleTag(getClassName(), LOG_BANNER, "Banner ad loaded. showing banner");
                if (!getAdProvider().getBanner().isShowing()) {
                    getAdProvider().getBanner().show(mainActivity, null);
                }
            }

            @Override
            public void onAdFailedToLoad(String reason) {
                Logger.logSingleTag(getClassName(), LOG_BANNER, "Banner ad failed to load. Reason is [%s]", reason);

            }
        };
    }

    public void loadInterstitial(final Activity mainActivity) {
        Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Ads Manger calling to load interstitial");
        if (!getAdProvider().hasInterstitial()) {
            Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "There is no interstitial. Creating one");
            getAdProvider().getInterstitial().create(mainActivity, R.string.admob_interstitial_id_test);
        }
        if (!getAdProvider().getInterstitial().isLoaded()) {
            Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial is not loaded. Loading one");
            getAdProvider().getInterstitial().load(mainActivity, new IAdLoadingEvents() {
                @Override
                public void onAdLoaded() {
                    getAdProvider().getInterstitial().setIsLoaded(true);
                    Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial ad loaded");
                }

                @Override
                public void onAdFailedToLoad(String reason) {
                    getAdProvider().getInterstitial().setIsLoaded(false);
                    Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial ad failed to load");
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
        Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                "Ads Manger calling to load rv");
        if (!getAdProvider().hasRv()) {
            Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                    "Don't have an rv. Creating one");
            getAdProvider().getRewardedVideo().create(mainActivity,
                    R.string.admob_rv_id_test);
        }
        if (!getAdProvider().getRewardedVideo().isLoaded()) {
            Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                    "Rv is not loaded. Loading one");
            getAdProvider().getRewardedVideo().load(mainActivity,
                    new IAdLoadingEvents() {
                        @Override
                        public void onAdLoaded() {
                            getAdProvider().getRewardedVideo().setIsLoaded(true);
                            Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                                    "Rv ad loaded");
                        }

                        @Override
                        public void onAdFailedToLoad(String reason) {
                            getAdProvider().getRewardedVideo().setIsLoaded(false);
                            Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
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
