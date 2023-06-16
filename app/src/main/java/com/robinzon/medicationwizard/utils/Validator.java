package com.robinzon.medicationwizard.utils;

import androidx.annotation.NonNull;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.util.Map;

public class Validator {

    private static WeakReference<Validator> sThisInstance;

    @NonNull public static Validator getInstance() {
        if (null == sThisInstance || null == sThisInstance.get()){
            sThisInstance = new WeakReference<>(new Validator());
        }
        return sThisInstance.get();
    }

    /**
     * @param jsonArray
     * @return null if the json array is empty or null.
     * Otherwise the json array received as the param
     */
    public boolean isValidJsonArray(final JSONArray jsonArray) {
        return null != jsonArray && 0 != jsonArray.length();
    }

    @SuppressWarnings("unused")
    public boolean isValidMap(final Map<Object, Object> map) {
        return null != map && !map.isEmpty();
    }









}
