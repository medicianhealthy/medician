package com.robinzon.medicationwizard.ads.interfaces;

public interface IFullScreenAd extends IAdInterface {
    void setIsExpired(boolean isExpired);
    boolean isExpired();
    void setLoadTime(long loadTime);
}
