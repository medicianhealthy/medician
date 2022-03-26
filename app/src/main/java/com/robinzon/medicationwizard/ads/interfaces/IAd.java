package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;

import com.robinzon.medicationwizard.ads.IAdDisplayingEvent;
import com.robinzon.medicationwizard.ads.IAdLoadingEvents;

public interface IAd {
    void create(final Activity activity, final int adUnitIdResourceId);
    void load(final Activity activity);
    void show(final Activity activity, final IAdDisplayingEvent adDisplayingEvent);
    boolean hasAd();
    boolean isLoaded();
    void setIsLoaded(final boolean isLoaded);
    boolean isShowing();
    void setIsShowing(boolean isShowing);
    void onResume(final Activity activity);
    void onPause(final Activity activity);
    void onDestroy(final Activity activity);
    void onCreate(final Activity activity);
    void setLoadingEventsListener(final IAdLoadingEvents loadingEvents);
}
