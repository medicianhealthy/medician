package com.robinzon.medicationwizard.entities;

public enum EInstructions {
    BeforeEating ("Before eating"),
    WhileEating ("While eating"),
    AfterEating ("After eating"),
    DoesNotMatter ("Doesn't matter");

    final String mDescription;

    EInstructions(String description) {
        this.mDescription = description;
    }

    public String getDescription(){
        return mDescription;
    }
}
