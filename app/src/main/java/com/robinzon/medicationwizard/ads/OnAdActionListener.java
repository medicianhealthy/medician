package com.robinzon.medicationwizard.ads;

import androidx.annotation.NonNull;

import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;

/**
 * Interface definition for a callback to be invoked when an ad action occurs.
 * This is used to communicate ad lifecycle events back to the managing components.
 */
public interface OnAdActionListener {
    /**
     * Called when an ad performs a specific action in its lifecycle.
     *
     * @param adMobAd The ad object that triggered the action. Must not be null.
     * @param adAction The specific action that occurred.
     */
    void onAdAction(@NonNull final AdMobAd adMobAd, final AdAction adAction);
}