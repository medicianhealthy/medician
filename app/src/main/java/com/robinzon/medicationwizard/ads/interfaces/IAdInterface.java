package com.robinzon.medicationwizard.ads.interfaces;


import android.app.Activity;

import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.EAdType;

public interface IAdInterface {
    String getAdUnitId();
    // Loading
    boolean shouldLoad();
    void load();
    void setIsInLoadingProgress(boolean isLoading);
    boolean isInLoadingProgress();
    boolean isLoaded();
    void setIsLoaded(final boolean isLoaded);
    void handleReloaderOnFaild();
    void handleReloaderOnSuccess();

    // Showing
    boolean canShow();
    void show();
    boolean isShowing();
    void setIsShowing(boolean isShowing);

    //AdCallBacks
    void handleAdCallBacks(final EAdCallBacks adCallback);
    //Life Cycle
    void onResume();
    void onPause();
    void onDestroy();
    void onCreate();

    //Expiration
    boolean isExpired();
    int getExpirationTimeInMinutes();

    Activity getActivity();
    AdsManager getAdsManager();
    EAdType getAdType();
}
