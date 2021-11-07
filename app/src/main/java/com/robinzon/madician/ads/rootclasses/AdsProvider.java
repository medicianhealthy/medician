
package com.robinzon.madician.ads.rootclasses;

import com.robinzon.madician.ads.interfaces.AdsProviderActions;
import com.robinzon.madician.ads.interfaces.BannerAdActions;
import com.robinzon.madician.ads.interfaces.InterstitialAdActions;
import com.robinzon.madician.ads.interfaces.RewardedVideoAdActions;

public abstract class AdsProvider implements AdsProviderActions {

    public static boolean USE_TEST_ADS_FOR_BANNER = true;
    public static boolean USE_TEST_ADS_FOR_INTERSTITIAL = true;
    public static boolean USE_TEST_ADS_FOR_RV = true;
    public static boolean USE_TEST_ADS_FOR_REWARDED_INTERSTITIAL = true;




}
