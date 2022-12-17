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

    protected Interstitial(Activity act, EAdPlacement placement) {
        super(act, placement);
    }

    @Override
    public boolean canShow() {
        final int secondsPassedFromLastInterstitialDismissed = AdsStatsManger.getSecondsPassedFromLastInterstitialDismissed(getActivity());
        final int remoteConfigCoolDownValue = RemoteConfigManager.getInstance().getIntValue(RemoteConfigKeysAndDefaults.AD_INTERSTITIAL_COOL_DOWN_SECONDS);
        boolean hasCoolDownPassed = secondsPassedFromLastInterstitialDismissed > remoteConfigCoolDownValue;
        return hasCoolDownPassed && super.canShow();
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
        if (adCallback == EAdCallBacks.DISMISSED){
            AdsStatsManger.onInterstitialDismissed(getActivity());
        }
    }

}
