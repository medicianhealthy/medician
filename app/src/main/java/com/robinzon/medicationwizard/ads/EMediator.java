package com.robinzon.medicationwizard.ads;

public enum EMediator {
    ALL("All"),
    ADMOB("AdMob"),
    IRONSOURCE("IronSource"),
    NONE("None");


    private final String mName;

    EMediator(String name) {
        mName = name;
    }

    String getName() {
        return mName;
    }
}
