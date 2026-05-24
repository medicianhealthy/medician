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

public class TodaysMedicationsViewModel extends AndroidViewModel {

    public enum SortOrder {
        TIME, NAME, ACTION_TIME
    }

    private final MutableLiveData<SortOrder> mSortOrder = new MutableLiveData<>(SortOrder.TIME);
    private final LiveData<List<DoseInstanceEntity>> mTodaysMedications;

    public TodaysMedicationsViewModel(@NonNull Application application) {
        super(application);
        
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

    public LiveData<List<DoseInstanceEntity>> getTodaysMedications() {
        return mTodaysMedications;
    }

    public void setSortOrder(SortOrder order) {
        mSortOrder.setValue(order);
    }
}
