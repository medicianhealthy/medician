package com.robinzon.madician.ads.rootclasses;

import com.robinzon.madician.ads.interfaces.RewardedVideoAdActions;

public abstract class RewardedVideoAd extends Ad implements RewardedVideoAdActions {
    protected int mRewardAmount;

    public int getRewardAmount() {
        return mRewardAmount;
    }

    public void setRewardAmount(int mRewardAmount) {
        this.mRewardAmount = mRewardAmount;
    }

    @Override
    public String getClassName() {
        return "{RewardedVideoAd}";
    }
}
