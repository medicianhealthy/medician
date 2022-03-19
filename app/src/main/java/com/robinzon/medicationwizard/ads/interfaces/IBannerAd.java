package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;

public interface IBannerAd extends IAd{
    void createFromLayout(final Activity activity, final int viewId);
    int getBannerHeightInPixels(final Activity activity);
}
