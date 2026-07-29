package com.robinzon.medicationwizard.ui.history;

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
 * ViewModel for the History screen.
 * <p>
 * This class manages the state for the history view, specifically the currently
 * selected date. It uses {@link Transformations#switchMap} to automatically
 * re-query the Room database whenever the selected date changes, ensuring
 * the UI always displays the correct historical records.
 * </p>
 */
public class HistoryViewModel extends AndroidViewModel {

    /**
     * The date currently selected by the user in the calendar (epoch millis).
     */
    private final MutableLiveData<Long> mSelectedDate = new MutableLiveData<>(com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal());

    /**
     * Reactive stream of medication instances for the selected date.
     */
    private final LiveData<List<DoseInstanceEntity>> mHistory;

    /**
     * Initializes the ViewModel and sets up the reactive database query.
     */
    public HistoryViewModel(@NonNull Application application) {
        super(application);

        // Listen for date changes and return a new LiveData query from Room
        mHistory = Transformations.switchMap(mSelectedDate, date -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(date);

            // Define the start of the selected day (00:00:00.000)
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long start = cal.getTimeInMillis();

            // Define the end of the selected day (23:59:59.999)
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            long end = cal.getTimeInMillis();

            return AppDatabase.getDatabase(application).doseInstanceDao().getInstancesInRangeSortedByTime(start, end);
        });
    }

    /**
     * Updates the selected date, which triggers a fresh database query.
     *
     * @param millis The new date in epoch milliseconds.
     */
    public void selectDate(long millis) {
        mSelectedDate.setValue(millis);
    }

    /**
     * @return An observable list of medication instances for the current date selection.
     */
    public LiveData<List<DoseInstanceEntity>> getHistory() {
        return mHistory;
    }
}