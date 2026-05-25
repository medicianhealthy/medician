package com.robinzon.medicationwizard.entities;

import androidx.annotation.NonNull;

/**
 * Enumeration of common consumption instructions for medications.
 * <p>
 * Provides human-readable descriptions that are displayed in the medication 
 * list and reminder cards to help users take their medicine correctly 
 * in relation to meals or sleep.
 * </p>
 */
@SuppressWarnings("unused")
public enum EInstructions {
    /** Take the dose on an empty stomach. */
    BEFORE_EATING("Before eating"),
    /** Take the dose during a meal. */
    WHILE_EATING("While eating"),
    /** Take the dose on a full stomach. */
    AFTER_EATING("After eating"),
    /** Take the dose immediately before going to bed. */
    BEFORE_SLEEP("Before sleep"),
    /** No specific consumption rules apply. */
    DOES_NOT_MATTER("Doesn't matter");

    private final String mDescription;

    EInstructions(@NonNull final String description) {
        this.mDescription = description;
    }

    /**
     * @return The localized, human-readable description of the instruction.
     */
    public String getDescription() {
        return mDescription;
    }
}