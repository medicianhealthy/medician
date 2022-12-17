package com.robinzon.medicationwizard.ads.rootclasses;

import android.app.Activity;

import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.EAdType;
import com.robinzon.medicationwizard.ads.interfaces.IAdsLifeCycleCallBack;
import com.robinzon.medicationwizard.ads.interfaces.IRewardedVideo;

public abstract class RewardedVideo extends FullScreenAd implements IRewardedVideo {
    protected int mRewardAmount;
    private boolean mIsSkipped;


    protected RewardedVideo(Activity activity, EAdPlacement placement) {
        super(activity, placement);
    }

    @Override
    public EAdType getAdType() {
        return EAdType.REWARDED_VIDEO;
    }

    @Override
    public void handleAdCallBacks(EAdCallBacks adCallback, IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        super.handleAdCallBacks(adCallback, adsLifeCycleCallBack);
        switch (adCallback) {
            case SHOWN:
                setIsSkipped(true);
                break;
            case REWARDED:
                setIsSkipped(false);
                break;
            case LOADED:
                stampLoadTime();
                break;
        }
    }

    @Override
    public void setIsSkipped(boolean isSkipped) {
        mIsSkipped = isSkipped;
    }

    @Override
    public boolean isSkipped() {
        return mIsSkipped;
    }

    @Override
    public void setRewardAmount(int amount) {
        mRewardAmount = amount;
    }

    @Override
    public int getRewardAmount() {
        return mRewardAmount;
    }

    @Override
    public boolean hasCoolDownPassedSinceLastImpression() {
        return false;
    }

}
