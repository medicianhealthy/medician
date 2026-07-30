package com.robinzon.medicationwizard.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NLMClient {
    private static NLMService service;

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
