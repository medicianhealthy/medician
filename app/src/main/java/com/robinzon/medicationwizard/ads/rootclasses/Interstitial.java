package com.robinzon.medicationwizard.ads.rootclasses;

import android.app.Activity;

import com.google.android.gms.ads.AdError;
import com.robinzon.medicationwizard.ads.AdsStatsManger;
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.EAdType;
import com.robinzon.medicationwizard.ads.interfaces.IAdsLifeCycleCallBack;
import com.robinzon.medicationwizard.ads.interfaces.IInterstitial;
import com.robinzon.medicationwizard.remoteconfig.RemoteConfigKeysAndDefaults;
import com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager;

public abstract class Interstitial extends FullScreenAd implements IInterstitial {



    private final byte COOL_DOWN_SECONDS;
    protected Interstitial(Activity act, EAdPlacement placement) {
        super(act, placement);
        COOL_DOWN_SECONDS = (byte) RemoteConfigManager.getInstance().getIntValue(RemoteConfigKeysAndDefaults.AD_INTERSTITIAL_COOL_DOWN_SECONDS);
    }

    protected void logMessageOnInterstitialShouldNotBeLoaded() {
        final boolean isLoadedButExpired = isLoaded() && isExpired();
        final boolean isLoaded = isLoaded();
        final boolean isInLoadingProgress = isInLoadingProgress();
        logMessage("Got a call to load interstitial. Interstitial shouldn't be loaded now. Possible reasons are %s",
                "Load but expired {" +
                        isLoadedButExpired +
                        "}, loaded {" +
                        isLoaded +
                        "}, is in loading progress{" +
                        isInLoadingProgress +
                        "}");
    }

    @Override
    public boolean canShow() {
        return hasCoolDownPassedSinceLastImpression() && super.canShow();
    }

    @Override
    public boolean hasCoolDownPassedSinceLastImpression() {
        final int secondsPassedFromLastInterstitialDismissed = AdsStatsManger.getInstance().getSecondsPassedFromLastInterstitialDismissed(getActivity());
        final int remoteConfigCoolDownValue = RemoteConfigManager.getInstance().getIntValue(RemoteConfigKeysAndDefaults.AD_INTERSTITIAL_COOL_DOWN_SECONDS);
        return secondsPassedFromLastInterstitialDismissed > remoteConfigCoolDownValue;
    }

    protected void logMessageOnInterstitialCantBeShown() {
        final boolean isObjectValid = null != getAdCoreObject();
        final boolean hasCoolDownPassed = hasCoolDownPassedSinceLastImpression();
        final boolean isExpired = isExpired();
        final boolean isShowing = isShowing();
        final boolean isLoaded = isLoaded();
        final String builder = "object valid {" +
                isObjectValid +
                "}, cool down passes {" +
                hasCoolDownPassed +
                "}, expired {" +
                isExpired +
                "}, showing {" +
                isShowing +
                "}, loaded {" +
                isLoaded + "}";
        logMessage("Got a call to show interstitial. Interstitial can't be shown now. Possible reasons are %s", builder);
    }

    @Override
    public EAdType getAdType() {
        return EAdType.INTERSTITIAL;
    }


    @Override
    public void handleAdCallBacks(EAdCallBacks adCallback, IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        this.handleAdCallBacks(adCallback, adsLifeCycleCallBack, null);
    }

    @Override
    public void handleAdCallBacks(EAdCallBacks adCallback, IAdsLifeCycleCallBack adsLifeCycleCallBack, AdError adError) {
        super.handleAdCallBacks(adCallback, adsLifeCycleCallBack, adError);
        if (adCallback == EAdCallBacks.DISMISSED) {
            AdsStatsManger.getInstance().onInterstitialDismissed(getActivity());
        }
    }

    public byte getCoolDownInSeconds() {
        return COOL_DOWN_SECONDS;
    }

}
