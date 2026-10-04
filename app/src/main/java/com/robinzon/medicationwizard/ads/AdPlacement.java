package com.robinzon.medicationwizard.ads;

/**
 * Defines the placement locations for ads within the application structure.
 * Used to categorize and differentiate where an ad is being requested or displayed.
 */
public enum AdPlacement {
    /**
     * Represents the primary ad placement, typically shown on the main user interface.
     */
    Main, 
    
    /**
     * Represents a secondary ad placement, usually nested deeper within the app's screens.
     */
    Secondary
}