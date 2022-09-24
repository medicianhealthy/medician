package com.robinzon.medicationwizard.ads.interfaces;

import android.app.Activity;

public interface IBannerAd extends IAdInterface {
    void createFromLayout(final Activity activity, final int viewId);
    int getBannerHeightInPixels(final Activity activity);
}
