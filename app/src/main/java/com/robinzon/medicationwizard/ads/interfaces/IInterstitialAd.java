package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;
import android.content.Context;

import com.robinzon.medicationwizard.ads.IAdDisplayingEvent;
import com.robinzon.medicationwizard.ads.IAdLoadingEvents;

public interface IInterstitialAd extends IAd{
    void create(final Activity activity, final int adUnitResourceId);
    void load(final Activity activity, final IAdLoadingEvents adLoadingEvents);
    void show(final Activity activity, final IAdDisplayingEvent adDisplayingEvent);
}
