package com.robinzon.medicationwizard.ads;

public enum EAdType {
    ALL("All Ads"),
    NONE("No Type"),
    SOME("Some"),
    BANNER("Banner"),
    INTERSTITIAL("Interstitial"),
    INTERSTITIAL_REWARDED("Rewarded Interstitial"),
    REWARDED_VIDEO("Rewarded Video"),
    APP_OPEN("App open");

    private final String mName;

    EAdType(String name) {
        mName = name;
    }

    String getName() {
        return mName;
    }
}