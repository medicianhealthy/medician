package com.robinzon.medicationwizard.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.robinzon.medicationwizard.IContextProvider;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.adsproviders.EAdsProvider;
import com.robinzon.medicationwizard.ads.adsproviders.admob.AdMob;
import com.robinzon.medicationwizard.ads.interfaces.EAdsInitializeState;
import com.robinzon.medicationwizard.ads.interfaces.IAdsInitializeCallBack;
import com.robinzon.medicationwizard.ads.interfaces.IAdsProvider;
import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.TimeInterval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AdsManager extends MedicationWizardSuper {
    public static final String LOG_BANNER = "mwiz_Banner_Ad";
    public static final String LOG_INTERSTITIAL = "mwiz_Interstitial_Ad";
    public static final String LOG_REWARDED_INTERSTITIAL = "mwiz_Rewarded_Interstitial_Ad";
    public static final String LOG_REWARDED_VIDEO = "mwiz_Rewarded_Video_Ad";
    public static final String RCKEY_ADS_TIMER_BANNER_GRACE_MINUTES = "ads_timer_banner_grace_minutes";
    public static final String RCKEY_ADS_TIMER_INTER_GRACE_MINUTES = "ads_timer_inter_grace_minutes";
    public static final String RCKEY_ADS_TIMER_RV_GRACE_MINUTES = "ads_timer_rv_grace_minutes";
    public static final int TICK_INTERVAL_SECONDS = 8;
    private static IContextProvider mContextProvider;
    private Ticker mTicker;

    /**
     * Object to handle all App's logic for ads.
     * Assumes that Firebase remote config values already fetched.
     */
    public AdsManager(final IContextProvider contextProvider) {
        mContextProvider = contextProvider;
    }

    private final Map<EAdsProvider, IAdsProvider> mAdsProviders =
            Collections.unmodifiableMap(new HashMap<EAdsProvider, IAdsProvider>() {{
                put(EAdsProvider.ADMOB, new AdMob());
            }});

    private IAdsProvider getAdProvider() {
        final IAdsProvider adProvider = getAdProvidersList().get(EAdsProvider.ADMOB);
        if (null != adProvider) {
            return adProvider;
        }
        return new AdMob();
    }

    private Map<EAdsProvider, IAdsProvider> getAdProvidersList() {
        return mAdsProviders;
    }


    public void onAdsFinishedInitializingSuccessfully(final Activity mainActivity) {
        loadBanner(mainActivity);
        loadInterstitial(mainActivity);
        loadRV(mainActivity);
        mainActivity.findViewById(R.id.text_home).setOnClickListener(v -> {
            if (hasRvToShow()) {
                showRv(mainActivity);
            }
        });
    }

    public void initializeAds(final Activity activity) {
        getAdProvider().initializeAds(activity, new IAdsInitializeCallBack() {
            @Override
            public void onAdsInitialized(EAdsInitializeState adsInitializeState) {
                if (EAdsInitializeState.SUCCESSFULLY == adsInitializeState) {
                    onAdsFinishedInitializingSuccessfully(activity);
                }
            }
        });
    }

    public void loadBanner(final Activity mainActivity) {
        if (shouldShowAd(EAdType.BANNER, EMediator.ADMOB)) {
            getAdProvider().loadBanner(mainActivity);
        }
    }

    private boolean shouldShowAd(final EAdType adType, final EMediator mediator) {
        if (mediator == EMediator.ADMOB) {
            if (EAdType.BANNER == adType)
                return true;
            else if (EAdType.INTERSTITIAL == adType)
                return true;
            else if (EAdType.REWARDED_VIDEO == adType)
                return true;
            else
                return false;
        }
        return false;
    }

    public void showBanner(final Activity mainActivity) {
        getAdProvider().getBanner().show(mainActivity, null);
    }

    public void loadInterstitial(final Activity mainActivity) {
        if (AdBreaker.canShowAd(EAdType.INTERSTITIAL, EMediator.ADMOB)) {
            getAdProvider().getInterstitial().load(mainActivity);
        }
    }

    public void showInterstitial(final Activity activity) {
        getAdProvider().getInterstitial().show(activity,
                null);
    }

    public void loadRV(Activity mainActivity) {
        Logger.getInstance().log(getClassName(), getRvLogs(),
                "Ads Manger calling to load rv");
        if (AdBreaker.canShowAd(EAdType.REWARDED_VIDEO, EMediator.ADMOB)) {
            if (!getAdProvider().getRewardedVideo().hasAd()) {
                Logger.getInstance().log(getClassName(), getRvLogs(),
                        "Rv is not loaded. Loading one");
                getAdProvider().getRewardedVideo().setLoadingEventsListener(new IAdLoadingEvents() {
                    @Override
                    public void onAdLoaded() {
                        Logger.getInstance().log(getClassName(), getRvLogs(),
                                "Rv ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(String reason) {
                        Logger.getInstance().log(getClassName(), getRvLogs(),
                                "Rv ad failed to load. Reason is[%s]",
                                reason);
                    }
                });
                getAdProvider().getRewardedVideo().load(mainActivity);
            }
        }
    }

    private List<String> getRvLogs() {
        return new ArrayList<String>(2) {
            {
                add(LOG_REWARDED_VIDEO);
            }
        };
    }

    @Override
    public List<String> getLogTags() {
        if (null == mLogTags || null == getLogTags() || getLogTags().isEmpty()){
            setLogTags(new ArrayList<String>() {{
                add(LOG_BANNER);
                add(LOG_REWARDED_VIDEO);
                add(LOG_INTERSTITIAL);
                add(LOG_REWARDED_INTERSTITIAL);
           }});
        }
        return super.getLogTags();
    }

    public void showRv(Activity activity) {
        getAdProvider().getRewardedVideo().show(activity, null);
    }

    public boolean isRvLoaded() {
        return getAdProvider().getRewardedVideo().isLoaded();
    }

    public void onResume(MainActivity activity) {
        //Always put the below line on start of this method
        setContextProvider(activity);
        if (null != getAdProvider()) {
            getAdProvider().onResume(activity);
        }
        getTicker().sendEmptyMessageDelayed(Ticker.MESSAGE_TICK, TimeInterval.MilliSeconds.getFromSeconds(TICK_INTERVAL_SECONDS));
        AdBreaker.onResume(activity);

    }

    public void onPause(Activity activity) {
        if (null != getAdProvider()) {
            getAdProvider().onPause(activity);
        }
        AdBreaker.onPause(activity);
        getTicker().removeMessages(Ticker.MESSAGE_TICK);
        //Always put the below line on the end of this onPause
        setContextProvider(null);
    }

    public void onStop() {
    }

    public void onDestroy(Activity activity) {
        if (null != getAdProvider()) {
            getAdProvider().onDestroy(activity);
        }
    }

    public void onCreate(Activity activity) {
        AdBreaker.setAdsGracePeriods(new AdsGracePeriod());
        if (null != getAdProvider()) {
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

    public boolean hasRvToShow() {
        return getAdProvider().getRewardedVideo().hasAd();
    }

    public void setContextProvider(MainActivity mainActivity) {
        mContextProvider = mainActivity;
    }


    private static void tick() {
        final Context context = null != mContextProvider ? mContextProvider.getContext() : null;
        if (null != context) {
            AdBreaker.tick(context);
        }
    }

    private Ticker getTicker() {
        if (null == mTicker) {
            mTicker = new Ticker();
        }
        return mTicker;
    }

    private static class Ticker extends Handler {
        public static final int MESSAGE_TICK = 1;
        public void handleMessage(final Message message) {
            switch (message.what) {
                case MESSAGE_TICK:
                    AdsManager.tick();
                    break;
                default:
                    break;
            }
        }
    }




}
