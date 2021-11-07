package com.robinzon.madician.ads.rootclasses;

import com.robinzon.madician.ads.interfaces.InterstitialAdActions;

public abstract class InterstitialAd extends Ad implements InterstitialAdActions {
    @Override
    public String getClassName() {
        return "{InterstitialAd}";
    }
}
