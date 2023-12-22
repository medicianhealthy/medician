package com.robinzon.medicationwizard.utils;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class PermissionManager {

    public static final int REQUEST_PERMISSION_CODE_POST_NOTIFICATIONS = 1001;
    public static boolean shouldShowRequestPermissionRationale(@NonNull final Activity activity,
                                                               @NonNull final String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    public static void askForPermission(@NonNull final Activity activity,
                                        @NonNull final String[] permission,
                                        final int requestCode) {
        ActivityCompat.requestPermissions(activity, permission , requestCode);

    }
}
