package com.robinzon.medicationwizard.ads.rootclasses;

import android.app.Activity;

public abstract class PartialScreenAd extends Ad{

    public PartialScreenAd(Activity mActivity, EAdPlacement placement) {
        super(mActivity, placement);
    }
}
