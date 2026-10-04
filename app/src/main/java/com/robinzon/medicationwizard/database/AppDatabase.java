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
@Database(entities = {DoseInstanceEntity.class}, version = 8, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * The number of threads used in the fixed thread pool for background database operations.
     */
    private static final int NUMBER_OF_THREADS = 4;

    /**
     * A thread pool for performing asynchronous database operations.
     * All insert, update, and delete calls should be wrapped in this executor.
     */
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    /**
     * The singleton instance of the application database.
     */
    private static volatile AppDatabase INSTANCE;

    /**
     * Retrieves the singleton database instance, creating it if necessary.
     *
     * @param context Application context used for building the database. Must not be null.
     * @return Returns the shared AppDatabase instance.
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

    /**
     * Provides the Data Access Object for dose instances.
     *
     * @return Returns the {@link DoseInstanceDao} used for database operations on dose instances.
     */
    public abstract DoseInstanceDao doseInstanceDao();
}