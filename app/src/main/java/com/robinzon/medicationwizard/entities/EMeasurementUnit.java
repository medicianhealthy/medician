package com.robinzon.medicationwizard.entities;

/**
 * Enumeration of standard measurement units for medication strength and volume.
 * <p>
 * This enum is used to format the "Strength" text on medication cards (e.g., "150 mg") 
 * to ensure consistency and professional display of medical data.
 * </p>
 */
@SuppressWarnings("unused")
public enum EMeasurementUnit {
    /** Grams. */
    Grams("g"),
    /** Milligrams. */
    Milligram("mg"),
    /** International Units (commonly used for vitamins). */
    IU("IU"),
    /** Micrograms. */
    Microgram("mcg"),
    /** Milliliters. */
    Milliliter("mL"),
    /** Percentage concentration (commonly used for topical solutions). */
    Percentage("%");

    private final String mName;

    EMeasurementUnit(final String name) {
        mName = name;
    }

    /**
     * @return The short symbol for the unit (e.g., "mg").
     */
    public String getName() {
        return mName;
    }
}