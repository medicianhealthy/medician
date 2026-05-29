package com.robinzon.medicationwizard.notifications;

import android.app.Activity;

import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

/**
 * Orchestrates the Google User Messaging Platform (UMP) SDK flow.
 * <p>
 * This manager handles gathering consent information from the user (specifically in the EU/UK)
 * before initializing advertisements to ensure GDPR/CCPA compliance.
 * </p>
 */
public class ConsentManager {

    /**
     * Interface for listening to the completion of the consent gathering process.
     */
    public interface OnConsentFinishedListener {
        /**
         * Triggered when the consent flow is complete, regardless of the user's choice.
         */
        void onFinished();
    }

    /**
     * Gathers consent information and shows the consent form if required.
     * <p>
     * Performance: Checks for existing consent information first and only requests 
     * a form if strictly necessary for the current region/status.
     * </p>
     *
     * @param activity The activity context.
     * @param listener The listener to notify upon completion.
     */
    public static void gatherConsent(Activity activity, OnConsentFinishedListener listener) {
        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();

        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    // Check if a consent form is available and required
                    if (consentInformation.isConsentFormAvailable()) {
                        loadAndShowForm(activity, listener);
                    } else {
                        // Consent already gathered or not required
                        if (listener != null) listener.onFinished();
                    }
                },
                requestConsentError -> {
                    // Consent info update failed; proceed with cautious initialization
                    if (listener != null) listener.onFinished();
                });
    }

    /**
     * Loads and presents the official Google consent form.
     */
    private static void loadAndShowForm(Activity activity, OnConsentFinishedListener listener) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                activity,
                formError -> {
                    // Form flow finished (either dismissed or error)
                    if (listener != null) listener.onFinished();
                }
        );
    }
}
