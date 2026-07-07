package com.robinzon.medicationwizard.ui.todaysmedications;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

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

    private static final String PREF_SORT_ORDER = "todays_medications_sort_order";

    /** Supported sorting strategies for the daily list. */
    public enum SortOrder {
        /** Chronological order by scheduled time (earliest first). */
        TIME, 
        /** Alphabetical order by medication name. */
        NAME, 
        /** Chronological order by when the action was performed (latest first). */
        ACTION_TIME
    }

    /** Observable sort preference. */
    private final MutableLiveData<SortOrder> mSortOrder;
    
    /** Trigger for manual data refresh (e.g., when returning from background or adding a med). */
    private final MutableLiveData<Long> mRefreshTrigger = new MutableLiveData<>(System.currentTimeMillis());
    
    /** Reactive stream of medication instances for today. */
    private final LiveData<List<DoseInstanceEntity>> mTodaysMedications;

    /**
     * Initializes the ViewModel and sets up the reactive query chain.
     */
    public TodaysMedicationsViewModel(@NonNull Application application) {
        super(application);

        String savedOrder = SharedPreferencesManager.getInstance(application).getString(PREF_SORT_ORDER, SortOrder.TIME.name());
        mSortOrder = new MutableLiveData<>(SortOrder.valueOf(savedOrder));

        // Combine SortOrder and RefreshTrigger to create the final data stream.
        // This ensures that either a sort change OR a manual refresh (which updates the time window)
        // will trigger a fresh database query.
        LiveData<Pair<SortOrder, Long>> combinedTrigger = new MediatorLiveData<>();
        ((MediatorLiveData<Pair<SortOrder, Long>>) combinedTrigger).addSource(mSortOrder, order -> 
            ((MediatorLiveData<Pair<SortOrder, Long>>) combinedTrigger).setValue(new Pair<>(order, mRefreshTrigger.getValue())));
        ((MediatorLiveData<Pair<SortOrder, Long>>) combinedTrigger).addSource(mRefreshTrigger, time -> 
            ((MediatorLiveData<Pair<SortOrder, Long>>) combinedTrigger).setValue(new Pair<>(mSortOrder.getValue(), time)));

        mTodaysMedications = Transformations.switchMap(combinedTrigger, trigger -> {
            SortOrder order = trigger.first;
            
            // FIX: Use a fresh Calendar and ensure we cover the entire day regardless of timezone edge cases.
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

            com.robinzon.medicationwizard.utils.Logger.log("TodaysMedicationsViewModel", 
                "Querying for Today: " + new java.util.Date(startTime) + " to " + new java.util.Date(endTime));

            if (order == null) order = SortOrder.TIME;
            
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
     * Forces a recalculation of the current day's bounds and re-queries the database.
     */
    public void refresh() {
        mRefreshTrigger.setValue(System.currentTimeMillis());
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
        SharedPreferencesManager.getInstance(getApplication()).setString(PREF_SORT_ORDER, order.name());
    }

    /**
     * @return The current sort order.
     */
    public SortOrder getSortOrder() {
        return mSortOrder.getValue();
    }

    /** Helper class for combining triggers. */
    private static class Pair<A, B> {
        final A first;
        final B second;
        Pair(A first, B second) { this.first = first; this.second = second; }
    }
}