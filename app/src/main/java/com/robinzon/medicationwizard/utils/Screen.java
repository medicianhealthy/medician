package com.robinzon.medicationwizard.utils;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

public final class Screen {

    private static float mDensity;
    public static int getScreenWidthPX(@NonNull final Activity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final Rect bounds = activity.getWindowManager().getCurrentWindowMetrics().getBounds();
            return bounds.width();
        } else {
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.widthPixels;
        }
    }

    public static float getDensity(final Resources resources){
        if (mDensity == 0F){
            mDensity = resources.getDisplayMetrics().density;
        }
        return mDensity;
    }
}
