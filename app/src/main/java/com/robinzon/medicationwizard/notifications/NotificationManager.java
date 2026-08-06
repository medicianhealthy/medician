package com.robinzon.medicationwizard.notifications;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ui.CustomMaterialDialog;
import com.robinzon.medicationwizard.utils.PermissionManager;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.lang.ref.WeakReference;

public class NotificationManager implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    // REFINED: V2 Channel ID to force-apply the 'No Sound' setting on existing installs
    public static final String CHANNEL_ID = "medication_reminders_v2";
    public static final String PREF_KEY_HAS_DENIED_NOTIFICATION_PERMISSION = "has_denied_noti_perm";
    private static final String PREF_KEY_DO_NOT_SHOW_RATIONALE = "do_not_show_rationale";
    private static final String PREF_KEY_REFUSE_COUNT = "refuse_count";
    private static NotificationManager sInstance;
    private WeakReference<Activity> mActivityRef;
    private Integer lastClickedButton;

    private NotificationManager(@NonNull final Activity activity) {
        this.mActivityRef = new WeakReference<>(activity);
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete old channel if it exists to clean up
            android.app.NotificationManager manager = context.getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.deleteNotificationChannel("medication_reminders");
            }

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notification_channel_desc));

            // FIX: Silence the system sound so we only hear the app's custom MediaPlayer alert.
            // This prevents the "Double Sound" issue.
            channel.setSound(null, null);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Retrieves the singleton NotificationManager instance for the current activity.
     */
    public static synchronized NotificationManager getInstance(@NonNull final Activity activity) {
        if (null == sInstance) {
            sInstance = new NotificationManager(activity);
        } else {
            sInstance.mActivityRef = new WeakReference<>(activity);
        }
        return sInstance;
    }

    /**
     * Dismisses a specific notification by its ID.
     *
     * @param context        Application context.
     * @param notificationId The unique ID of the notification to dismiss.
     */
    public static void dismissNotification(Context context, int notificationId) {
        NotificationManagerCompat.from(context).cancel(notificationId);
    }

    private boolean shouldAskForNotificationPermission() {
        Activity activity = mActivityRef.get();
        if (activity == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return notificationsAreDisabled() &&
                    !SharedPreferencesManager.getInstance(activity).getBoolean(PREF_KEY_DO_NOT_SHOW_RATIONALE, false);
        }
        return false;
    }

    private int getDeltaToShowRationale() {
        return 5;
    }

    private boolean notificationsAreDisabled() {
        final NotificationManagerCompat managerCompat = getNotificationManagerCompat();
        return !managerCompat.areNotificationsEnabled();
    }

    @NonNull
    private NotificationManagerCompat getNotificationManagerCompat() {
        Activity activity = mActivityRef.get();
        Context context = activity != null ? activity.getApplicationContext() : null;
        if (context == null) {
            // Fallback for cases where activity might be null but we need compat manager
            // Though unlikely given how this class is used.
            throw new IllegalStateException("NotificationManager requires an active Activity context");
        }
        return NotificationManagerCompat.from(context);
    }

    /**
     * @return True if the application has the POST_NOTIFICATIONS permission granted.
     */
    public boolean hasPermission() {
        return !notificationsAreDisabled();
    }

    public void requestPermissionIfNeeded() {
        if (shouldAskForNotificationPermission()) {
            if (shouldShowRationalInnerDialog()) {
                if (0 == (getCountRefusedSoFar() % getDeltaToShowRationale())) {
                    showRationaleDialog();
                } else {
                    showPermissionDialog(false);
                }
                increaseRefuseNumber();
            } else {
                if (!getHasDeniedPermission()) {
                    requestPermission();
                } else {
                    showPermissionDialog(true);
                }
            }
        } else if (notificationsAreDisabled()) {
            showPermissionDialog(!shouldShowRationalInnerDialog());
        }
    }

    /**
     * Shows a rationale dialog first, then requests permission.
     * Ideal for the first time a user takes an action that requires notifications.
     */
    public void requestWithRationale() {
        Activity activity = mActivityRef.get();
        if (activity == null) return;
        
        if (notificationsAreDisabled()) {
            if (!SharedPreferencesManager.getInstance(activity).getBoolean(PREF_KEY_DO_NOT_SHOW_RATIONALE, false)) {
                showRationaleDialog();
            } else {
                requestPermission();
            }
        }
    }

    private boolean shouldShowRationalInnerDialog() {
        Activity activity = mActivityRef.get();
        return activity != null && PermissionManager.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS);
    }

    /**
     * Displays a dialog inviting the user to enable notifications.
     * Guides them to system settings if they have previously denied the permission.
     */
    public void showInvitationDialog() {
        // If we can show the system dialog, do that. Otherwise, we must guide them to settings.
        boolean mustGoToSettings = !shouldShowRationalInnerDialog() && getHasDeniedPermission();
        showPermissionDialog(mustGoToSettings);
    }

    private void showPermissionDialog(boolean forceSettings) {
        Activity activity = mActivityRef.get();
        if (activity == null) return;
        
        final CustomMaterialDialog dialog = new CustomMaterialDialog(activity);
        dialog.setTitle(activity.getString(R.string.permission_rational_notification_title));
        dialog.setMessage(activity.getString(R.string.permission_rational_notification_message));
        dialog.setPositiveButton(activity.getString(R.string.button_sure), (currentDialog, buttonIndex) -> {
            if (forceSettings) {
                openNotificationAppSettings(activity);
            } else {
                requestPermission();
            }
        });
        dialog.setNegativeButton(activity.getString(R.string.button_not_now), null);
        dialog.show();
    }

    private void requestPermission() {
        Activity activity = mActivityRef.get();
        if (activity == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionManager.askForPermission(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PermissionManager.REQUEST_PERMISSION_CODE_POST_NOTIFICATIONS);
        }
    }

    private void showRationaleDialog() {
        Activity activity = mActivityRef.get();
        if (activity == null) return;
        
        final CustomMaterialDialog dialog = new CustomMaterialDialog(activity);
        dialog.setTitle(activity.getString(R.string.permission_rational_notification_title));
        dialog.setMessage(activity.getString(R.string.permission_rational_notification_message));
        dialog.setPositiveButton(activity.getString(R.string.button_sure), this);
        dialog.setNegativeButton(activity.getString(R.string.button_not_now), this);
        dialog.setNeutralButton(activity.getString(R.string.button_never), this);
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE ->
                    setButtonClickedBeforeDismissed(DialogInterface.BUTTON_POSITIVE);
            case DialogInterface.BUTTON_NEGATIVE ->
                    setButtonClickedBeforeDismissed(DialogInterface.BUTTON_NEGATIVE);
            case DialogInterface.BUTTON_NEUTRAL ->
                    setButtonClickedBeforeDismissed(DialogInterface.BUTTON_NEUTRAL);
        }
    }

    private Integer getButtonClickedBeforeDismissed() {
        return lastClickedButton;
    }

    private void setButtonClickedBeforeDismissed(Integer buttonClickedBeforeDismissed) {
        this.lastClickedButton = buttonClickedBeforeDismissed;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        final Integer buttonClickedBeforeDismissed = getButtonClickedBeforeDismissed();
        final boolean hasAReferenceToLastButtonClicked = (null != buttonClickedBeforeDismissed);
        if (hasAReferenceToLastButtonClicked) {
            if (DialogInterface.BUTTON_NEUTRAL == buttonClickedBeforeDismissed) {
                setDoNotShowRationaleAgain();
            } else if (DialogInterface.BUTTON_POSITIVE == buttonClickedBeforeDismissed) {
                requestPermission();
            }
        }
        setButtonClickedBeforeDismissed(null);
    }

    private void increaseRefuseNumber() {
        Activity activity = mActivityRef.get();
        if (activity == null) return;
        final int countRefusedSoFar = getCountRefusedSoFar();
        SharedPreferencesManager.getInstance(activity).setInt(PREF_KEY_REFUSE_COUNT, countRefusedSoFar + 1);
    }

    private int getCountRefusedSoFar() {
        Activity activity = mActivityRef.get();
        if (activity == null) return 0;
        return SharedPreferencesManager.getInstance(activity).getInt(PREF_KEY_REFUSE_COUNT, 0);
    }

    private void setDoNotShowRationaleAgain() {
        Activity activity = mActivityRef.get();
        if (activity == null) return;
        SharedPreferencesManager.getInstance(activity).setBoolean(PREF_KEY_DO_NOT_SHOW_RATIONALE, true);
    }

    public void setHasGrantedPermission(final boolean granted) {
        Activity activity = mActivityRef.get();
        if (activity == null) return;
        if (!granted) {
            SharedPreferencesManager.getInstance(activity).setBoolean(PREF_KEY_HAS_DENIED_NOTIFICATION_PERMISSION, true);
        }
    }

    private boolean getHasDeniedPermission() {
        Activity activity = mActivityRef.get();
        if (activity == null) return false;
        return SharedPreferencesManager.getInstance(activity).getBoolean(PREF_KEY_HAS_DENIED_NOTIFICATION_PERMISSION, false);
    }

    /**
     * Opens the system application details settings screen.
     */
    public void openNotificationAppSettings(final Context context) {
        if (context == null) return;
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(intent);
    }

    public static void postInventoryAlert(Context context, String medName, String message) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        
        Intent intent = new Intent(context, MainActivity.class);
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(context, 1001, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_med_pill)
                .setContentTitle(context.getString(R.string.inventory_low_stock_notif_title, medName))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        try {
            nm.notify(medName.hashCode(), builder.build());
        } catch (SecurityException ignored) {}
    }
}
