package com.robinzon.medicationwizard.entities;

import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;

import org.json.JSONException;
import org.json.JSONObject;

public class ActiveIngredient extends MedicationWizardSuper {
    @Override
    public String getClassName() {
        return ActiveIngredient.class.getSimpleName();
    }

    public static class JsonKeys {
        public static final String JSON_KEY_NAME = "mName";
        public static final String JSON_MEASUREMENT_UNIT = "mMeasurementUnit";
    }

    public final String mName;
    public final EMeasurementUnit mMeasurementUnit;

    @SuppressWarnings("unused")
    public ActiveIngredient(String name, EMeasurementUnit measurementUnit) {
        this.mName = name;
        this.mMeasurementUnit = measurementUnit;
    }

    public JSONObject toJsonObject() {
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
