package com.robinzon.medicationwizard.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;

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

public class HistoryFragment extends MedicationWizardFragment {

    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private MedicationsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFabVisible(false);
        }

        setupRecyclerView();
        setupCalendar();

        viewModel.getHistory().observe(getViewLifecycleOwner(), instances -> {
            adapter.setMedications(instances);
            boolean isEmpty = instances == null || instances.isEmpty();
            binding.emptyLayout.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });
    }

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
                // Future logic or snackbar
            }

            @Override
            public void onUntake(DoseInstanceEntity instance, int position) {
                updateStatus(instance, "SCHEDULED");
            }
        });
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(adapter);
    }

    private void updateStatus(DoseInstanceEntity instance, String status) {
        instance.setStatus(status);
        if ("TAKEN".equals(status)) {
            instance.setActionTime(System.currentTimeMillis());
        } else if ("SCHEDULED".equals(status)) {
            instance.setActionTime(0);
        }
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getDatabase(requireContext()).doseInstanceDao().update(instance);
        });
    }

    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            viewModel.selectDate(cal.getTimeInMillis());
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}