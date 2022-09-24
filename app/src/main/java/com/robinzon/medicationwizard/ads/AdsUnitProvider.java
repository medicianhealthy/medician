package com.robinzon.medicationwizard.ads;

import android.content.Context;
import android.text.TextUtils;

import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;

import java.util.HashMap;
import java.util.Map;

public class AdsUnitProvider extends MedicationWizardSuper {

    public static final String BANNER_AD_PLACEMENT_PREFIX = "banner_ad_placement_";
    public static final String BANNER_AD_PLACEMENT_MAIN = BANNER_AD_PLACEMENT_PREFIX.concat("main");
    public static final String BANNER_AD_PLACEMENT_SETTINGS = BANNER_AD_PLACEMENT_PREFIX.concat("settings");
    public static final String BANNER_AD_PLACEMENT_AD_MED = BANNER_AD_PLACEMENT_PREFIX.concat("ad_med");
    public static final String BANNER_AD_PLACEMENT_EDIT_MED = BANNER_AD_PLACEMENT_PREFIX.concat("edit_med");
    public static final String BANNER_AD_PLACEMENT_TEST = BANNER_AD_PLACEMENT_PREFIX.concat("test");
    
    public static final String INTERSTITIAL_AD_PLACEMENT_PREFIX = "interstitial_ad_placement_";
    public static final String INTERSTITIAL_AD_PLACEMENT_EDIT_MED = INTERSTITIAL_AD_PLACEMENT_PREFIX.concat("edit_med");
    public static final String INTERSTITIAL_AD_PLACEMENT_ADD_MED = INTERSTITIAL_AD_PLACEMENT_PREFIX.concat("add_med");
    public static final String INTERSTITIAL_AD_PLACEMENT_SETTINGS_CHANGE = INTERSTITIAL_AD_PLACEMENT_PREFIX.concat("settings_change");
    public static final String INTERSTITIAL_AD_PLACEMENT_TEST = INTERSTITIAL_AD_PLACEMENT_PREFIX.concat("test");

    public static final String REWARDED_VIDEO_AD_PLACEMENT_PREFIX = "rewarded_video_ad_placement_";
    public static final String REWARDED_VIDEO_AD_PLACEMENT_MED_COLOR = REWARDED_VIDEO_AD_PLACEMENT_PREFIX.concat("med_color");
    public static final String REWARDED_VIDEO_AD_PLACEMENT_TEST = REWARDED_VIDEO_AD_PLACEMENT_PREFIX.concat("test");

    public static final String APP_OPEN_AD_PLACEMENT_PREFIX = "app_open_placement_";
    public static final String APP_OPEN_MAIN = APP_OPEN_AD_PLACEMENT_PREFIX.concat("app_open");
    public static final String APP_OPEN_TEST = APP_OPEN_AD_PLACEMENT_PREFIX.concat("test");
   
    
    
    private static final Map<EAdType, Map<String, Integer>> AD_UNITS = new HashMap<>();

    static {
        AD_UNITS.put(EAdType.BANNER, new HashMap<String, Integer>() {{
            put(BANNER_AD_PLACEMENT_EDIT_MED, 0);
            put(BANNER_AD_PLACEMENT_MAIN, 0);
            put(BANNER_AD_PLACEMENT_SETTINGS, 0);
            put(BANNER_AD_PLACEMENT_AD_MED, 0);
            put(BANNER_AD_PLACEMENT_TEST, R.string.admob_banner_id_test);
        }});

        AD_UNITS.put(EAdType.INTERSTITIAL, new HashMap<String, Integer>() {{
            put(INTERSTITIAL_AD_PLACEMENT_EDIT_MED, 0);
            put(INTERSTITIAL_AD_PLACEMENT_ADD_MED, 0);
            put(INTERSTITIAL_AD_PLACEMENT_SETTINGS_CHANGE, 0);
            put(INTERSTITIAL_AD_PLACEMENT_TEST, R.string.admob_interstitial_id_test);
        }});

        AD_UNITS.put(EAdType.REWARDED_VIDEO, new HashMap<String, Integer>() {{
            put(REWARDED_VIDEO_AD_PLACEMENT_MED_COLOR, 0);
            put(REWARDED_VIDEO_AD_PLACEMENT_TEST, R.string.admob_rv_id_test);
        }});

        AD_UNITS.put(EAdType.APP_OPEN, new HashMap<String, Integer>() {{
            put(APP_OPEN_MAIN, 0);
            put(APP_OPEN_TEST, R.string.admob_app_open_id_test);
        }});
    }

    public static String getAdUnit(final Context context, final EAdType adType, final String placement) {
        if (null != context || null != adType || !TextUtils.isEmpty(placement)) {
            Map<String, Integer> adUitForPlacement = AD_UNITS.get(adType);
            if (null != adUitForPlacement) {
                final Integer adUnitResourceId = adUitForPlacement.get(placement);
                if (null != adUnitResourceId && null != context) {
                    return context.getString(adUnitResourceId);
                }
            }
        }
        return null;
    }
}