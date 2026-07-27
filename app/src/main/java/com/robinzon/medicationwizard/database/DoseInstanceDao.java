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
     * @param instance The entity to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DoseInstanceEntity instance);

    /**
     * Inserts multiple dose instances (e.g., a week's worth of schedules).
     *
     * @param instances The list of entities to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DoseInstanceEntity> instances);

    /**
     * Updates an existing dose instance (e.g., when a user marks a med as taken).
     *
     * @param instance The entity with updated status or action time.
     */
    @Update
    void update(DoseInstanceEntity instance);

    /**
     * Deletes a specific dose instance.
     *
     * @param instance The entity to remove.
     */
    @androidx.room.Delete
    void deleteInstanceInternal(DoseInstanceEntity instance);

    /**
     * Returns every dose instance in the database, ordered chronologically.
     *
     * @return Observable list of all historical and future dose instances.
     */
    @Query("SELECT * FROM dose_instances WHERE id = :instanceId")
    DoseInstanceEntity getInstanceById(int instanceId);

    @Query("UPDATE dose_instances SET medicationName = :name, amount = :amount, strength = :strength, unit = :unit, form = :form, instruction = :instruction WHERE medicationId = :id")
    void updateMetadataForAllDoses(String id, String name, float amount, float strength, String unit, String form, String instruction);

    /**
     * @return Observable list of all historical and future dose instances.
     */
    @Query("SELECT * FROM dose_instances ORDER BY scheduledTime ASC")
    LiveData<List<DoseInstanceEntity>> getAllInstances();

    /**
     * Synchronously returns all dose instances.
     */
    @Query("SELECT * FROM dose_instances ORDER BY scheduledTime ASC")
    List<DoseInstanceEntity> getAllInstancesInternal();

    @Query("SELECT * FROM dose_instances WHERE medicationId = :medicationId AND scheduledTime = :time LIMIT 1")
    DoseInstanceEntity getInstanceByTime(String medicationId, long time);

    /**
     * Returns medications scheduled for a specific time window, sorted by schedule time.
     * Used primary for the "Today's Medications" and "History" views.
     *
     * @param startTime Start of range (epoch millis).
     * @param endTime   End of range (epoch millis).
     * @return Observable list of instances in the window.
     */
    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY scheduledTime ASC")
    LiveData<List<DoseInstanceEntity>> getInstancesInRangeSortedByTime(long startTime, long endTime);

    /**
     * Returns medications in a window sorted alphabetically by name.
     *
     * @param startTime Start of range (epoch millis).
     * @param endTime   End of range (epoch millis).
     * @return Observable list of instances in the window.
     */
    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY medicationName ASC")
    LiveData<List<DoseInstanceEntity>> getInstancesInRangeSortedByName(long startTime, long endTime);

    /**
     * Returns medications in a window sorted by the time the action was performed (latest first).
     *
     * @param startTime Start of range (epoch millis).
     * @param endTime   End of range (epoch millis).
     * @return Observable list of instances in the window.
     */
    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY actionTime DESC")
    LiveData<List<DoseInstanceEntity>> getInstancesInRangeSortedByActionTime(long startTime, long endTime);

    /**
     * Synchronous (blocking) version of range fetch. Useful for background alarm scheduling
     * or boot-time re-scheduling where LiveData is not appropriate.
     *
     * @param startTime Start of range (epoch millis).
     * @param endTime   End of range (epoch millis).
     * @return The list of instances in the window.
     */
    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime")
    List<DoseInstanceEntity> getInstancesInRangeInternal(long startTime, long endTime);

    /**
     * Returns the count of doses that were NOT taken for a specific day.
     * Used by the StreakManager to determine if a day was "Perfect".
     *
     * @param startTime Start of day (epoch millis).
     * @param endTime   End of day (epoch millis).
     * @return Count of doses with status other than 'TAKEN'.
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
     * Wipes the entire database table.
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

    @Query("SELECT * FROM dose_instances WHERE medicationId = :medicationId AND status = 'SCHEDULED'")
    List<DoseInstanceEntity> getScheduledByMedicationId(String medicationId);

    @Query("SELECT * FROM dose_instances WHERE scheduledTime = :time AND status = 'SCHEDULED'")
    List<DoseInstanceEntity> getScheduledAtTime(long time);

    @Query("SELECT * FROM dose_instances WHERE medicationId = :medicationId AND scheduledTime >= :start AND scheduledTime <= :end LIMIT 1")
    DoseInstanceEntity getAnyInstanceInWindow(String medicationId, long start, long end);

    @Query("SELECT * FROM dose_instances WHERE medicationId = :medicationId AND scheduledTime >= :startOfDay AND scheduledTime <= :endOfDay AND status = 'SCHEDULED' LIMIT 1")
    DoseInstanceEntity getScheduledInstanceForDay(String medicationId, long startOfDay, long endOfDay);

    /**
     * Deletes dose instances that were scheduled before the given timestamp.
     * Used for periodic database maintenance.
     *
     * @param thresholdTime Cut-off time (epoch millis).
     */
    @Query("DELETE FROM dose_instances WHERE scheduledTime < :thresholdTime")
    void deleteOldInstances(long thresholdTime);
}