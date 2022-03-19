package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;
import android.content.Context;

import com.robinzon.medicationwizard.ads.IAdDisplayingEvent;
import com.robinzon.medicationwizard.ads.IAdLoadingEvents;

public interface IRewardedVideo extends IAd{
    public void setRewardAmount(final int amount);
    public int getRewardAmount();
}
