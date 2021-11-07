package com.robinzon.madician.ads.interfaces;

import android.app.Activity;

import com.robinzon.madician.ads.AdDisplayingEvent;
import com.robinzon.madician.ads.AdLoadingEvents;

public interface RewardedVideoAdActions {
    public void create(Activity mainActivity, final int adUnitIdResourceId);
    public void load(final Activity activity, final AdLoadingEvents adLoadingEvents);
    public void show(final Activity activity, final AdDisplayingEvent adDisplayingEvent);
    boolean hasAd();
}
