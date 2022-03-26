package com.robinzon.medicationwizard.ads.rootclasses;

import com.robinzon.medicationwizard.ads.IAdLoadingEvents;
import com.robinzon.medicationwizard.ads.adsproviders.admob.AdMob;
import com.robinzon.medicationwizard.ads.interfaces.IAd;
import com.robinzon.medicationwizard.utils.TimeInterval;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Ad extends MedicationWizardSuper implements IAd {
    protected String mAdUnitId;
    protected final AtomicBoolean mIsLoaded = new AtomicBoolean(false);
    protected final AtomicBoolean mIsInLoadingProgress = new AtomicBoolean(false);
    protected final AtomicBoolean mIsShowing = new AtomicBoolean(false);
    protected IAdLoadingEvents mLoadingEventsListener;
    private int mRetryAttempts;
    private long mLastSuccessfulLoadTimeStamp;

    protected String getAdUnitId(){
        return mAdUnitId;
    }
    protected void setAdUnitId(final String adUnitId){
        mAdUnitId = adUnitId;
    }

    public boolean isExpired(){
        final long adsExpirationTimeMillis = TimeInterval.MilliSeconds.getFromMinutes(AdMob.ADS_EXPIRATION_MINUTES);
        final long delta = System.currentTimeMillis() - getLastSuccessfulLoadTimeStamp();
        return delta > adsExpirationTimeMillis;
    }

    public void setLastSuccessfulLoadTimeStamp(){
        mLastSuccessfulLoadTimeStamp = System.currentTimeMillis();
    }

    public long getLastSuccessfulLoadTimeStamp(){
        return mLastSuccessfulLoadTimeStamp;
    }

    public void markLoadFailAttempt(){
       mRetryAttempts++;
    }

    public void setRetryAttemptsToZero(){
        mRetryAttempts = 0;
    }

    public int getRetryAttempts(){
        return mRetryAttempts;
    }
}
