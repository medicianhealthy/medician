package com.robinzon.medicationwizard.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;

import org.json.JSONException;
import org.json.JSONObject;

public class ActiveIngredient extends MedicationWizardSuper {

    public static final String JSON_KEY_NAME = "mName";
    public static final String JSON_MEASUREMENT_UNIT = "mMeasurementUnit";



    public final String Name;
    public final EMeasurementUnit mMeasurementUnit;

    @SuppressWarnings("unused")
    public ActiveIngredient(@NonNull String name, @NonNull EMeasurementUnit measurementUnit) {
        this.Name = name;
        this.mMeasurementUnit = measurementUnit;
    }

    @Nullable public JSONObject toJsonObject() {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JSON_KEY_NAME, getName());
            jsonObject.put(JSON_MEASUREMENT_UNIT, getMeasurementUnit().getName());
            return jsonObject;
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public String getClassName() {
        return ActiveIngredient.class.getSimpleName();
    }

    private String getName() {
        return Name;
    }

    private EMeasurementUnit getMeasurementUnit() {
        return mMeasurementUnit;
    }
}
