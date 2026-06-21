package com.robinzon.medicationwizard.remoteconfig;

import java.util.Map;

public class RemoteConfigKeysAndDefaults {
    public static final String NUM_OF_MEDS_TO_SHOW_RV = "num_of_meds_to_show_rv";
    public static final String AD_SHOULD_SHOW_APP_OPEN = "ad_should_show_app_open";
    public static final String AD_INTERSTITIAL_COOL_DOWN_SECONDS = "ad_interstitial_cool_down";
    
    // New Keys
    public static final String MIN_SESSIONS_INTERSTITIAL = "min_sessions_interstitial";
    public static final String MIN_APP_TIME_INTERSTITIAL_MINS = "min_app_time_interstitial_mins";
    public static final String MIN_APP_TIME_BANNER_MINS = "min_app_time_banner_mins";
    public static final String MAGIC_PASS_DURATION_HOURS = "magic_pass_duration_hours";
    public static final String HISTORY_RETENTION_DAYS = "history_retention_days";
    public static final String EARLY_TAKE_THRESHOLD_MINS = "early_take_threshold_mins";
    public static final String LATE_TAKE_THRESHOLD_MINS = "late_take_threshold_mins";

    public static final Map<String, Object> VALUES = Map.of(
            NUM_OF_MEDS_TO_SHOW_RV, 3,
            AD_SHOULD_SHOW_APP_OPEN, Boolean.FALSE,
            AD_INTERSTITIAL_COOL_DOWN_SECONDS, 60,
            MIN_SESSIONS_INTERSTITIAL, 2,
            MIN_APP_TIME_INTERSTITIAL_MINS, 2,
            MIN_APP_TIME_BANNER_MINS, 1,
            MAGIC_PASS_DURATION_HOURS, 8,
            HISTORY_RETENTION_DAYS, 30,
            EARLY_TAKE_THRESHOLD_MINS, 60,
            LATE_TAKE_THRESHOLD_MINS, 180
    );
}
