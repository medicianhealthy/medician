package com.robinzon.medicationwizard.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents the response from the RxNav API for spelling suggestions.
 * This model parses the nested JSON structure returned when querying for medication name alternatives.
 */
public class RxNormSpellingResponse {
    
    /**
     * The root group of suggestions provided by the API.
     */
    @SerializedName("suggestionGroup")
    public SuggestionGroup suggestionGroup;

    /**
     * Contains the wrapper for the actual list of spelling suggestions.
     */
    public static class SuggestionGroup {
        
        /**
         * The list container containing suggested medication names.
         */
        @SerializedName("suggestionList")
        public SuggestionList suggestionList;
    }

    /**
     * Represents the actual list of suggestion strings.
     */
    public static class SuggestionList {
        
        /**
         * The list of corrected or alternative medication names.
         */
        @SerializedName("suggestion")
        public List<String> suggestions;
    }
}
