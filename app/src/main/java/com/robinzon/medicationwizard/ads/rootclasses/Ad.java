package com.robinzon.medicationwizard.ads.rootclasses;

public abstract class Ad extends MedicationWizardSuperClass {
    protected String mAdUnitId;
    protected boolean mIsLoaded;
    protected boolean mIsShowing;
    protected boolean mIsExpired;

    @Override
    public String getClassName() {
        return "{Ad}";
    }

    public void setClassName(String mClassName) {
        this.mClassName = mClassName;
    }

    protected String mClassName;

    public String getAdUnitId() {
        return mAdUnitId;
    }

    public void setAdUnitId(String mAdUnitId) {
        this.mAdUnitId = mAdUnitId;
    }

    public boolean isLoaded() {
        return mIsLoaded;
    }

    public void setIsLoaded(boolean mIsLoaded) {
        this.mIsLoaded = mIsLoaded;
    }

    public boolean isShowing() {
        return mIsShowing;
    }

    public void setIsShowing(boolean mIsShowing) {
        this.mIsShowing = mIsShowing;
    }

    public boolean isIsExpired() {
        return mIsExpired;
    }

    public void setIsExpired(boolean mIsExpired) {
        this.mIsExpired = mIsExpired;
    }
}
