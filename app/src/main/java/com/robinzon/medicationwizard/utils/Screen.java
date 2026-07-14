package com.robinzon.medicationwizard.utils;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

/**
 * Utility class for retrieving device display metrics and screen dimensions.
 * <p>
 * This class handles the complexity of retrieving screen sizes across different
 * Android versions, including the modern {@link android.view.WindowMetrics} API
 * introduced in Android R (API 30).
 * </p>
 */
public final class Screen {

    private static float mDensity;

    /**
     * Returns the physical width of the usable screen area in pixels.
     *
     * @param activity The current activity.
     * @return Screen width in pixels.
     */
    public static int getUsableScreenWidthPX(@NonNull final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final Rect bounds = activity.getWindowManager().getCurrentWindowMetrics().getBounds();
            return bounds.width();
        } else {
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.widthPixels;
        }
    }

    /**
     * Returns the physical height of the usable screen area in pixels.
     *
     * @param activity The current activity.
     * @return Screen height in pixels.
     */
    public static int getUsableScreenHeightPX(@NonNull final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final Rect bounds = activity.getWindowManager().getCurrentWindowMetrics().getBounds();
            return bounds.height();
        } else {
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.heightPixels;
        }
    }

    /**
     * Retrieves the logical density of the display (density-independent pixel factor).
     * Caches the value after the first successful retrieval.
     *
     * @param resources Application or Activity resources.
     * @return The display density (e.g., 2.0 for xhdpi).
     */
    public static float getDensity(final Resources resources) {
        if (mDensity == 0F) {
            mDensity = resources.getDisplayMetrics().density;
        }
        return mDensity;
    }
}