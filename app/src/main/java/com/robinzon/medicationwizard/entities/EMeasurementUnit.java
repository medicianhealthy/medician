package com.robinzon.medicationwizard.entities;

@SuppressWarnings("unused")
public enum EMeasurementUnit {
    Grams("g"),
    Milligram("mg"),
    IU("IU"),
    Microgram("mcg"),
    Milliliter("mL"),
    Percentage("%");

    private final String mName;

    EMeasurementUnit(final String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }
}
