package com.robinzon.medicationwizard.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public record ActiveIngredient(String Name, EMeasurementUnit mMeasurementUnit) {

    public static final String JSON_KEY_NAME = "mName";
    public static final String JSON_MEASUREMENT_UNIT = "mMeasurementUnit";


    @SuppressWarnings("unused")
    public ActiveIngredient(@NonNull String Name, @NonNull EMeasurementUnit mMeasurementUnit) {
        this.Name = Name;
        this.mMeasurementUnit = mMeasurementUnit;
    }

    @Nullable
    public JSONObject toJsonObject() {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JSON_KEY_NAME, getName());
            jsonObject.put(JSON_MEASUREMENT_UNIT, getMeasurementUnit().getName());
            return jsonObject;
        } catch (JSONException e) {
            return null;
        }
    }


    private String getName() {
        return Name;
    }

    private EMeasurementUnit getMeasurementUnit() {
        return mMeasurementUnit;
    }
}
