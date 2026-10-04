package com.robinzon.medicationwizard.ads;

/**
 * Specifies the supported ad formats for the application.
 * This enum is utilized to determine the appropriate ad loader and behavioral logic for different ad units.
 */
public enum AdType {
    /** Represents an App Open ad, shown during app launch or foregrounding. */
    AppOpen,

    /** Represents an adaptive banner ad that resizes optimally based on the device width. */
    AdaptiveBanner,

    /** Represents a standard banner ad with fixed dimensions. */
    Banner,

    /** Represents a full-screen interstitial static ad. */
    Interstitial,

    /** Represents a full-screen interstitial video ad. */
    InterstitialVideo,

    /** Represents a standard Native Advanced ad integrated into the UI. */
    NativeAdvanced,

    /** Represents a Native Advanced ad containing video content. */
    NativeAdvancedVideo,

    /** Represents a full-screen Rewarded video ad where users earn in-app items. */
    Rewarded,

    /** Represents a Rewarded Interstitial ad, shown automatically with a reward given. */
    RewardedInterstitial
}