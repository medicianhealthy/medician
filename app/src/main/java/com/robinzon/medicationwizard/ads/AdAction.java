package com.robinzon.medicationwizard.ads;

/**
 * Represents the various lifecycle actions and events that an ad can undergo.
 * This enum is used to track and communicate the state of an ad across the application.
 */
public enum AdAction {
    /**
     * Indicates that the ad has initiated a network request to load its content.
     */
    StartingToLoad,

    /**
     * Indicates that the ad has successfully received content and is ready to be shown.
     */
    LoadedSuccessfully,

    /**
     * Indicates that the ad failed to load its content from the network.
     */
    FailedToLoad,

    /**
     * Indicates that the ad is currently being displayed to the user.
     */
    Showing,

    /**
     * Indicates that the ad encountered an error while attempting to display its content.
     */
    FailedToShow,

    /**
     * Indicates that the user has interacted with the ad by clicking on it.
     */
    Clicked,

    /**
     * Indicates that the user or system has closed or dismissed the ad.
     */
    Dismissed,

    /**
     * Indicates that an impression has been successfully recorded for the ad.
     */
    Impression,

    /**
     * Indicates that the internal ad object instance has been instantiated.
     */
    Created,

    /**
     * Indicates that a rewarded ad is currently dispensing a reward to the user.
     */
    Rewarding
}