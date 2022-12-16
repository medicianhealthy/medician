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
import com.robinzon.medicationwizard.utils.Logger;

import java.util.ArrayList;
import java.util.List;

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
        super.handleAdCallBacks(adCallback, adsLifeCycleCallBack);
        handleAdCallBacks(adCallback, adsLifeCycleCallBack, null);
    }

    @Override
    public void handleAdCallBacks(EAdCallBacks adCallback, IAdsLifeCycleCallBack adsLifeCycleCallBack, AdError adError) {
        super.handleAdCallBacks(adCallback, adsLifeCycleCallBack, adError);
        if (adCallback == EAdCallBacks.DISMISSED){
            AdsStatsManger.onInterstitialDismissed(getActivity());
        }
    }

    @Override
    protected List<String> getLogTags() {
        if (Logger.isLoggingEnabled()){
            final ArrayList<String> tags = new ArrayList<>(1);
            tags.add("interstitial_ad");
            return tags;
        }
        return null;
    }
}
