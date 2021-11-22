package com.robinzon.medicationwizard.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;

import org.json.JSONArray;

import java.util.Collection;
import java.util.Map;

public class Validator {

    public static boolean isValidString(final String string){
        return (null != string) && (!string.isEmpty());
    }

    public static boolean isValidCollection(final Collection<?> collection){
        return (null != collection) && (!collection.isEmpty());
    }

    public static boolean isValidMap(final Map<?,?> map){
        return (null != map) && (!map.isEmpty());
    }

    public static boolean isValidAndroidResourceId(final Integer resourceId){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Resources.ID_NULL != resourceId;
        }
        return (0 != resourceId);
    }

    public static boolean isValidBitMap(final Bitmap bitmap){
        return ((null != bitmap) && (bitmap.getWidth() > 0) && (bitmap.getHeight() > 0) && !bitmap.isRecycled());
    }

    public static boolean isValidObject(Object object) {
        return null != object;
    }

    public static boolean isValidJsonArray(JSONArray jsonArray) {
        return null != jsonArray && jsonArray.length() > 0;
    }
}
