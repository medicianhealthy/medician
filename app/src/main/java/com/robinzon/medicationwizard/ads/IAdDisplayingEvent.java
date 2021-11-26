package com.robinzon.medicationwizard.ads;

public interface IAdDisplayingEvent {
    public void onAdShown();
    public void onAdFailedToShow();
    public void onAdDismissed();
    public void onAdRewarded(final int reward);
}
