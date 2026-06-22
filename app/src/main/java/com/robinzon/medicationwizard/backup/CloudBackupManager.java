package com.robinzon.medicationwizard.backup;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.NetworkUtils;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Orchestrates cloud backup and restore operations with Google Drive.
 */
public class CloudBackupManager {

    private static final String BACKUP_FILE_NAME = "medication_wizard_backup.json";
    private final Context mContext;
    private final DriveServiceHelper mDriveHelper;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    public CloudBackupManager(Context context, DriveServiceHelper driveHelper) {
        mContext = context.getApplicationContext();
        mDriveHelper = driveHelper;
    }

    /**
     * Performs a full backup of medications and history to Google Drive appDataFolder.
     */
    public Task<Void> backupToCloud() {
        if (!com.robinzon.medicationwizard.AppConfig.isPremium(mContext)) {
            Logger.log("CloudBackupManager", "Skipping backup: User is not premium");
            return Tasks.forResult(null);
        }
        if (!shouldPerformBackup()) {
            Logger.log("CloudBackupManager", "Skipping backup due to settings/network");
            return Tasks.forResult(null);
        }

        return Tasks.call(mExecutor, () -> {
            try {
                JSONObject backup = new JSONObject();
                backup.put("version", 1);
                backup.put("timestamp", System.currentTimeMillis());

                JSONArray medsArray = SharedPreferencesManager.getInstance(mContext).getJsonArray(Medication.PREF_MEDICATION_LIST, null);
                backup.put("medications", medsArray != null ? medsArray : new JSONArray());

                List<DoseInstanceEntity> history = AppDatabase.getDatabase(mContext).doseInstanceDao().getAllInstancesInternal();
                JSONArray historyArray = new JSONArray();
                for (DoseInstanceEntity entity : history) {
                    historyArray.put(entity.toJson());
                }
                backup.put("history", historyArray);

                Tasks.await(mDriveHelper.uploadFile(BACKUP_FILE_NAME, backup.toString(2)));
                Logger.log("CloudBackupManager", "Cloud backup successful");
            } catch (Exception e) {
                Logger.log("CloudBackupManager", "Cloud backup failed: " + e.getMessage());
                throw e;
            }
            return null;
        });
    }

    /**
     * Restores data from the cloud backup file on Google Drive.
     */
    public Task<Boolean> restoreFromCloud() {
        return Tasks.call(mExecutor, () -> {
            try {
                String fileId = mDriveHelper.findFileId(BACKUP_FILE_NAME);
                if (fileId == null) {
                    Logger.log("CloudBackupManager", "No backup file found in cloud");
                    return false;
                }

                String content = Tasks.await(mDriveHelper.downloadFile(fileId));
                JSONObject backup = new JSONObject(content);

                // 1. Restore medications to SharedPreferences
                if (backup.has("medications")) {
                    JSONArray medsArray = backup.getJSONArray("medications");
                    SharedPreferencesManager.getInstance(mContext).setJsonArray(Medication.PREF_MEDICATION_LIST, medsArray);
                }

                // 2. Restore history to Room Database
                if (backup.has("history")) {
                    JSONArray historyArray = backup.getJSONArray("history");
                    AppDatabase db = AppDatabase.getDatabase(mContext);
                    db.runInTransaction(() -> {
                        db.doseInstanceDao().deleteAll();
                        for (int i = 0; i < historyArray.length(); i++) {
                            try {
                                DoseInstanceEntity entity = DoseInstanceEntity.fromJson(historyArray.getJSONObject(i));
                                db.doseInstanceDao().insert(entity);
                            } catch (Exception ignored) {}
                        }
                    });
                }

                Logger.log("CloudBackupManager", "Restore from cloud successful");
                return true;
            } catch (Exception e) {
                Logger.log("CloudBackupManager", "Restore from cloud failed: " + e.getMessage());
                return false;
            }
        });
    }

    public Task<Void> deleteBackup() {
        return mDriveHelper.deleteFile(BACKUP_FILE_NAME);
    }

    private boolean shouldPerformBackup() {
        CloudBackupSettings settings = CloudBackupSettings.getInstance(mContext);
        if (!settings.isAutoBackupEnabled()) return false;

        if (settings.isWifiOnlyEnabled() && !NetworkUtils.isWifiConnected(mContext)) {
            return false;
        }

        return true;
    }
}
