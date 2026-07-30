package com.robinzon.medicationwizard.api;

import com.robinzon.medicationwizard.api.models.RxNormSpellingResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NLMService {
    @GET("https://clinicaltables.nlm.nih.gov/api/rxterms/v3/search")
    Call<List<Object>> searchMedications(@Query("terms") String terms);

    @GET("https://rxnav.nlm.nih.gov/REST/spellingsuggestions.json")
    Call<RxNormSpellingResponse> getSpellingSuggestions(@Query("name") String name);
}
