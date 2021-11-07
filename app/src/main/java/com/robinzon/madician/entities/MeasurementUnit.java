package com.robinzon.madician.entities;

public enum MeasurementUnit {
    GRAMS ("g"),
    MILLIGRAM ("mg"),
    INTERNATIONAL_UNITS ("IU"),
    MICRO_GRAM("mcg"),
    MICRO_GRAM_PER_MILLILITER ("mcg/ml"),
    MILLI_EQUIVALENT("mEq"),
    MILLILITER ("mL"),
    PERCENTAGE ("%");

    private final String mName;

    MeasurementUnit(final String name) {
        mName = name;
    }

    public String getName(){
        return mName;
    }
}
