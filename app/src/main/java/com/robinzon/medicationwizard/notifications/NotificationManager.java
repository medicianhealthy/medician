package com.robinzon.medicationwizard.notifications;
//https://developer.android.com/develop/ui/views/notifications/notification-permission

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.snackbar.Snackbar;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ui.CustomMaterialDialog;
import com.robinzon.medicationwizard.utils.PermissionManager;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.lang.ref.WeakReference;

public class NotificationManager implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private static final String SHARED_PREF_KEY_DO_NOT_SHOW_RATIONAL = "do_not_show_rational";
    private static final String SHARED_PREF_KEY_REFUSE_COUNT = "refuse_count";
    public static final String SHARED_PREF_KEY_HAS_DENIED_NOTI_PERM = "has_denied_noti_perm";
    private static WeakReference<NotificationManager> sInstance;
    private final Activity mActivity;
    private Integer mButtonClickedBeforeDismissed;

    public NotificationManager(@NonNull final Activity activity) {
        this.mActivity = activity;
    }

    /**
     * Determines whether the app should ask the user for notification permissions.
     * This check is necessary as from Android Tiramisu (API level 33) and onwards,
     * apps need explicit permission to send notifications. The method also checks
     * if the user has previously refused the permission and the frequency at which
     * the rational should be shown based on user refusals.
     *
     * @return true if the app should request notification permissions, false otherwise.
     */
    private boolean shouldAskForNotificationPermission() {
        // Check if the Android version is Tiramisu (API level 33) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if notifications are not enabled and if the user hasn't previously
            // chosen not to show the rationale again (based on shared preferences).
            // Also checks if the count of refusals has reached the specified delta to show the rationale.
            return notificationsAreDisabled() &&
                    !SharedPreferencesManager.getInstance(getActivity()).getBoolean(SHARED_PREF_KEY_DO_NOT_SHOW_RATIONAL, false);
        }
        // For Android versions below Tiramisu, notification permission is not required,
        // so return false indicating that the app should not request permission.
        return false;
    }


    @SuppressWarnings("SameReturnValue")
    private int getDeltaToShowRational() {
        return 5;
    }


    private boolean notificationsAreDisabled() {
        final NotificationManagerCompat notificationManager = getNotificationManager();
        return !notificationManager.areNotificationsEnabled();
    }


    @NonNull
    private NotificationManagerCompat getNotificationManager() {
        return NotificationManagerCompat.from(getActivity().getApplicationContext());
    }

    public static NotificationManager getInstance(final Activity activity) {
        if (null == sInstance || null == sInstance.get()) {
            sInstance = new WeakReference<>(new NotificationManager(activity));
        }
        return sInstance.get();
    }


    public void requestPermissionIfNeeded() {
        if (shouldAskForNotificationPermission()) {
            //This method shouldShowRequestPermissionRationale will return true only if user denied once
            //If he didn't deny it will return false, if he denied more than once it will return false;
            if (shouldShowRationalInnerDialog()) {
                if (0 == (getCountRefusedSoFar() % getDeltaToShowRational())) {
                    showRationaleDialog();
                } else {
                    showSnackNoPermission();
                }
                increaseRefuseNumber();
            } else {
                if (!getHasDeniedPermission()) {
                    requestPermission();
                } else {
                    showSnackNoPermissionAndOpen();
                }
            }
        } else if (notificationsAreDisabled()) {
            if (shouldShowRationalInnerDialog()) {
                showSnackNoPermission();
            } else {
                showSnackNoPermissionAndOpen();
            }
        }
    }

    private boolean shouldShowRationalInnerDialog() {
        return PermissionManager.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.POST_NOTIFICATIONS);
    }

    private void showSnackNoPermissionAndOpen() {
        Snackbar.make(getActivity().findViewById(R.id.fab), getActivity().getString(R.string.notification_missing), Snackbar.LENGTH_LONG)
                .setAction(getActivity().getString(R.string.button_allow), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openNotificationAppSettings(getActivity().getApplicationContext());
                    }
                }).show();
    }

    private void showSnackNoPermission() {
        Snackbar.make(getActivity().findViewById(R.id.fab), getActivity().getString(R.string.notification_missing), Snackbar.LENGTH_LONG)
                .setAction(getActivity().getString(R.string.button_allow), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestPermission();
                    }
                }).show();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionManager.askForPermission(getActivity(),
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PermissionManager.REQUEST_PERMISSION_CODE_POST_NOTIFICATIONS);
        }
    }

    private void showRationaleDialog() {
        final CustomMaterialDialog dialog = new CustomMaterialDialog(getActivity());
        dialog.setTitle(getActivity().getString(R.string.permission_rational_notification_title));
        dialog.setMessage(getActivity().getString(R.string.permission_rational_notification_message));
        dialog.setPositiveButton(getActivity().getString(R.string.button_sure), this);
        dialog.setNegativeButton(getActivity().getString(R.string.buttoh_not_now), this);
        dialog.setNeutralButton(getActivity().getString(R.string.buttoh_never), this);
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE -> setButtonClickedBeforeDismissed(DialogInterface.BUTTON_POSITIVE);
            case DialogInterface.BUTTON_NEGATIVE -> setButtonClickedBeforeDismissed(DialogInterface.BUTTON_NEGATIVE);
            case DialogInterface.BUTTON_NEUTRAL -> setButtonClickedBeforeDismissed(DialogInterface.BUTTON_NEUTRAL);
        }

    }

    private Integer getButtonClickedBeforeDismissed() {
        return mButtonClickedBeforeDismissed;
    }

    private void setButtonClickedBeforeDismissed(Integer buttonClickedBeforeDismissed) {
        this.mButtonClickedBeforeDismissed = buttonClickedBeforeDismissed;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        final Integer buttonClickedBeforeDismissed = getButtonClickedBeforeDismissed();
        final boolean hasAReferenceToLastButtonClicked = (null != buttonClickedBeforeDismissed);
        if (hasAReferenceToLastButtonClicked) {
            if (DialogInterface.BUTTON_NEUTRAL == buttonClickedBeforeDismissed) {
                setDoNotShowRationalAgain();
            } else if (DialogInterface.BUTTON_POSITIVE == buttonClickedBeforeDismissed) {
                requestPermission();
            }
        }
        setButtonClickedBeforeDismissed(null);
    }

    private void increaseRefuseNumber() {
        final int countRefusedSoFar = getCountRefusedSoFar();
        SharedPreferencesManager.getInstance(getActivity()).setInt(SHARED_PREF_KEY_REFUSE_COUNT, countRefusedSoFar + 1);
    }

    private int getCountRefusedSoFar() {
        return SharedPreferencesManager.getInstance(getActivity()).getInt(SHARED_PREF_KEY_REFUSE_COUNT, 0);
    }

    private void setDoNotShowRationalAgain() {
        SharedPreferencesManager.getInstance(getActivity()).setBoolean(SHARED_PREF_KEY_DO_NOT_SHOW_RATIONAL, true);
    }

    @NonNull
    private Activity getActivity() {
        return mActivity;
    }

    public void setHasGrantedPermission(final boolean granted) {
        if (!granted) {
            SharedPreferencesManager.getInstance(getActivity()).setBoolean(SHARED_PREF_KEY_HAS_DENIED_NOTI_PERM, true);
        }
    }

    private boolean getHasDeniedPermission() {
        return SharedPreferencesManager.getInstance(getActivity()).getBoolean(SHARED_PREF_KEY_HAS_DENIED_NOTI_PERM, false);
    }

    /**
     * Opens the system settings screen for the application's notifications.
     *
     * <p>Since direct access to notification settings is not available before Android API level 26,
     * this method will open the general settings screen for the application. The user may have
     * to navigate to notification settings manually.</p>
     *
     * @param context The Context in which this method is called.
     */
    private void openNotificationAppSettings(final Context context) {
        // Create an Intent to open the application's general settings screen.
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);

        // Use the package name to create a URI for the Intent.
        intent.setData(Uri.parse("package:" + context.getPackageName()));

        // Add flags to the Intent to avoid creating a new task and to bring the existing settings
        // task to the front if it already exists.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        // Start the settings activity.
        context.startActivity(intent);
    }
}
