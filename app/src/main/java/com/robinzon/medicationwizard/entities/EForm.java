package com.robinzon.medicationwizard.entities;

import com.robinzon.medicationwizard.R;

/**
 * Enumeration of supported physical forms for medications.
 * <p>
 * This enum is used to determine which icon to display in lists and
 * how to pluralize the dose amount in summaries (e.g., "1 Pill" vs "2 Pills").
 * </p>
 */
@SuppressWarnings("unused")
public enum EForm {
    /**
     * Solid oral medication.
     */
    Pill(R.string.form_pill),
    /**
     * Liquid oral or topical medication.
     */
    Solution(R.string.form_solution),
    /**
     * Intravenous or intramuscular medication.
     */
    Injection(R.string.form_injection),
    /**
     * Dry medication to be dissolved or inhaled.
     */
    Powder(R.string.form_powder),
    /**
     * Eye, ear, or oral drops.
     */
    Drops(R.string.form_drops),
    /**
     * Metered-dose inhaler or spray.
     */
    Inhaler(R.string.form_inhaler),
    /**
     * Fallback for unconventional medication forms.
     */
    Other(R.string.form_other);

    private final int labelResId;

    EForm(int labelResId) {
        this.labelResId = labelResId;
    }

    public int getLabelResId() {
        return labelResId;
    }

    public String getLabel(android.content.Context context) {
        return context.getString(labelResId);
    }
}