package com.robinzon.medicationwizard.utils;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

/**
 * Utility class for handling Android runtime permissions.
 * <p>
 * This class abstracts away the boilerplate of requesting permissions and checking
 * rationale, centralizing permission logic for the application.
 * </p>
 */
public class PermissionManager {

    /**
     * Request code identifier for posting notifications permission.
     */
    public static final int REQUEST_PERMISSION_CODE_POST_NOTIFICATIONS = 1001;

    /**
     * Determines whether the app should show a UI with a rationale for requesting a permission.
     * <p>
     * This is typically used when the user has previously denied the permission request
     * but hasn't selected "Don't ask again".
     * </p>
     *
     * @param activity   The current activity requesting the permission. Must not be null.
     * @param permission The specific permission string to check. Must not be null.
     * @return True if the user previously denied the permission and a rationale should be shown, false otherwise.
     */
    public static boolean shouldShowRequestPermissionRationale(@NonNull final Activity activity,
                                                               @NonNull final String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    /**
     * Requests the specified permissions from the user.
     * <p>
     * Displays the standard Android permission dialog. The result is delivered to
     * the activity's onRequestPermissionsResult method.
     * </p>
     *
     * @param activity    The activity that is requesting the permissions. Must not be null.
     * @param permission  An array of permission strings to be requested. Must not be null.
     * @param requestCode An integer request code to identify the permission request in the result callback.
     */
    public static void askForPermission(@NonNull final Activity activity,
                                        @NonNull final String[] permission,
                                        final int requestCode) {
        ActivityCompat.requestPermissions(activity, permission, requestCode);

    }
}
