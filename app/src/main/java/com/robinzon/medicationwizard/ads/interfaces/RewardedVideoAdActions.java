package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;

import com.robinzon.medicationwizard.ads.AdDisplayingEvent;
import com.robinzon.medicationwizard.ads.AdLoadingEvents;

public interface RewardedVideoAdActions {
    public void create(Activity mainActivity, final int adUnitIdResourceId);
    public void load(final Activity activity, final AdLoadingEvents adLoadingEvents);
    public void show(final Activity activity, final AdDisplayingEvent adDisplayingEvent);
    boolean hasAd();
}
