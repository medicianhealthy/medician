package com.robinzon.medicationwizard.ads.interfaces;


import android.app.Activity;

import com.google.android.gms.ads.AdError;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.EAdType;
import com.robinzon.medicationwizard.ads.rootclasses.EAdPlacement;

public interface IAd {
    String getAdUnitId(); //to be implemented on root class - Ad
    EAdPlacement getPlacement();//to be implemented on root class - Ad

    // Loading
    boolean shouldLoad(); //to be implemented on root class - Ad
    void load(); //Implementation
    void load(IAdsLifeCycleCallBack adsLifeCycleCallBack); //Implementation
    void setIsInLoadingProgress(boolean isLoading);//to be implemented on root class - Ad
    boolean isInLoadingProgress();//to be implemented on root class - Ad
    boolean isLoaded();//to be implemented on root class - Ad
    void setIsLoaded(final boolean isLoaded);//to be implemented on root class - Ad

    // Showing
    boolean canShow(); //Both
    void show();//Implementation
    void show(IAdsLifeCycleCallBack adsLifeCycleCallBack);
    boolean isShowing();//to be implemented on root class - Ad/to be implemented on root class - Ad
    void setIsShowing(boolean isShowing);

    //AdCallBacks
    void handleAdCallBacks(final EAdCallBacks adCallback, IAdsLifeCycleCallBack adsLifeCycleCallBack, AdError adError);//Both
    void handleAdCallBacks(final EAdCallBacks adCallback, IAdsLifeCycleCallBack adsLifeCycleCallBack);//Both
    //Life Cycle
    void onResume();//Implementation
    void onPause();//Implementation
    void onDestroy();//Implementation
    void onCreate();//Implementation

    Activity getActivity();
    AdsManager getAdsManager();
    EAdType getAdType();

    Object getAdCoreObject();

    boolean hasCoolDownPassedSinceLastImpression();
}
