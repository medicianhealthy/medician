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

/**
 * ViewModel for the Medications List screen.
 * <p>
 * This class manages the state for the full medication library. It implements 
 * {@link SharedPreferences.OnSharedPreferenceChangeListener} to provide 
 * real-time UI updates whenever the underlying medication list is modified 
 * from anywhere in the app (e.g., adding or deleting a med).
 * </p>
 */
public class MedicationsListViewModel extends AndroidViewModel implements SharedPreferences.OnSharedPreferenceChangeListener {

    /** Observable list of all defined medications. */
    private final MutableLiveData<List<Medication>> mMedications = new MutableLiveData<>();

    /**
     * Initializes the ViewModel, loads the initial list, and registers for 
     * real-time data changes.
     */
    public MedicationsListViewModel(@NonNull Application application) {
        super(application);
        refreshMedications();
        SharedPreferencesManager.getInstance(application).registerListener(this);
    }

    /**
     * @return Observable stream of the full medication list.
     */
    public LiveData<List<Medication>> getMedications() {
        return mMedications;
    }

    /**
     * Forces a fresh reload of medications from SharedPreferences.
     * <p>
     * Performance: Runs parsing on a background thread to prevent UI stutter 
     * when the medication list grows large.
     * </p>
     */
    public void refreshMedications() {
        new Thread(() -> {
            List<Medication> list = Medication.getSavedMedications(getApplication());
            mMedications.postValue(list);
        }).start();
    }

    /**
     * Listener callback triggered by SharedPreferences changes. 
     * If the medication list key is modified, it automatically refreshes the UI.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Medication.PREF_MEDICATION_LIST.equals(key)) {
            refreshMedications();
        }
    }

    /**
     * Standard cleanup to prevent memory leaks by unregistering the data listener.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        SharedPreferencesManager.getInstance(getApplication()).unregisterListener(this);
    }
}