package com.robinzon.medicationwizard.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RxNormSpellingResponse {
    @SerializedName("suggestionGroup")
    public SuggestionGroup suggestionGroup;

    public static class SuggestionGroup {
        @SerializedName("suggestionList")
        public SuggestionList suggestionList;
    }

    public static class SuggestionList {
        @SerializedName("suggestion")
        public List<String> suggestions;
    }
}
