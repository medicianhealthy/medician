package com.robinzon.medicationwizard.notifications;
//https://developer.android.com/develop/ui/views/notifications/notification-permission

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.snackbar.Snackbar;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ui.CustomMaterialDialog;
import com.robinzon.medicationwizard.utils.PermissionManager;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.lang.ref.WeakReference;

public class NotificationManager implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    public static final String CHANNEL_ID = "medication_reminders";

    private static final String SHARED_PREF_KEY_DO_NOT_SHOW_RATIONAL = "do_not_show_rational";
    private static final String SHARED_PREF_KEY_REFUSE_COUNT = "refuse_count";
    public static final String SHARED_PREF_KEY_HAS_DENIED_NOTI_PERM = "has_denied_noti_perm";
    private static WeakReference<NotificationManager> sInstance;
    private final Activity mActivity;
    private Integer mButtonClickedBeforeDismissed;

    public NotificationManager(@NonNull final Activity activity) {
        this.mActivity = activity;
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notification_channel_desc));

            android.app.NotificationManager manager = context.getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private boolean shouldAskForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return notificationsAreDisabled() &&
                    !SharedPreferencesManager.getInstance(getActivity()).getBoolean(SHARED_PREF_KEY_DO_NOT_SHOW_RATIONAL, false);
        }
        return false;
    }

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

    public boolean hasPermission() {
        return !notificationsAreDisabled();
    }

    public static NotificationManager getInstance(final Activity activity) {
        if (null == sInstance || null == sInstance.get()) {
            sInstance = new WeakReference<>(new NotificationManager(activity));
        }
        return sInstance.get();
    }

    public void requestPermissionIfNeeded() {
        if (shouldAskForNotificationPermission()) {
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

    /**
     * Shows a rationale dialog first, then requests permission.
     * Ideal for the first time a user takes an action that requires notifications.
     */
    public void requestWithRationale() {
        if (notificationsAreDisabled()) {
            if (!SharedPreferencesManager.getInstance(getActivity()).getBoolean(SHARED_PREF_KEY_DO_NOT_SHOW_RATIONAL, false)) {
                showRationaleDialog();
            } else {
                requestPermission();
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

    private void openNotificationAppSettings(final Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(intent);
    }
}