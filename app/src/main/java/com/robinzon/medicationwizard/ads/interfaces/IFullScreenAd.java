package com.robinzon.medicationwizard.ads.interfaces;

public interface IFullScreenAd extends IAd {
    boolean isExpired();
    void stampLoadTime();
    int getExpirationTimeInMinutes();
}
