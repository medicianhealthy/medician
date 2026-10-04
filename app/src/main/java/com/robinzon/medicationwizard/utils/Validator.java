package com.robinzon.medicationwizard.utils;

import androidx.annotation.NonNull;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Singleton utility class used for common validation operations.
 * <p>
 * Provides centralized helper methods for evaluating data types, verifying non-empty states,
 * and performing sanity checks. Managed using a WeakReference to reduce memory overhead.
 * </p>
 */
public class Validator {

    private static WeakReference<Validator> sThisInstance;

    /**
     * Retrieves the singleton instance of the Validator.
     * <p>
     * Reinitializes the instance if the garbage collector has cleared the weak reference.
     * </p>
     *
     * @return The active Validator instance.
     */
    @NonNull
    public static Validator getInstance() {
        if (null == sThisInstance || null == sThisInstance.get()) {
            sThisInstance = new WeakReference<>(new Validator());
        }
        return sThisInstance.get();
    }

    /**
     * Verifies whether the provided JSON array is valid and contains elements.
     *
     * @param jsonArray The JSON array to evaluate.
     * @return True if the JSON array is not null and has a length greater than zero, false otherwise.
     */
    public boolean isValidJsonArray(final JSONArray jsonArray) {
        return null != jsonArray && 0 != jsonArray.length();
    }

    /**
     * Verifies whether the provided map is valid and contains entries.
     *
     * @param map The map to evaluate.
     * @return True if the map is not null and not empty, false otherwise.
     */
    @SuppressWarnings("unused")
    public boolean isValidMap(final Map<Object, Object> map) {
        return null != map && !map.isEmpty();
    }


}
