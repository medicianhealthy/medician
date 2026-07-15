package com.robinzon.medicationwizard.entities;

import android.content.Context;

import androidx.annotation.StringRes;

import com.robinzon.medicationwizard.R;

/**
 * Enumeration of standard measurement units for medication strength and volume.
 * <p>
 * This enum is used to format the "Strength" text on medication cards (e.g., "150 mg")
 * to ensure consistency and professional display of medical data.
 * </p>
 */
@SuppressWarnings("unused")
public enum EMeasurementUnit {
    /**
     * Milligrams.
     */
    Milligram("mg", R.string.unit_mg),
    /**
     * International Units (commonly used for vitamins).
     */
    IU("IU", R.string.unit_iu),
    /**
     * Micrograms.
     */
    Microgram("mcg", R.string.unit_mcg),
    /**
     * Milliliters.
     */
    Milliliter("mL", R.string.unit_ml),
    /**
     * Grams.
     */
    Grams("g", R.string.unit_grams),
    /**
     * Percentage concentration (commonly used for topical solutions).
     */
    Percentage("%", R.string.unit_percentage);

    private final String mName;
    private final int mLabelResId;

    EMeasurementUnit(final String name, @StringRes final int labelResId) {
        mName = name;
        mLabelResId = labelResId;
    }

    /**
     * Finds a unit by its internal name and returns its localized label.
     */
    public static String getLabelByName(Context context, String name) {
        if (name == null) return "";
        for (EMeasurementUnit unit : values()) {
            if (unit.getName().equalsIgnoreCase(name)) {
                return unit.getLabel(context);
            }
        }
        return name;
    }

    /**
     * @return The short symbol for the unit (e.g., "mg").
     */
    public String getName() {
        return mName;
    }

    /**
     * @return The localized label for the unit.
     */
    public String getLabel(Context context) {
        return context.getString(mLabelResId);
    }
}
