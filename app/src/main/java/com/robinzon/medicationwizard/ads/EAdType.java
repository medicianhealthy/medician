package com.robinzon.medicationwizard.ads;

public enum EAdType {
    BANNER("Banner"),
    INTERSTITIAL("Interstitial"),
    INTERSTITIAL_REWARDED("Rewarded Interstitial"),
    REWARDED_VIDEO("Rewarded Video"),
    APP_OPEN("App open"),
    NATIVE("Native");

    private final String mName;

    EAdType(String name) {
        mName = name;
    }

    String getName() {
        return mName;
    }
}