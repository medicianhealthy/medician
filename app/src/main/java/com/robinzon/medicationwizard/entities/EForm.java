package com.robinzon.medicationwizard.entities;

/**
 * Enumeration of supported physical forms for medications.
 * <p>
 * This enum is used to determine which icon to display in lists and 
 * how to pluralize the dose amount in summaries (e.g., "1 Pill" vs "2 Pills").
 * </p>
 */
@SuppressWarnings("unused")
public enum EForm {
    /** Solid oral medication. */
    Pill,
    /** Liquid oral or topical medication. */
    Solution,
    /** Intravenous or intramuscular medication. */
    Injection,
    /** Dry medication to be dissolved or inhaled. */
    Powder,
    /** Eye, ear, or oral drops. */
    Drops,
    /** Metered-dose inhaler or spray. */
    Inhaler,
    /** Fallback for unconventional medication forms. */
    Other
}