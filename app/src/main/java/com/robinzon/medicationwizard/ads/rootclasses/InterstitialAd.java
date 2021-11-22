package com.robinzon.medicationwizard.ads.rootclasses;

import com.robinzon.medicationwizard.ads.interfaces.InterstitialAdActions;

public abstract class InterstitialAd extends Ad implements InterstitialAdActions {
    @Override
    public String getClassName() {
        return "{InterstitialAd}";
    }
}
