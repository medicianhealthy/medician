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

public class HistoryViewModel extends AndroidViewModel {

    private final MutableLiveData<Long> mSelectedDate = new MutableLiveData<>(System.currentTimeMillis());
    private final LiveData<List<DoseInstanceEntity>> mHistory;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        mHistory = Transformations.switchMap(mSelectedDate, date -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long start = cal.getTimeInMillis();

            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            long end = cal.getTimeInMillis();

            return AppDatabase.getDatabase(application).doseInstanceDao().getInstancesInRangeSortedByTime(start, end);
        });
    }

    public void selectDate(long millis) {
        mSelectedDate.setValue(millis);
    }

    public LiveData<List<DoseInstanceEntity>> getHistory() {
        return mHistory;
    }
}