package com.robinzon.medicationwizard.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The main Room database class for the Medication Wizard.
 * <p>
 * This class follows the singleton pattern to ensure only one instance of the database 
 * is open at any time, which prevents data corruption and saves resources. 
 * It also manages a background thread pool via {@link #databaseWriteExecutor} to 
 * keep all heavy database operations off the Main UI Thread.
 * </p>
 */
@Database(entities = {DoseInstanceEntity.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * @return The DAO for interacting with dose instances.
     */
    /**
     * @return The Data Access Object for dose instances.
     */
    public abstract DoseInstanceDao doseInstanceDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    
    /**
     * A thread pool for performing asynchronous database operations.
     * All insert, update, and delete calls should be wrapped in this executor.
     */
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    /**
     * Retrieves the singleton database instance.
     *
     * @param context The application context.
     * @return The active AppDatabase instance.
     */
    /**
     * Retrieves the singleton database instance, creating it if necessary.
     *
     * @param context Application context.
     * @return The shared AppDatabase instance.
     */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "medication_wizard_db")
                            // Caution: destructive migration wipes data if version increments without a migration path.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}