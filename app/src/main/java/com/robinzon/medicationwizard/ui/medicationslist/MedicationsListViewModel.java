package com.robinzon.medicationwizard.ui.medicationslist;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.List;

public class MedicationsListViewModel extends AndroidViewModel implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final MutableLiveData<List<Medication>> mMedications = new MutableLiveData<>();

    public MedicationsListViewModel(@NonNull Application application) {
        super(application);
        refreshMedications();
        SharedPreferencesManager.getInstance(application).registerListener(this);
    }

    public LiveData<List<Medication>> getMedications() {
        return mMedications;
    }

    public void refreshMedications() {
        mMedications.setValue(Medication.getSavedMedications(getApplication()));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Medication.SPK_MEDICATION_LIST.equals(key)) {
            refreshMedications();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        SharedPreferencesManager.getInstance(getApplication()).unregisterListener(this);
    }
}