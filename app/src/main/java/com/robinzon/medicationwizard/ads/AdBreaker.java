package com.robinzon.medicationwizard.ads;

import android.content.Context;

import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.utils.TimeInterval;

public class AdBreaker extends MedicationWizardSuper {
    private static final String KEY_TOTAL_SESSION_TIME_MIN = "key_tstm";
    private static AdsGracePeriod sAdsGracePeriod;
    private static long sSessionStartTimeStamp;
    private static float mAccumulatingSessionTimeMinutes;

    public static void setAdsGracePeriods(final AdsGracePeriod adsGracePeriod) {
        sAdsGracePeriod = adsGracePeriod;
    }

    @Override
    public String getClassName() {
        return AdBreaker.class.getSimpleName();
    }

    public static boolean canShowAd(final EAdType adType, EMediator mediator) {
        switch (adType) {
            case SOME:
                return shouldShowBannerAd(mediator)
                        || shouldShowInterstitialAd(mediator)
                        || shouldShowRvAd(mediator)
                        || shouldShowRewardedInterstitialAd(mediator);
            case ALL:
                return shouldShowBannerAd(mediator)
                        && shouldShowInterstitialAd(mediator)
                        && shouldShowRvAd(mediator)
                        && shouldShowRewardedInterstitialAd(mediator);
            case BANNER:
                return shouldShowBannerAd(mediator);
            case INTERSTITIAL:
                return shouldShowInterstitialAd(mediator);
            case INTERSTITIAL_REWARDED:
                return shouldShowRewardedInterstitialAd(mediator);
            case REWARDED_VIDEO:
                return shouldShowRvAd(mediator);
            case NONE:
                return true;
            default:
                return false;
        }
    }

    private static boolean shouldShowRewardedInterstitialAd(EMediator mediator) {
        switch (mediator) {
            case ALL:
                return shouldShowRewardedInterstitialAdForAdMb() && shouldShowRewardedInterstitialAdForIronSource();
            case ADMOB:
                return shouldShowRewardedInterstitialAdForAdMb();
            case IRONSOURCE:
                return true;
            case NONE:
                return false;
            default:
                return false;
        }
    }


    private static boolean shouldShowRvAd(EMediator mediator) {
        switch (mediator) {
            case ALL:
                return shouldShowRvAdForAdMob() && shouldShowRvAdForIronSource();
            case ADMOB:
                return shouldShowRvAdForAdMob();
            case IRONSOURCE:
                return true;
            case NONE:
                return false;
            default:
                return false;
        }
    }


    private static boolean shouldShowInterstitialAd(EMediator mediator) {
        switch (mediator) {
            case ALL:
                return shouldShowInterstitialAdForAdMob() && shouldShowInterstitialAdForIronSource();
            case IRONSOURCE:
                return true;
            case ADMOB:
                return shouldShowInterstitialAdForAdMob();
            case NONE:
                return false;
            default:
                return false;
        }
    }


    private static boolean shouldShowBannerAd(EMediator mediator) {
        switch (mediator) {
            case ALL:
                return shouldShowBannerAdForAdMob() && shouldShowBannerAdForIronSource();
            case IRONSOURCE:
                return true;
            case ADMOB:
                return shouldShowBannerAdForAdMob();
            case NONE:
                return false;
            default:
                return false;
        }
    }


    public static void onResume(final Context context) {
        setAccumulatingSessionTimeMinutes(SharedPreferencesManager.getInstance(context).getFloat(KEY_TOTAL_SESSION_TIME_MIN, 0F));
        sSessionStartTimeStamp = System.currentTimeMillis();
    }

    public static void onPause(final Context context) {
        final float currentSessionTimeInMinutes = TimeInterval.Minutes.getFromMilliSeconds(System.currentTimeMillis() - sSessionStartTimeStamp);
        SharedPreferencesManager.getInstance(context).setValue(KEY_TOTAL_SESSION_TIME_MIN,
                currentSessionTimeInMinutes + getAccumulatingSessionTimeMinutes());
        sSessionStartTimeStamp = 0L;
    }

    public static void tick(Context context){
        onPause(context);
    }

    private static void setAccumulatingSessionTimeMinutes(final float totalTime) {
        mAccumulatingSessionTimeMinutes = totalTime;
    }

    private static float getAccumulatingSessionTimeMinutes() {
        return mAccumulatingSessionTimeMinutes;
    }

    private static boolean shouldShowBannerAdForIronSource() {
        return false;
    }

    private static boolean shouldShowBannerAdForAdMob() {
        final float totalPlayTimeMinutes = getAccumulatingSessionTimeMinutes();
        if (totalPlayTimeMinutes < 2F){
            return false;
        } else return hasEngagedEnoughForBanner() || totalPlayTimeMinutes >= 5F;
    }

    private static boolean hasEngagedEnoughForBanner() {
        return false;
    }

    private static boolean shouldShowRvAdForAdMob() {
        return true;
    }

    private static boolean shouldShowRvAdForIronSource() {
        return false;
    }

    private static boolean shouldShowRewardedInterstitialAdForAdMb() {
        return false;
    }

    private static boolean shouldShowRewardedInterstitialAdForIronSource() {
        return false;
    }

    private static boolean shouldShowInterstitialAdForIronSource() {
        return false;
    }

    private static boolean shouldShowInterstitialAdForAdMob() {
        return false;
    }


}
