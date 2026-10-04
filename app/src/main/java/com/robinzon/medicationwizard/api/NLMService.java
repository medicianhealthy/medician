package com.robinzon.medicationwizard.api;

import com.robinzon.medicationwizard.api.models.RxNormSpellingResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Defines the endpoints for querying the National Library of Medicine (NLM) APIs.
 * This service facilitates searching for medication terms and retrieving spelling suggestions.
 */
public interface NLMService {
    
    /**
     * Searches for medications based on the provided search terms using the clinicaltables API.
     *
     * @param terms The text string to search for within medication names. Must not be null.
     * @return A {@link Call} object containing a dynamic list representing the parsed JSON response.
     */
    @GET("https://clinicaltables.nlm.nih.gov/api/rxterms/v3/search")
    Call<List<Object>> searchMedications(@Query("terms") String terms);

    /**
     * Retrieves spelling suggestions for a medication name from the RxNav API.
     * This is useful for providing alternatives when a medication name is misspelled.
     *
     * @param name The potentially misspelled medication name to check. Must not be null.
     * @return A {@link Call} object containing the {@link RxNormSpellingResponse} with spelling suggestions.
     */
    @GET("https://rxnav.nlm.nih.gov/REST/spellingsuggestions.json")
    Call<RxNormSpellingResponse> getSpellingSuggestions(@Query("name") String name);
}
