package com.robinzon.medicationwizard.utils;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

public final class Screen {

    private static float mDensity;
    private static ScreenSize mScreenSize;
    public static int getScreenWidthPX(@NonNull final Activity activity){
        if (null == mScreenSize){
            mScreenSize = new ScreenSize();
        }
        if (mScreenSize.width == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final Rect bounds = activity.getWindowManager().getCurrentWindowMetrics().getBounds();
                mScreenSize.width = bounds.width();
            } else {
                final DisplayMetrics displayMetrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                mScreenSize.width = displayMetrics.widthPixels;
            }
        }
        return mScreenSize.width;
    }

    public static int getScreenHeightPX(@NonNull final Activity activity){
        if (null == mScreenSize){
            mScreenSize = new ScreenSize();
        }
        if (mScreenSize.height == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final Rect bounds = activity.getWindowManager().getCurrentWindowMetrics().getBounds();
                mScreenSize.height = bounds.height();
            } else {
                final DisplayMetrics displayMetrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                mScreenSize.height = displayMetrics.heightPixels;
            }
        }
        return mScreenSize.height;
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
