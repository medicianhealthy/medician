package com.robinzon.medicationwizard.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Manages the Retrofit client instance for the National Library of Medicine (NLM) APIs.
 * This class ensures a singleton instance of the NLMService is created and reused.
 */
public class NLMClient {
    private static NLMService service;

    /**
     * Retrieves the singleton instance of the NLMService.
     * Initializes the Retrofit client lazily on the first call, configuring it with
     * the base URL and Gson converter for JSON serialization and deserialization.
     *
     * @return The configured instance of {@link NLMService} for making API calls.
     */
    public static NLMService getService() {
        if (service == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://clinicaltables.nlm.nih.gov/") // Base URL is required but we use full URLs in GET
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            service = retrofit.create(NLMService.class);
        }
        return service;
    }
}
