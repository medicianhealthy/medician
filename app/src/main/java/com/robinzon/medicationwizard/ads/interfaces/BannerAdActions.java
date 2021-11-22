package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;

import com.robinzon.medicationwizard.ads.AdDisplayingEvent;
import com.robinzon.medicationwizard.ads.AdLoadingEvents;

public interface BannerAdActions {
    public void createBannerAd(final Activity activity, final int adUnitIdResourceId);
    public void createBannerAdFromLayout(final Activity activity, final int viewId);

    public void load(final AdLoadingEvents adLoadingEvents);
    public void show(final Activity activity, final AdDisplayingEvent adDisplayingEvent);
    public int getBannerHeightInPixels(final Activity activity);
    boolean hasAd();
}
