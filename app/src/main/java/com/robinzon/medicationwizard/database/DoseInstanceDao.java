package com.robinzon.medicationwizard.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DoseInstanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DoseInstanceEntity instance);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DoseInstanceEntity> instances);

    @Update
    void update(DoseInstanceEntity instance);

    @Query("SELECT * FROM dose_instances ORDER BY scheduledTime ASC")
    LiveData<List<DoseInstanceEntity>> getAllInstances();

    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY scheduledTime ASC")
    LiveData<List<DoseInstanceEntity>> getInstancesInRangeSortedByTime(long startTime, long endTime);

    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY medicationName ASC")
    LiveData<List<DoseInstanceEntity>> getInstancesInRangeSortedByName(long startTime, long endTime);

    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY actionTime DESC")
    LiveData<List<DoseInstanceEntity>> getInstancesInRangeSortedByActionTime(long startTime, long endTime);

    @Query("SELECT * FROM dose_instances WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime")
    List<DoseInstanceEntity> getInstancesInRangeInternal(long startTime, long endTime);

    @Query("DELETE FROM dose_instances WHERE medicationId = :medicationId")
    void deleteByMedicationId(String medicationId);

    @Query("DELETE FROM dose_instances")
    void deleteAll();
}