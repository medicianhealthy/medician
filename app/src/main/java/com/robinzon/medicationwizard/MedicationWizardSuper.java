package com.robinzon.medicationwizard;

/**
 * Base class for domain objects and utilities within the Medication Wizard app.
 * <p>
 * Provides common utility methods for logging and identification.
 * Most entity classes (like {@link com.robinzon.medicationwizard.entities.Medication})
 * extend this class to gain access to standardized naming conventions.
 * </p>
 */
public class MedicationWizardSuper {

    /**
     * Helper method for logging that returns the simple class name of the caller.
     *
     * @return The simple name of the current class (e.g., "Medication").
     */
    protected String Me() {
        return getClass().getSimpleName();
    }
}