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

    /**
     * Key used to store and retrieve the backup version in the JSON object.
     */
    private static final String KEY_VERSION = "version";
    /**
     * Key used to store and retrieve the medications array in the JSON object.
     */
    private static final String KEY_MEDICATIONS = "medications";
    /**
     * Key used to store and retrieve the dose history array in the JSON object.
     */
    private static final String KEY_HISTORY = "history";
    /**
     * The current version of the backup format to handle future migrations.
     */
    private static final int CURRENT_BACKUP_VERSION = 1;

    /**
     * Exports all medication and dose history data to a specified URI in JSON format.
     * <p>
     * This method executes asynchronously on a database write executor to prevent blocking the UI thread.
     * It gathers data from SharedPreferences (for medications) and Room database (for history).
     * </p>
     *
     * @param context  The application or activity context. Must not be null.
     * @param uri      The URI where the backup JSON file should be written. Must not be null.
     * @param callback The callback to notify upon success or failure of the backup process. Must not be null.
     */
    public static void createBackup(Context context, Uri uri, BackupCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                JSONObject backup = new JSONObject();
                backup.put(KEY_VERSION, CURRENT_BACKUP_VERSION);

                // 1. Export Medications (from SharedPreferences)
                JSONArray medsArray = SharedPreferencesManager.getInstance(context).getJsonArray(Medication.PREF_MEDICATION_LIST, null);
                backup.put(KEY_MEDICATIONS, medsArray != null ? medsArray : new JSONArray());

                // 2. Export History (from Room)
                List<DoseInstanceEntity> history = AppDatabase.getDatabase(context).doseInstanceDao().getAllInstancesInternal();
                JSONArray historyArray = new JSONArray();
                for (DoseInstanceEntity entity : history) {
                    historyArray.put(entity.toJson());
                }
                backup.put(KEY_HISTORY, historyArray);

                // 3. Write to file
                OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    outputStream.write(backup.toString(2).getBytes());
                    outputStream.close();
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
     * Restores medication and dose history data from a specified JSON file URI.
     * <p>
     * WARNING: This operation wipes all currently stored medications and history to ensure a clean slate before importing.
     * The method executes asynchronously. Upon successful restoration, it reschedules all alarms for future doses.
     * </p>
     *
     * @param context  The application or activity context. Must not be null.
     * @param uri      The URI of the backup JSON file to read from. Must not be null.
     * @param callback The callback to notify upon success or failure of the restore process. Must not be null.
     */
    public static void restoreBackup(Context context, Uri uri, BackupCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // 1. Read file
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    callback.onComplete(false, "Failed to open input stream.");
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                inputStream.close();

                JSONObject backup = new JSONObject(stringBuilder.toString());
                // int version = backup.optInt(KEY_VERSION, 0);

                // 2. Clear current data (Synchronous wipe ensures clean slate for import)
                Medication.clearAllMedicationsInternal(context);

                // 3. Restore Medications
                JSONArray medsArray = backup.optJSONArray(KEY_MEDICATIONS);
                if (medsArray != null) {
                    SharedPreferencesManager.getInstance(context).setJsonArray(Medication.PREF_MEDICATION_LIST, medsArray);
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
                long now = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();
                List<DoseInstanceEntity> futureDoses = AppDatabase.getDatabase(context).doseInstanceDao().getInstancesInRangeInternal(now, now + (com.robinzon.medicationwizard.AppConfig.NUMBER_OF_DAYS_TO_SCHEDULE * 24 * 60 * 60 * 1000L));
                for (DoseInstanceEntity doseInstance : futureDoses) {
                    ReminderManager.scheduleReminder(context, doseInstance);
                }

                callback.onComplete(true, "Restore complete! Alarms have been reset.");
            } catch (Exception e) {
                Logger.log("BackupManager", "Restore failed: " + e.getMessage());
                callback.onComplete(false, "Restore failed: " + e.getMessage());
            }
        });
    }

    /**
     * Interface definition for a callback to be invoked when a backup or restore operation completes.
     */
    public interface BackupCallback {
        /**
         * Called when the backup or restore operation finishes.
         *
         * @param success True if the operation was successful, false otherwise.
         * @param message A descriptive message about the result (e.g., success message or error details).
         */
        void onComplete(boolean success, String message);
    }
}
