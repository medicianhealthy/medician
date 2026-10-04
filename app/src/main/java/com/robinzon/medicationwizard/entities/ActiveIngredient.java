package com.robinzon.medicationwizard.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the active chemical component of a medication and its measurement unit.
 * <p>
 * This record holds the fundamental data required to identify what the medication is made of
 * and how it's measured, useful for detailed medical summaries or API integration.
 * </p>
 *
 * @param Name             Must not be null. The name of the active ingredient.
 * @param mMeasurementUnit Must not be null. The measurement unit used for this ingredient.
 */
public record ActiveIngredient(String Name, EMeasurementUnit mMeasurementUnit) {

    public static final String JSON_KEY_NAME = "mName";
    public static final String JSON_MEASUREMENT_UNIT = "mMeasurementUnit";


    /**
     * Constructs a new ActiveIngredient with the specified name and measurement unit.
     *
     * @param Name             Must not be null. The name of the active ingredient.
     * @param mMeasurementUnit Must not be null. The measurement unit for the ingredient.
     */
    @SuppressWarnings("unused")
    public ActiveIngredient(@NonNull String Name, @NonNull EMeasurementUnit mMeasurementUnit) {
        this.Name = Name;
        this.mMeasurementUnit = mMeasurementUnit;
    }

    /**
     * Serializes this active ingredient into a JSON object.
     *
     * @return Returns a JSONObject containing the ingredient's name and measurement unit, or null if a JSONException occurs.
     */
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


    /**
     * Retrieves the name of the active ingredient.
     *
     * @return Returns the name of the active ingredient.
     */
    private String getName() {
        return Name;
    }

    /**
     * Retrieves the measurement unit associated with this active ingredient.
     *
     * @return Returns the measurement unit.
     */
    private EMeasurementUnit getMeasurementUnit() {
        return mMeasurementUnit;
    }
}
