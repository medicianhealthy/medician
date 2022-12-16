package com.robinzon.medicationwizard.ads.rootclasses;

import android.app.Activity;

import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.interfaces.IAdsLifeCycleCallBack;
import com.robinzon.medicationwizard.ads.interfaces.IFullScreenAd;

import java.util.List;

public abstract class FullScreenAd extends Ad implements IFullScreenAd {

    private long mLastSuccessfulLoadTimeStamp;
    private int mExpirationTimeInMinutes;

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
        super.handleAdCallBacks(adCallback, adsLifeCycleCallBack);
        if (adCallback == EAdCallBacks.LOADED) {
            stampLoadTime();
        }

    }

    @Override
    public boolean canShow() {
        return !isExpired() && super.canShow();
    }



    @Override
    protected List<String> getLogTags() {
        final List<String> thisLogTags = super.getLogTags();
        thisLogTags.add(getClass().getSimpleName());
        return thisLogTags;
    }
}
