package com.robinzon.medicationwizard.ads.interfaces;


import android.app.Activity;

import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.EAdType;

public interface IAd {
    String getAdUnitId();
    String getPlacement();
    // Loading
    boolean shouldLoad(); //Both
    void load(); //Implementation
    void setIsInLoadingProgress(boolean isLoading);
    boolean isInLoadingProgress();
    boolean isLoaded();
    void setIsLoaded(final boolean isLoaded);

    // Showing
    boolean canShow(); //Both
    void show();//Implementation
    boolean isShowing();
    void setIsShowing(boolean isShowing);

    //AdCallBacks
    void handleAdCallBacks(final EAdCallBacks adCallback);//Both
    //Life Cycle
    void onResume();//Implementation
    void onPause();//Implementation
    void onDestroy();//Implementation
    void onCreate();//Implementation

    Activity getActivity();
    AdsManager getAdsManager();
    EAdType getAdType();
}
