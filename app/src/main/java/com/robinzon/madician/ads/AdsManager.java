package com.robinzon.madician.ads;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.robinzon.madician.R;
import com.robinzon.madician.ads.adsproviders.EAdsProvider;
import com.robinzon.madician.ads.adsproviders.admob.AdMobAdProvider;
import com.robinzon.madician.ads.interfaces.AdsInitializeCallBack;
import com.robinzon.madician.ads.rootclasses.AdsProvider;
import com.robinzon.madician.ads.rootclasses.MedicianSuperClass;
import com.robinzon.madician.utils.Logger;
import com.robinzon.madician.utils.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class AdsManager extends MedicianSuperClass {
    public static boolean DISABLE_ADS = false;
    public static final String LOG_BANNER = "madician_ad_banner";
    public static final String LOG_INTERSTITIAL = "madician_ad_inter";
    public static final String LOG_REWARDED_INTERSTITIAL = "madician_ad_rewarded_inter";
    public static final String LOG_REWARDED_VIDEO = "madician_ad_rv";
    public static final ArrayList<String> LOGS_ADS = new ArrayList<String>() {{
        add(LOG_BANNER);
        add(LOG_INTERSTITIAL);
        add(LOG_REWARDED_INTERSTITIAL);
        add(LOG_REWARDED_VIDEO);
    }};
    private final Map<EAdsProvider, AdsProvider> mAdsProviders;

    private AdMobAdProvider getAdProvider(final EAdsProvider adsProviderENUM) {
        if (Validator.isValidMap(getAdProvidersList())){
            final AdsProvider adProvider = getAdProvidersList().get(adsProviderENUM);
            if (Validator.isValidObject(adProvider)) {
                return (AdMobAdProvider) adProvider;
            }
        }
        return new AdMobAdProvider();
    }

    private Map<EAdsProvider, AdsProvider> getAdProvidersList() {
        return mAdsProviders;
    }

    public AdsManager() {
        mAdsProviders = new HashMap<>(1);
        mAdsProviders.put(EAdsProvider.ADMOB, new AdMobAdProvider());
    }

    public void initializeAdMobAds(final Activity activity, AdsInitializeCallBack adsInitializeCallBack) {
        if(!Validator.isValidObject(activity)){
            return;
        }
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                Logger.logMultipleTags(getClassName(), LOGS_ADS,"Initialization of AdMob ads completed.");
                final Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                byte networksReadyCounter = 0;
                for (String key:statusMap.keySet()) {
                    final AdapterStatus adapterStatus = statusMap.get(key);
                    if(Validator.isValidObject(adapterStatus)) {
                        if(AdapterStatus.State.READY == adapterStatus.getInitializationState()){
                            networksReadyCounter++;
                        }
                        Logger.logMultipleTags(getClassName(), LOGS_ADS,
                                "Initialization of [%s,%s] status is [%s]",
                                key,
                                adapterStatus.getDescription(),
                                adapterStatus.getInitializationState() == AdapterStatus.State.READY ? "ready" : "not ready");
                    }
                }
                if(null != adsInitializeCallBack) {
                    if (networksReadyCounter == statusMap.size()) {
                        adsInitializeCallBack.onAdsInitialized(AdsInitializeCallBack.AdsInitializeState.ALL_NETWORKS_READY);
                    } else if (0 == networksReadyCounter) {
                        adsInitializeCallBack.onAdsInitialized(AdsInitializeCallBack.AdsInitializeState.NO_NETWORKS_ARE_READY);
                    } else {
                        adsInitializeCallBack.onAdsInitialized(AdsInitializeCallBack.AdsInitializeState.SOME_NETWORKS_READY);
                    }
                }
            }
        });
    }


    public void showBanner(final Activity mainActivity) {
        Logger.logSingleTag(getClassName(), LOG_BANNER, "AdsManger calling to show banner");
        if(getAdProvider(EAdsProvider.ADMOB).hasBanner()) {
            if (getAdProvider(EAdsProvider.ADMOB).getBanner().isLoaded()) {
                Logger.logSingleTag(getClassName(), LOG_BANNER, "No need to load. showing banner");
                getAdProvider(EAdsProvider.ADMOB).getBanner().show(mainActivity, null);
            } else {
                Logger.logSingleTag(getClassName(), LOG_BANNER, "Banner is " +
                        "not loaded. Calling to load it");
                getAdProvider(EAdsProvider.ADMOB).getBanner().load(getBannerAdLoadingEvents(mainActivity));
            }
        } else { // Banner object not live yet
            getAdProvider(EAdsProvider.ADMOB).getBanner().createBannerAdFromLayout(mainActivity , R.id.adView);
            getAdProvider(EAdsProvider.ADMOB).getBanner().load(getBannerAdLoadingEvents(mainActivity));
        }
    }

    @NonNull
    private AdLoadingEvents getBannerAdLoadingEvents(Activity mainActivity) {
        return new AdLoadingEvents() {
            @Override
            public void onAdLoaded() {
                Logger.logSingleTag(getClassName(), LOG_BANNER, "Banner ad loaded. showing banner");
                if(!getAdProvider(EAdsProvider.ADMOB).getBanner().isShowing()) {
                    getAdProvider(EAdsProvider.ADMOB).getBanner().show(mainActivity, null);
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
        if (!getAdProvider(EAdsProvider.ADMOB).hasInterstitial()) {
            Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "There is no interstitial. Creating one");
            getAdProvider(EAdsProvider.ADMOB).getInterstitial().create(mainActivity, R.string.admob_interstitial_id_test);
        }
        if(!getAdProvider(EAdsProvider.ADMOB).getInterstitial().isLoaded()) {
            Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial is not loaded. Loading one");
            getAdProvider(EAdsProvider.ADMOB).getInterstitial().load(mainActivity, new AdLoadingEvents() {
                @Override
                public void onAdLoaded() {
                    getAdProvider(EAdsProvider.ADMOB).getInterstitial().setIsLoaded(true);
                    Logger.logSingleTag(getClassName(),AdsManager.LOG_INTERSTITIAL, "Interstitial ad loaded");
                }

                @Override
                public void onAdFailedToLoad(String reason) {
                    getAdProvider(EAdsProvider.ADMOB).getInterstitial().setIsLoaded(false);
                    Logger.logSingleTag(getClassName(), AdsManager.LOG_INTERSTITIAL, "Interstitial ad failed to load");
                }
            });
        }
    }

    public boolean isInterstitialLoaded() {
        return getAdProvider(EAdsProvider.ADMOB).getInterstitial().isLoaded();
    }

    public void showInterstitial(final Activity activity) {
        getAdProvider(EAdsProvider.ADMOB).getInterstitial().show(activity,
                null);
    }

    public void loadRV(Activity mainActivity) {
        Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                "Ads Manger calling to load rv");
        if(!getAdProvider(EAdsProvider.ADMOB).hasRv()){
            Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                    "Don't have an rv. Creating one");
            getAdProvider(EAdsProvider.ADMOB).getRewardedVideo().create(mainActivity,
                    R.string.admob_rv_id_test);
        }
        if(!getAdProvider(EAdsProvider.ADMOB).getRewardedVideo().isLoaded()){
            Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                    "Rv is not loaded. Loading one");
            getAdProvider(EAdsProvider.ADMOB).getRewardedVideo().load(mainActivity,
                    new AdLoadingEvents() {
                        @Override
                        public void onAdLoaded() {
                            getAdProvider(EAdsProvider.ADMOB).getRewardedVideo().setIsLoaded(true);
                            Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                                    "Rv ad loaded");
                        }

                        @Override
                        public void onAdFailedToLoad(String reason) {
                            getAdProvider(EAdsProvider.ADMOB).getRewardedVideo().setIsLoaded(false);
                            Logger.logSingleTag(getClassName(), AdsManager.LOG_REWARDED_VIDEO,
                                    "Rv ad failed to load. Reason is[%s]",
                                    reason);
                        }
                    });
        }

    }

    public void showRv(Activity activity) {
        getAdProvider(EAdsProvider.ADMOB).getRewardedVideo().show(activity, null);
    }

    public boolean isRvLoaded() {
        return getAdProvider(EAdsProvider.ADMOB).getRewardedVideo().isLoaded();
    }

    final String getClassNameForLog(){
        return "{AdsManager} - ";
    }

    public void onResume(Activity activity) {
        if (Validator.isValidObject(getAdProvider(EAdsProvider.ADMOB))){
            getAdProvider(EAdsProvider.ADMOB).onResume(activity);
        }
    }

    public void onPause(Activity activity) {
        if (Validator.isValidObject(getAdProvider(EAdsProvider.ADMOB))){
            getAdProvider(EAdsProvider.ADMOB).onPause(activity);
        }
    }

    public void onDestroy(Activity activity) {
        if (Validator.isValidObject(getAdProvider(EAdsProvider.ADMOB))){
            getAdProvider(EAdsProvider.ADMOB).onDestroy(activity);
        }
    }

    public void onCreate(Activity activity) {
        if (Validator.isValidObject(getAdProvider(EAdsProvider.ADMOB))){
            getAdProvider(EAdsProvider.ADMOB).onCreate(activity);
        }
    }

    @Override
    public String getClassName() {
        return "{AdsManager}";
    }
}
