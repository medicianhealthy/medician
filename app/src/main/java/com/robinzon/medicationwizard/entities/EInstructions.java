package com.robinzon.medicationwizard.entities;

import androidx.annotation.NonNull;

@SuppressWarnings("unused")
public enum EInstructions {
    BEFORE_EATING("Before eating"),
    WHILE_EATING("While eating"),
    AFTER_EATING("After eating"),
    BEFORE_SLEEP("Before sleep"),
    DOES_NOT_MATTER("Doesn't matter");

    final String mDescription;

    EInstructions(@NonNull final String description) {
        this.mDescription = description;
    }

    public String getDescription() {
        return mDescription;
    }
}
