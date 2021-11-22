package com.robinzon.medicationwizard.ads;

public interface AdLoadingEvents {

    public void onAdLoaded();
    public void onAdFailedToLoad(final String reason);
}
