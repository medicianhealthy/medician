package com.robinzon.medicationwizard.ads.rootclasses;

public abstract class BannerAd extends Ad {
   protected int mRefreshRate;

    public int getRefreshRate() {
        return mRefreshRate;
    }

    public void setRefreshRate(int mRefreshRate) {
        this.mRefreshRate = mRefreshRate;
    }

    @Override
    public String getClassName() {
        return "{BannerAd}";
    }
}
