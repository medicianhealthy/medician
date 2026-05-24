package com.robinzon.medicationwizard.utils;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

public final class Screen {

    private static float mDensity;

    public static int getUsableScreenWidthPX(@NonNull final Activity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final Rect bounds = activity.getWindowManager().getCurrentWindowMetrics().getBounds();
            return bounds.width();
        } else {
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.widthPixels;
        }
    }

    public static int getUsableScreenHeightPX(@NonNull final Activity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final Rect bounds = activity.getWindowManager().getCurrentWindowMetrics().getBounds();
            return bounds.height();
        } else {
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.heightPixels;
        }
    }

    public static float getDensity(final Resources resources){
        if (mDensity == 0F){
            mDensity = resources.getDisplayMetrics().density;
        }
        return mDensity;
    }

    private static class ScreenSize{
        public int width;
        public int height;
    }
}
