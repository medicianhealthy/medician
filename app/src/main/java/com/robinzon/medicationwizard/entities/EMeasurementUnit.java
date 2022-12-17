package com.robinzon.medicationwizard.entities;

public enum EMeasurementUnit {
    GRAMS("g"),
    MILLIGRAM("mg"),
    INTERNATIONAL_UNITS("IU"),
    MICRO_GRAM("mcg"),
    MICRO_GRAM_PER_MILLILITER("mcg/ml"),
    MILLI_EQUIVALENT("mEq"),
    MILLILITER("mL"),
    PERCENTAGE("%");

    private final String mName;

    EMeasurementUnit(final String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }
}
