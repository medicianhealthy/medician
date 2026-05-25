package com.robinzon.medicationwizard.utils;

import android.content.Context;
import android.net.Uri;

import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.reminders.ReminderManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to handle data Backup and Restore.
 * <p>
 * It exports the Medication definitions and Dose History into a single JSON file.
 * Restoration involves wiping current data and re-importing from the JSON.
 * </p>
 */
public class BackupManager {

    private static final String KEY_VERSION = "version";
    private static final String KEY_MEDICATIONS = "medications";
    private static final String KEY_HISTORY = "history";
    private static final int CURRENT_BACKUP_VERSION = 1;

    public interface BackupCallback {
        void onComplete(boolean success, String message);
    }

    /**
     * Exports all data to the provided URI.
     */
    public static void createBackup(Context context, Uri uri, BackupCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                JSONObject backup = new JSONObject();
                backup.put(KEY_VERSION, CURRENT_BACKUP_VERSION);

                // 1. Export Medications (from SharedPreferences)
                JSONArray medsArray = SharedPreferencesManager.getInstance(context).getJsonArray(Medication.SPK_MEDICATION_LIST, null);
                backup.put(KEY_MEDICATIONS, medsArray != null ? medsArray : new JSONArray());

                // 2. Export History (from Room)
                List<DoseInstanceEntity> history = AppDatabase.getDatabase(context).doseInstanceDao().getAllInstancesInternal();
                JSONArray historyArray = new JSONArray();
                for (DoseInstanceEntity entity : history) {
                    historyArray.put(entity.toJson());
                }
                backup.put(KEY_HISTORY, historyArray);

                // 3. Write to file
                OutputStream os = context.getContentResolver().openOutputStream(uri);
                if (os != null) {
                    os.write(backup.toString(2).getBytes());
                    os.close();
                    callback.onComplete(true, "Backup created successfully!");
                } else {
                    callback.onComplete(false, "Failed to open output stream.");
                }
            } catch (Exception e) {
                Logger.log("BackupManager", "Backup failed: " + e.getMessage());
                callback.onComplete(false, "Backup failed: " + e.getMessage());
            }
        });
    }

    /**
     * Restores data from the provided URI.
     * WARNING: This wipes current data!
     */
    public static void restoreBackup(Context context, Uri uri, BackupCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // 1. Read file
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is == null) {
                    callback.onComplete(false, "Failed to open input stream.");
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                is.close();

                JSONObject backup = new JSONObject(sb.toString());
                // int version = backup.optInt(KEY_VERSION, 0);

                // 2. Clear current data
                Medication.clearAllMedications(context);

                // 3. Restore Medications
                JSONArray medsArray = backup.optJSONArray(KEY_MEDICATIONS);
                if (medsArray != null) {
                    SharedPreferencesManager.getInstance(context).setJsonArray(Medication.SPK_MEDICATION_LIST, medsArray);
                }

                // 4. Restore History
                JSONArray historyArray = backup.optJSONArray(KEY_HISTORY);
                if (historyArray != null) {
                    List<DoseInstanceEntity> entities = new ArrayList<>();
                    for (int i = 0; i < historyArray.length(); i++) {
                        DoseInstanceEntity entity = DoseInstanceEntity.fromJson(historyArray.getJSONObject(i));
                        if (entity != null) {
                            entities.add(entity);
                        }
                    }
                    if (!entities.isEmpty()) {
                        AppDatabase.getDatabase(context).doseInstanceDao().insertAll(entities);
                    }
                }

                // 5. Reschedule all alarms for future doses
                long now = System.currentTimeMillis();
                List<DoseInstanceEntity> future = AppDatabase.getDatabase(context).doseInstanceDao().getInstancesInRangeInternal(now, now + (30 * 24 * 60 * 60 * 1000L));
                for (DoseInstanceEntity e : future) {
                    ReminderManager.scheduleReminder(context, e);
                }

                callback.onComplete(true, "Restore complete! Alarms have been reset.");
            } catch (Exception e) {
                Logger.log("BackupManager", "Restore failed: " + e.getMessage());
                callback.onComplete(false, "Restore failed: " + e.getMessage());
            }
        });
    }
}
