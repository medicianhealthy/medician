package com.robinzon.medicationwizard.utils;

import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuperClass;

public class AppConfig extends MedicationWizardSuperClass {
    public static final long FIREBASE_FETCH_TIMEOUT_SECONDS = 3;
    public static final long FIREBASE_FETCH_INTERVAL_HOURS = 12;

    @Override
    public String getClassName() {
        return "{AppConfig}";
    }
}
