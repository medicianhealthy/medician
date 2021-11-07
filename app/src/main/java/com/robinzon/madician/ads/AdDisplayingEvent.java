package com.robinzon.madician.ads;

public interface AdDisplayingEvent {
    public void onAdShown();
    public void onAdFailedToShow();
    public void onAdDismissed();
    public void onAdRewarded(final int reward);
}
