package com.robinzon.medicationwizard.notifications;

import android.app.Activity;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.utils.Logger;

/**
 * Orchestrates the Google User Messaging Platform (UMP) SDK flow.
 * <p>
 * This manager handles gathering consent information from the user (specifically in the EU/UK)
 * before initializing advertisements to ensure GDPR/CCPA compliance.
 * </p>
 */
public class ConsentManager {

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
        ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false);

        if (BuildConfig.DEBUG) {
            ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId("D66793A602B7390D434222E426F66E74") // Emulator ID
                    .build();
            paramsBuilder.setConsentDebugSettings(debugSettings);
            Logger.log("ConsentManager", "Debug mode active: Forcing EEA geography.");
        }

        ConsentRequestParameters params = paramsBuilder.build();

        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    Logger.log("ConsentManager", "Consent info updated. Form available: " + consentInformation.isConsentFormAvailable()
                            + ", Status: " + consentInformation.getConsentStatus());

                    // Check if a consent form is available and required
                    if (consentInformation.isConsentFormAvailable()) {
                        loadAndShowForm(activity, listener);
                    } else {
                        // Consent already gathered or not required
                        if (listener != null) listener.onFinished();
                    }
                },
                requestConsentError -> {
                    Logger.log("ConsentManager", "Consent update failed: " + requestConsentError.getMessage());
                    // Consent info update failed; proceed with cautious initialization
                    if (listener != null) listener.onFinished();
                });
    }

    /**
     * Loads and presents the official Google consent form.
     */
    private static void loadAndShowForm(Activity activity, OnConsentFinishedListener listener) {
        Logger.log("ConsentManager", "Loading and showing form...");
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                activity,
                formError -> {
                    if (formError != null) {
                        Logger.log("ConsentManager", "Form error: " + formError.getMessage());
                    } else {
                        Logger.log("ConsentManager", "Form flow finished successfully.");
                    }
                    // Form flow finished (either dismissed or error)
                    if (listener != null) listener.onFinished();
                }
        );
    }

    /**
     * Interface for listening to the completion of the consent gathering process.
     */
    public interface OnConsentFinishedListener {
        /**
         * Triggered when the consent flow is complete, regardless of the user's choice.
         */
        void onFinished();
    }
}
