package com.robinzon.medicationwizard.ui.todaysmedications;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;

import java.util.Calendar;
import java.util.List;

/**
 * ViewModel for the "Today's Medications" dashboard.
 * <p>
 * This class provides a reactive stream of medication dose instances scheduled for 
 * the current calendar day. It maintains the user's preferred {@link SortOrder} 
 * and automatically triggers a fresh Room database query whenever the sort 
 * preference changes.
 * </p>
 */
public class TodaysMedicationsViewModel extends AndroidViewModel {

    /** Supported sorting strategies for the daily list. */
    public enum SortOrder {
        /** Chronological order by scheduled time (earliest first). */
        TIME, 
        /** Alphabetical order by medication name. */
        NAME, 
        /** Chronological order by when the action was performed (latest first). */
        ACTION_TIME
    }

    /** Observable sort preference, defaults to {@link SortOrder#TIME}. */
    private final MutableLiveData<SortOrder> mSortOrder = new MutableLiveData<>(SortOrder.TIME);
    
    /** Reactive stream of medication instances for today. */
    private final LiveData<List<DoseInstanceEntity>> mTodaysMedications;

    /**
     * Initializes the ViewModel and calculates the epoch window for "Today".
     */
    public TodaysMedicationsViewModel(@NonNull Application application) {
        super(application);
        
        // Define the bounds for the current day
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endTime = calendar.getTimeInMillis();

        // switchMap listens for changes in SortOrder and executes the corresponding DAO query
        mTodaysMedications = Transformations.switchMap(mSortOrder, order -> {
            switch (order) {
                case NAME:
                    return AppDatabase.getDatabase(application).doseInstanceDao().getInstancesInRangeSortedByName(startTime, endTime);
                case ACTION_TIME:
                    return AppDatabase.getDatabase(application).doseInstanceDao().getInstancesInRangeSortedByActionTime(startTime, endTime);
                case TIME:
                default:
                    return AppDatabase.getDatabase(application).doseInstanceDao().getInstancesInRangeSortedByTime(startTime, endTime);
            }
        });
    }

    /**
     * @return Observable list of medication instances for today, respecting the current sort order.
     */
    public LiveData<List<DoseInstanceEntity>> getTodaysMedications() {
        return mTodaysMedications;
    }

    /**
     * Updates the active sort order, triggering an immediate UI refresh.
     *
     * @param order The new sort strategy.
     */
    public void setSortOrder(SortOrder order) {
        mSortOrder.setValue(order);
    }
}