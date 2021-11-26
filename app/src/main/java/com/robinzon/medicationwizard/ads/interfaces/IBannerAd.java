package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;
import android.content.Context;

import com.robinzon.medicationwizard.ads.IAdDisplayingEvent;
import com.robinzon.medicationwizard.ads.IAdLoadingEvents;

public interface IBannerAd extends IAd{
    public void createBannerAd(final Activity activity, final int adUnitIdResourceId);
    public void createBannerAdFromLayout(final Activity activity, final int viewId);
    public void load(final IAdLoadingEvents adLoadingEvents);
    public void show(final Activity activity, final IAdDisplayingEvent adDisplayingEvent);
    public int getBannerHeightInPixels(final Activity activity);
}
