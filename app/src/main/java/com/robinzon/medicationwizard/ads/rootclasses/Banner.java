package com.robinzon.medicationwizard.ads.rootclasses;

import android.app.Activity;

import com.robinzon.medicationwizard.ads.EAdType;
import com.robinzon.medicationwizard.ads.interfaces.IBanner;

public abstract class Banner extends Ad implements IBanner {
    private long mLastSuccessfulLoadTimeStamp;
    private int mExpirationTimeInMinutes;

    protected Banner(final Activity activity, final String placement) {
        super(activity, placement);
    }

    @Override
    public EAdType getAdType() {
        return EAdType.BANNER;
    }



}
