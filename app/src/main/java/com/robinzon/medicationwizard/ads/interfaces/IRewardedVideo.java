package com.robinzon.medicationwizard.ads.interfaces;

public interface IRewardedVideo extends IFullScreenAd {
    public void setRewardAmount(final int amount);
    public int getRewardAmount();
    void setIsSkipped(boolean isSkipped);
    boolean isSkipped();
}
