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
     * Retrieves the physical width of the usable screen area in pixels.
     * <p>
     * For devices running Android R and above, it uses the current window metrics bounds.
     * For older devices, it falls back to the default display metrics.
     * </p>
     *
     * @param activity The current activity context. Must not be null.
     * @return The physical screen width in pixels.
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
     * Retrieves the physical height of the usable screen area in pixels.
     * <p>
     * For devices running Android R and above, it uses the current window metrics bounds.
     * For older devices, it falls back to the default display metrics.
     * </p>
     *
     * @param activity The current activity context. Must not be null.
     * @return The physical screen height in pixels.
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
     * <p>
     * Caches the value after the first successful retrieval to improve performance
     * on subsequent calls.
     * </p>
     *
     * @param resources The application or activity resources. Must not be null.
     * @return The display density scaling factor (e.g., 2.0 for xhdpi).
     */
    public static float getDensity(final Resources resources) {
        if (mDensity == 0F) {
            mDensity = resources.getDisplayMetrics().density;
        }
        return mDensity;
    }
}
