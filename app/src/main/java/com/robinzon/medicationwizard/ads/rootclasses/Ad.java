package com.robinzon.medicationwizard.ads.rootclasses;

import com.robinzon.medicationwizard.ads.IAdLoadingEvents;
import com.robinzon.medicationwizard.ads.interfaces.IAd;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Ad extends MedicationWizardSuper implements IAd {
    protected String mAdUnitId;
    protected final AtomicBoolean mIsLoaded = new AtomicBoolean(false);
    protected final AtomicBoolean mIsInLoadingProgress = new AtomicBoolean(false);
    protected final AtomicBoolean mIsShowing = new AtomicBoolean(false);
    protected IAdLoadingEvents mLoadingEventsListener;
    protected int mRetryAttempts;

    protected String getAdUnitId(){
        return mAdUnitId;
    }
    protected void setAdUnitId(final String adUnitId){
        mAdUnitId = adUnitId;
    }
}
