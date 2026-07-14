package com.robinzon.medicationwizard.entities;

import android.content.Context;
import com.robinzon.medicationwizard.R;

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
    BEFORE_EATING(R.string.instruction_before_eating),
    /** Take the dose during a meal. */
    WHILE_EATING(R.string.instruction_while_eating),
    /** Take the dose on a full stomach. */
    AFTER_EATING(R.string.instruction_after_eating),
    /** Take the dose immediately before going to bed. */
    BEFORE_SLEEP(R.string.instruction_before_sleep),
    /** No specific consumption rules apply. */
    DOES_NOT_MATTER(R.string.instruction_no_preference);

    private final int mLabelResId;

    EInstructions(int labelResId) {
        this.mLabelResId = labelResId;
    }

    /**
     * @return The localized, human-readable description of the instruction.
     */
    public String getDescription(Context context) {
        return context.getString(mLabelResId);
    }
}