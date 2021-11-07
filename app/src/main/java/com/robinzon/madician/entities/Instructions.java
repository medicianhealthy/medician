package com.robinzon.madician.entities;

public enum Instructions {
    BeforeEating ("Before eating"),
    WhileEating ("While eating"),
    AfterEating ("After eating"),
    DoesNotMatter ("Doesn't matter");

    final String mDescription;

    Instructions(String description) {
        this.mDescription = description;
    }

    public String getDescription(){
        return mDescription;
    }
}
