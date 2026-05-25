package com.robinzon.medicationwizard.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.databinding.FragmentHistoryBinding;
import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.entities.MedicationWizardFragment;
import com.robinzon.medicationwizard.ui.todaysmedications.MedicationsAdapter;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * A fragment providing a historical log of medication doses.
 * <p>
 * This screen features a {@link android.widget.CalendarView} that allows the user 
 * to select any past or future date. Upon selection, it displays a list of all 
 * medication instances for that specific day, including their completion status 
 * (Taken, Skipped, etc.).
 * </p>
 */
public class HistoryFragment extends MedicationWizardFragment {

    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private MedicationsAdapter adapter;

    /**
     * Initializes the data binding and the {@link HistoryViewModel}.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Configures the view components once they are ready.
     * Hides the Main FAB to focus on history logs, and sets up the observer 
     * for the filtered history list.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Hide FAB on History screen for a focused reading experience
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFabVisible(false);
        }

        setupRecyclerView();
        setupCalendar();

        // Observe history for the selected date
        viewModel.getHistory().observe(getViewLifecycleOwner(), instances -> {
            adapter.setMedications(instances);
            boolean isEmpty = instances == null || instances.isEmpty();
            binding.emptyLayout.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });
    }

    /**
     * Initializes the RecyclerView with the standard {@link MedicationsAdapter}.
     * Reuses the same action logic as the Today's Medications screen.
     */
    private void setupRecyclerView() {
        adapter = new MedicationsAdapter(new ArrayList<>());
        adapter.setOnMedicationActionListener(new MedicationsAdapter.OnMedicationActionListener() {
            @Override
            public void onTake(DoseInstanceEntity instance, int position) {
                updateStatus(instance, "TAKEN");
            }

            @Override
            public void onSkip(DoseInstanceEntity instance, int position) {
                updateStatus(instance, "SKIPPED");
            }

            @Override
            public void onReschedule(DoseInstanceEntity instance, int position) {
                // Future expansion: Reschedule logic for history records
            }

            @Override
            public void onUntake(DoseInstanceEntity instance, int position) {
                updateStatus(instance, "SCHEDULED");
            }
        });
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(adapter);
    }

    /**
     * Updates the status of a specific dose in the database and manages 
     * action timestamps for historical accuracy.
     *
     * @param instance The entity to update.
     * @param status   The new status (TAKEN, SKIPPED, etc.).
     */
    private void updateStatus(DoseInstanceEntity instance, String status) {
        instance.setStatus(status);
        if ("TAKEN".equals(status)) {
            instance.setActionTime(System.currentTimeMillis());
        } else if ("SCHEDULED".equals(status)) {
            instance.setActionTime(0); // Reset if user marks as "Un-take"
        }
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getDatabase(requireContext()).doseInstanceDao().update(instance);
        });
    }

    /**
     * Connects the CalendarView listener to the ViewModel.
     * When a user clicks a day, the ViewModel is notified to refresh the data range.
     */
    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            viewModel.selectDate(cal.getTimeInMillis());
        });
    }

    /**
     * Nullifies binding to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}