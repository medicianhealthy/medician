package com.robinzon.medicationwizard.ads.interfaces;

import com.google.android.gms.ads.AdError;
import com.robinzon.medicationwizard.ads.EAdCallBacks;

public interface IAdsLifeCycleCallBack {
    void onInterstitialLifeCycleStageChanged(final IAd ad, final EAdCallBacks adCallBack, final AdError adError);
}
