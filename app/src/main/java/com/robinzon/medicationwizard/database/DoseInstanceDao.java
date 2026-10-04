package com.robinzon.medicationwizard.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object (DAO) providing the API for all database operations on {@link DoseInstanceEntity}.
 * <p>
 * This interface defines the SQL queries and interactions for scheduled medication instances.
 * It supports reactive UI updates via {@link LiveData} for daily schedules and history.
 * </p>
 */
@Dao
public interface DoseInstanceDao {

    /**
     * Inserts a single dose instance into the database.
     * If a record with the same ID already exists, it will be replaced.
     *
     * @param instance The entity to persist. Must not be null.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DoseInstanceEntity instance);

    /**
     * Inserts multiple dose instances (e.g., a week's worth of schedules).
     *
     * @param instances The list of entities to persist. Must not be null.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DoseInstanceEntity> instances);

    /**
     * Updates an existing dose instance (e.g., when a user marks a med as taken).
     *
     * @param instance The entity with updated status or action time. Must not be null.
     */
    @Update
    void update(DoseInstanceEntity instance);

    /**
     * Deletes a specific dose instance.
     *
     * @param instance The entity to remove. Must not be null.
     */
    @androidx.room.Delete
    void deleteInstanceInternal(DoseInstanceEntity instance);

    /**
     * Retrieves a specific dose instance by its database identifier.
     *
     * @param instanceId The unique ID of the dose instance.
     * @return Returns the matching {@link DoseInstanceEntity}, or null if not found.
     */
    @Query("SELECT * FROM dose_instances WHERE id = :instanceId")
    DoseInstanceEntity getInstanceById(int instanceId);

    /**
     * Updates the metadata for all dose instances associated with a specific medication.
     * This ensures that changes to a medication's definition are reflected in its scheduled doses.
     *
     * @param id The unique ID of the parent medication.
     * @param name The new name of the medication.
     * @param amount The new dose amount.
     * @param strength The new strength value.
     * @param unit The new measurement unit.
     * @param form The new physical form.
     * @param instruction The new instruction details.
     * @param imagePath The new file path to the medication image.
     */
    @Query("UPDATE dose_instances SET medicationName = :name, amount = :amount, strength = :strength, unit = :unit, form = :form, instruction = :instruction, imagePath = :imagePath WHERE medicationId = :id")
    void updateMetadataForAllDoses(String id, String name, float amount, float strength, String unit, String form, String instruction, String imagePath);

    /**
     * Retrieves all dose instances from the database, ordered chronologically by scheduled time.
     *
     * @return Returns an observable {@link LiveData} list containing all historical and future dose instances.
     */
    @Query("SELECT * FROM dose_instances ORDER BY scheduledTime ASC")
    LiveData<List<DoseInstanceEntity>> getAllInstances();

    /**
     * Retrieves all dose instances from the database synchronously, ordered chronologically by scheduled time.
     *
     * @return Returns a standard list containing all historical and future dose instances.
     */
    @Query("SELECT * FROM dose_instances ORDER BY scheduledTime ASC")
    List<DoseInstanceEntity> getAllInstancesInternal();

    /**
     * Retrieves a single dose instance for a specific medication at a precise scheduled time.
     *
     * @param medicationId The unique ID of the parent medication.
     * @param time The scheduled time in epoch milliseconds.
     * @return Returns the matching {@link DoseInstanceEntity}, or null if none exists.
     */
    @Query("SELECT * FROM dose_instances WHERE medicationId = :medicationId AND scheduledTime = :time LIMIT 1")
    DoseInstanceEntity getInstanceByTime(String medicationId, long time);

    /**
     * Retrieves the most recently taken dose instance for a specific medication.
     *
     * @param medId The unique ID of the parent medication.
     * @return Returns the latest taken {@link DoseInstanceEntity}, or null if none has been taken yet.
     */
    @Query("SELECT * FROM dose_instances WHERE medicationId = :medId AND status = 'TAKEN' ORDER BY actionTime DESC LIMIT 1")
    DoseInstanceEntity getLatestTakenInstance(String medId);

    /**
     * Deletes a specific dose instance from the database by its ID.
     *
     * @param id The unique identifier of the dose instance to delete.
     */
    @Query("DELETE FROM dose_instances WHERE id = :id")
    void deleteByIdInternal(int id);

    /**
     * Retrieves medications scheduled for a specific time window, sorted by schedule time.
     * Used primarily for the "Today's Medications" and "History" views.
     *
     * @param startTime Start of range (epoch milliseconds).
     * @param endTime End of range (epoch milliseconds).
     * @return Returns an observable {@link LiveData} list of instances in the window.
     */
    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY scheduledTime ASC")
    LiveData<List<DoseInstanceEntity>> getInstancesInRangeSortedByTime(long startTime, long endTime);

    /**
     * Retrieves medications scheduled for a specific time window, sorted alphabetically by name.
     *
     * @param startTime Start of range (epoch milliseconds).
     * @param endTime End of range (epoch milliseconds).
     * @return Returns an observable {@link LiveData} list of instances in the window.
     */
    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY medicationName ASC")
    LiveData<List<DoseInstanceEntity>> getInstancesInRangeSortedByName(long startTime, long endTime);

    /**
     * Retrieves medications in a specific time window, sorted by the time the action was performed (latest first).
     *
     * @param startTime Start of range (epoch milliseconds).
     * @param endTime End of range (epoch milliseconds).
     * @return Returns an observable {@link LiveData} list of instances in the window.
     */
    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY actionTime DESC")
    LiveData<List<DoseInstanceEntity>> getInstancesInRangeSortedByActionTime(long startTime, long endTime);

    /**
     * Synchronously retrieves medications scheduled for a specific time window.
     * Useful for background alarm scheduling or boot-time re-scheduling where LiveData is not appropriate.
     *
     * @param startTime Start of range (epoch milliseconds).
     * @param endTime End of range (epoch milliseconds).
     * @return Returns the list of instances in the window.
     */
    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime")
    List<DoseInstanceEntity> getInstancesInRangeInternal(long startTime, long endTime);

    /**
     * Retrieves the count of doses that were NOT taken within a specific day.
     * Used by the StreakManager to determine if a day was "Perfect".
     *
     * @param startTime Start of day (epoch milliseconds).
     * @param endTime End of day (epoch milliseconds).
     * @return Returns the count of doses with a status other than 'TAKEN'.
     */
    @Query("SELECT COUNT(*) FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime AND status != 'TAKEN'")
    int getUnfinishedDosesCount(long startTime, long endTime);

    /**
     * Deletes all future and past instances for a specific medication.
     * Called when a user deletes a medication definition from the main list.
     *
     * @param medicationId The ID of the medication to purge.
     */
    @Query("DELETE FROM dose_instances WHERE medicationId = :medicationId")
    void deleteByMedicationId(String medicationId);

    /**
     * Wipes all records from the dose_instances table.
     * Part of the "Start Fresh" safety feature in settings.
     */
    @Query("DELETE FROM dose_instances")
    void deleteAll();

    /**
     * Deletes only the pending (SCHEDULED) doses for a specific medication.
     * Used when refreshing definitions to ensure pending tasks match the latest edits.
     *
     * @param medicationId The ID of the medication to target.
     */
    @Query("DELETE FROM dose_instances WHERE medicationId = :medicationId AND status = 'SCHEDULED'")
    void deleteScheduledByMedicationId(String medicationId);

    /**
     * Retrieves all pending dose instances for a specific medication.
     *
     * @param medicationId The unique ID of the parent medication.
     * @return Returns a list of pending {@link DoseInstanceEntity} records.
     */
    @Query("SELECT * FROM dose_instances WHERE medicationId = :medicationId AND status = 'SCHEDULED'")
    List<DoseInstanceEntity> getScheduledByMedicationId(String medicationId);

    /**
     * Retrieves all scheduled dose instances that are set for a specific time.
     *
     * @param time The scheduled time in epoch milliseconds.
     * @return Returns a list of matching pending {@link DoseInstanceEntity} records.
     */
    @Query("SELECT * FROM dose_instances WHERE scheduledTime = :time AND status = 'SCHEDULED'")
    List<DoseInstanceEntity> getScheduledAtTime(long time);

    /**
     * Retrieves a single dose instance for a specific medication within a given time window.
     *
     * @param medicationId The unique ID of the parent medication.
     * @param start The start of the time window in epoch milliseconds.
     * @param end The end of the time window in epoch milliseconds.
     * @return Returns the first found {@link DoseInstanceEntity} in the window, or null if none exist.
     */
    @Query("SELECT * FROM dose_instances WHERE medicationId = :medicationId AND scheduledTime >= :start AND scheduledTime <= :end LIMIT 1")
    DoseInstanceEntity getAnyInstanceInWindow(String medicationId, long start, long end);

    /**
     * Retrieves a single pending dose instance for a specific medication within a given day.
     *
     * @param medicationId The unique ID of the parent medication.
     * @param startOfDay The start of the day in epoch milliseconds.
     * @param endOfDay The end of the day in epoch milliseconds.
     * @return Returns a scheduled {@link DoseInstanceEntity} for the day, or null if none exist.
     */
    @Query("SELECT * FROM dose_instances WHERE medicationId = :medicationId AND scheduledTime >= :startOfDay AND scheduledTime <= :endOfDay AND status = 'SCHEDULED' LIMIT 1")
    DoseInstanceEntity getScheduledInstanceForDay(String medicationId, long startOfDay, long endOfDay);

    /**
     * Deletes dose instances that were scheduled before the given timestamp.
     * Used for periodic database maintenance to remove very old records.
     *
     * @param thresholdTime Cut-off time (epoch milliseconds).
     */
    @Query("DELETE FROM dose_instances WHERE scheduledTime < :thresholdTime")
    void deleteOldInstances(long thresholdTime);
}