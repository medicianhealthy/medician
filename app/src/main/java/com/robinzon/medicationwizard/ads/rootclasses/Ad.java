package com.robinzon.medicationwizard.ads.rootclasses;

import android.app.Activity;
import android.text.TextUtils;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.AdsUnitProvider;
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.EAdType;
import com.robinzon.medicationwizard.ads.interfaces.IAd;

import java.lang.ref.WeakReference;

public abstract class Ad extends MedicationWizardSuper implements IAd {

    private final Activity mActivity;
    private WeakReference<String> mAdUnitId = new WeakReference<>(null);
    private boolean mIsLoaded;
    private boolean mIsInLoadingProgress;
    private boolean mIsShowing;
    private final String mPlacement;

    protected Ad(Activity mActivity, String placement) {
        this.mActivity = mActivity;
        this.mPlacement = placement;
    }

    @Override
    public String getAdUnitId() {
       if (null == mAdUnitId.get()){
           final String adUnit = AdsUnitProvider.getAdUnit(getActivity(), getAdType(), getPlacement());
           if (!TextUtils.isEmpty(adUnit)) {
               mAdUnitId = new WeakReference<>(adUnit);
           }
       }
       return mAdUnitId.get();
    }

    @Override
    public void setIsInLoadingProgress(boolean isLoading) {
        mIsInLoadingProgress = isLoading;
    }

    @Override
    public boolean isInLoadingProgress() {
        return mIsInLoadingProgress;
    }

    @Override
    public boolean isLoaded() {
        return mIsLoaded;
    }

    @Override
    public void setIsLoaded(boolean isLoaded) {
        mIsLoaded = isLoaded;
    }

    @Override
    public boolean canShow() {
        return !isShowing() && isLoaded();
    }

    @Override
    public boolean isShowing() {
        return mIsShowing;
    }

    @Override
    public void setIsShowing(boolean isShowing) {
        mIsShowing = isShowing;
    }

    @Override
    public boolean shouldLoad() {
        return !isLoaded() && !isInLoadingProgress();
    }

    @Override
    public void handleAdCallBacks(EAdCallBacks adCallback) {
        switch (adCallback) {
            case STARTING_TO_LOAD:
                setIsInLoadingProgress(true);
                break;
            case LOADED:
                setIsLoaded(true);
                setIsInLoadingProgress(false);
                break;
            case FAILED_TO_LOAD:
                setIsLoaded(false);
                setIsInLoadingProgress(false);
                break;
            case SHOWN:
                setIsShowing(true);
                if (EAdType.BANNER != getAdType()) {
                    setIsLoaded(false);
                    load();
                }
                break;
            case FAILED_TO_SHOW:
            case DISMISSED:
                setIsShowing(false);
                break;
            case CLICKED:
                break;
            default:
                break;
        }
    }


    @Override
    public Activity getActivity() {
        return this.mActivity;
    }

    @Override
    public AdsManager getAdsManager() {
        return ((MainActivity) getActivity()).getAdsManager();
    }

    @Override
    public String getPlacement() {
        return mPlacement;
    }

}
