package com.robinzon.medicationwizard.entities;

import org.json.JSONException;
import org.json.JSONObject;

public class ActiveIngredient {
    public static class JsonKeys{
        public static final String JSON_KEY_NAME = "mName";
        public static final String JSON_MEASUREMENT_UNIT = "mMeasurementUnit";
    }
    public final String mName;
    public final MeasurementUnit mMeasurementUnit;

    public ActiveIngredient(String name, MeasurementUnit measurementUnit) {
        this.mName = name;
        this.mMeasurementUnit = measurementUnit;
    }

    public JSONObject toJsonObject(){
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JsonKeys.JSON_KEY_NAME, mName);
            jsonObject.put(JsonKeys.JSON_MEASUREMENT_UNIT, mMeasurementUnit.getName());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
