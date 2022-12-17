package com.robinzon.medicationwizard.ads;

import com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager;

public class AdsGracePeriod {
    final int mBannerGracePeriodMinutes;
    final int mInterGracePeriodMinutes;
    final int mRvGracePeriodMinutes;

    public AdsGracePeriod() {
        mBannerGracePeriodMinutes = RemoteConfigManager.getInstance().getIntValue(AdsManager.RCKEY_ADS_TIMER_BANNER_GRACE_MINUTES);
        mInterGracePeriodMinutes = RemoteConfigManager.getInstance().getIntValue(AdsManager.RCKEY_ADS_TIMER_INTER_GRACE_MINUTES);
        mRvGracePeriodMinutes = RemoteConfigManager.getInstance().getIntValue(AdsManager.RCKEY_ADS_TIMER_RV_GRACE_MINUTES);
    }

    public int getBannerGracePeriodMinutes() {
        return mBannerGracePeriodMinutes;
    }

    public int getInterGracePeriodMinutes() {
        return mInterGracePeriodMinutes;
    }

    public int getRvGracePeriodMinutes() {
        return mRvGracePeriodMinutes;
    }
}
