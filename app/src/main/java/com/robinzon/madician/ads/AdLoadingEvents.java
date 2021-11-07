package com.robinzon.madician.ads;

public interface AdLoadingEvents {

    public void onAdLoaded();
    public void onAdFailedToLoad(final String reason);
}
