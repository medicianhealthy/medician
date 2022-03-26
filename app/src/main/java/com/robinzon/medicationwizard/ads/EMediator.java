package com.robinzon.medicationwizard.ads;

public enum EMediator {
    ALL("All"),
    ADMOB("All Ads"),
    IRONSOURCE("No Type"),
    NONE("None");


    private final String mName;

    EMediator(String name) {
        mName = name;
    }

    String getName() {
        return mName;
    }
}
