package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;
import android.content.Context;

import com.robinzon.medicationwizard.ads.IAdDisplayingEvent;
import com.robinzon.medicationwizard.ads.IAdLoadingEvents;

public interface IRewardedVideo extends IAd{
    public void create(Activity mainActivity, final int adUnitIdResourceId);
    public void load(final Activity activity, final IAdLoadingEvents adLoadingEvents);
    public void show(final Activity activity, final IAdDisplayingEvent adDisplayingEvent);
    public void setRewardAmount(final int amount);
    public int getRewardAmount();
}
