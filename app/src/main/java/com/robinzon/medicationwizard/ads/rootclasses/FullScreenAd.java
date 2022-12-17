package com.robinzon.medicationwizard.ads.rootclasses;

import android.app.Activity;

import com.google.android.gms.ads.AdError;
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.interfaces.IAdsLifeCycleCallBack;
import com.robinzon.medicationwizard.ads.interfaces.IFullScreenAd;

public abstract class FullScreenAd extends Ad implements IFullScreenAd {

    private long mLastSuccessfulLoadTimeStamp;
    private int mExpirationTimeInMinutes = 60;

    protected FullScreenAd(Activity act, EAdPlacement placement) {
        super(act, placement);
    }

    @Override
    public boolean isExpired() {
        final int expirationInMinutes = getExpirationTimeInMinutes();
        final long currentTimeInMillis = System.currentTimeMillis();
        final int deltaInMinutes = (int) ((currentTimeInMillis - mLastSuccessfulLoadTimeStamp) / 1000 / 60);
        return deltaInMinutes > expirationInMinutes;
    }

    @Override
    public int getExpirationTimeInMinutes() {
        return this.mExpirationTimeInMinutes;
    }

    @Override
    public void stampLoadTime() {
        mLastSuccessfulLoadTimeStamp = System.currentTimeMillis();
    }

    @Override
    public boolean shouldLoad() {
        return super.shouldLoad() || (isLoaded() && isExpired());
    }

    @Override
    public void handleAdCallBacks(EAdCallBacks adCallback, IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        handleAdCallBacks(adCallback, adsLifeCycleCallBack, null);
    }

    @Override
    public void handleAdCallBacks(EAdCallBacks adCallback, IAdsLifeCycleCallBack adsLifeCycleCallBack, AdError adError) {
        super.handleAdCallBacks(adCallback, adsLifeCycleCallBack, adError);
        if (adCallback == EAdCallBacks.LOADED) {
            stampLoadTime();
        }
    }

    @Override
    public boolean canShow() {
        return !isExpired() && super.canShow();
    }

}
