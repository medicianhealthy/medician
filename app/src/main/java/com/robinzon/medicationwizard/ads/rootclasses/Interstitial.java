package com.robinzon.medicationwizard.ads.rootclasses;

import android.app.Activity;

import com.robinzon.medicationwizard.ads.EAdType;
import com.robinzon.medicationwizard.ads.interfaces.IInterstitial;

public abstract class Interstitial extends FullScreenAd implements IInterstitial {


    protected Interstitial(Activity act, String placement) {
        super(act, placement);
    }

    @Override
    public EAdType getAdType() {
        return EAdType.INTERSTITIAL;
    }




}
