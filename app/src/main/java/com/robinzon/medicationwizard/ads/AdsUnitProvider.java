package com.robinzon.medicationwizard.ads;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.rootclasses.EAdPlacement;
import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;

import java.util.HashMap;
import java.util.Map;

public class AdsUnitProvider extends MedicationWizardSuper {

    private static final Map<EAdType, Map<EAdPlacement, Integer>> AD_UNITS = new HashMap<>();

    static {
        AD_UNITS.put(EAdType.BANNER, new HashMap<>() {{
            put(EAdPlacement.BANNER_AD_PLACEMENT_EDIT_MED, 0);
            put(EAdPlacement.BANNER_AD_PLACEMENT_MAIN, 0);
            put(EAdPlacement.BANNER_AD_PLACEMENT_SETTINGS, 0);
            put(EAdPlacement.BANNER_AD_PLACEMENT_AD_MED, 0);
            put(EAdPlacement.BANNER_AD_PLACEMENT_TEST, R.string.admob_banner_id_test);
        }});

        AD_UNITS.put(EAdType.INTERSTITIAL, new HashMap<>() {{
            put(EAdPlacement.INTERSTITIAL_AD_PLACEMENT_EDIT_MED, 0);
            put(EAdPlacement.INTERSTITIAL_AD_PLACEMENT_ADD_MED, 0);
            put(EAdPlacement.INTERSTITIAL_AD_PLACEMENT_SETTINGS_CHANGE, 0);
            put(EAdPlacement.INTERSTITIAL_AD_PLACEMENT_TEST, R.string.admob_interstitial_id_test);
        }});

        AD_UNITS.put(EAdType.REWARDED_VIDEO, new HashMap<>() {{
            put(EAdPlacement.REWARDED_VIDEO_AD_PLACEMENT_MED_COLOR, 0);
            put(EAdPlacement.REWARDED_VIDEO_AD_PLACEMENT_TEST, R.string.admob_rv_id_test);
        }});

        AD_UNITS.put(EAdType.APP_OPEN, new HashMap<>() {{
            put(EAdPlacement.APP_OPEN_MAIN, 0);
            put(EAdPlacement.APP_OPEN_TEST, R.string.admob_app_open_id_test);
        }});
    }

    @Nullable public static String getAdUnit(@NonNull final Context context, @NonNull final EAdType adType, @NonNull final EAdPlacement placement) {
        Map<EAdPlacement, Integer> adsUnitMapForAdType = AD_UNITS.get(adType);
        if (null != adsUnitMapForAdType && !adsUnitMapForAdType.isEmpty()) {
            final Integer adUnitResourceId = adsUnitMapForAdType.get(placement);
            if (null != adUnitResourceId) {
                return context.getString(adUnitResourceId);
            }
        }
        return null;
    }
}