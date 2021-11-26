package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;
import android.content.Context;

public interface IAd {
    boolean hasAd();
    boolean isLoaded();
    public void setIsLoaded(final boolean isLoaded);
    boolean isShowing();
    void setIsShowing(boolean isShowing);
    String getAdUnitId();
    void setAdUnitId(final String adUnitId);
    void callOnResume(final Activity activity);
    void callOnPause(final Activity activity);
    void callOnDestroy(final Activity activity);
    void callOnCreate(final Activity activity);

}
