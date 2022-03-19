package com.robinzon.medicationwizard.ads.rootclasses;

import com.robinzon.medicationwizard.ads.interfaces.IAdsProvider;
import com.robinzon.medicationwizard.ads.interfaces.IBannerAd;
import com.robinzon.medicationwizard.ads.interfaces.IInterstitialAd;
import com.robinzon.medicationwizard.ads.interfaces.IRewardedVideo;

;

public abstract class AdProvider extends MedicationWizardSuper implements IAdsProvider {
    protected boolean mIsSdkInitialized;
    protected IBannerAd mBanner;
    protected IInterstitialAd mInterstitial;
    protected IRewardedVideo mRewardedVideo;
}
