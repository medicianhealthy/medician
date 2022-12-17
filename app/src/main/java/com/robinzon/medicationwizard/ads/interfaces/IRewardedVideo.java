package com.robinzon.medicationwizard.ads.interfaces;

public interface IRewardedVideo extends IFullScreenAd {
    void setRewardAmount(final int amount);
    int getRewardAmount();
    void setIsSkipped(boolean isSkipped);
    boolean isSkipped();
}
