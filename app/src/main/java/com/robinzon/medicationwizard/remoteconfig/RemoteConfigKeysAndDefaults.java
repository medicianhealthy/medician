package com.robinzon.medicationwizard.remoteconfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RemoteConfigKeysAndDefaults {
    public static final String NUM_OF_MEDS_TO_SHOW_RV = "num_of_meds_to_show_rv";

    public static final Map<String, Object> VALUES = Collections.unmodifiableMap(
            new HashMap<String, Object>() {{
                put(NUM_OF_MEDS_TO_SHOW_RV,3);
            }}
    );
}
