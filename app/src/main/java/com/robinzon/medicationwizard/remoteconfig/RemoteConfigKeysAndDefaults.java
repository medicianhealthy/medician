package com.robinzon.medicationwizard.remoteconfig;

import java.util.Map;

public class RemoteConfigKeysAndDefaults {
    public static final String NUM_OF_MEDS_TO_SHOW_RV = "num_of_meds_to_show_rv";
    public static final String AD_SHOULD_SHOW_APP_OPEN = "ad_should_show_app_open";
    public static final String AD_INTERSTITIAL_COOL_DOWN_SECONDS = "ad_interstitial_cool_down";

    public static final Map<String, Object> VALUES = Map.of(
            NUM_OF_MEDS_TO_SHOW_RV, 3,
            AD_SHOULD_SHOW_APP_OPEN, Boolean.FALSE,
            AD_INTERSTITIAL_COOL_DOWN_SECONDS, 60);
}
