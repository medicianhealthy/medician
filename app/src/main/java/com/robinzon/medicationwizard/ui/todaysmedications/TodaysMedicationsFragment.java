package com.robinzon.medicationwizard.ui.todaysmedications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.databinding.FragmentTodaysMedicationsBinding;
import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.entities.MedicationWizardFragment;
import com.robinzon.medicationwizard.ui.AddMedicationBottomSheet;

import java.util.ArrayList;

public class TodaysMedicationsFragment extends MedicationWizardFragment {

    private FragmentTodaysMedicationsBinding mBinding;
    private TodaysMedicationsViewModel mViewModel;
    private MedicationsAdapter mAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(TodaysMedicationsViewModel.class);
        mBinding = FragmentTodaysMedicationsBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setPaddingForRecyclerView(mBinding.recyclerView);
        setupSwipeRefresh();
        setupEmptyView();
        setupRecyclerView();
        setupSortChips();

        mViewModel.getTodaysMedications().observe(getViewLifecycleOwner(), instances -> {
            mAdapter.setMedications(instances);
            updateUiState(instances.isEmpty());
            mBinding.swipeRefresh.setRefreshing(false);
        });
    }

    private void setupSortChips() {
        mBinding.chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_sort_time) {
                mViewModel.setSortOrder(TodaysMedicationsViewModel.SortOrder.TIME);
            } else if (checkedId == R.id.chip_sort_name) {
                mViewModel.setSortOrder(TodaysMedicationsViewModel.SortOrder.NAME);
            } else if (checkedId == R.id.chip_sort_action) {
                mViewModel.setSortOrder(TodaysMedicationsViewModel.SortOrder.ACTION_TIME);
            }
        });
    }

    private void setupRecyclerView() {
        mAdapter = new MedicationsAdapter(new ArrayList<>());
        mAdapter.setOnMedicationActionListener(new MedicationsAdapter.OnMedicationActionListener() {
            @Override
            public void onTake(DoseInstanceEntity instance, int position) {
                updateInstanceStatus(instance, "TAKEN");
            }

            @Override
            public void onSkip(DoseInstanceEntity instance, int position) {
                updateInstanceStatus(instance, "SKIPPED");
            }

            @Override
            public void onReschedule(DoseInstanceEntity instance, int position) {
                showReschedulePicker(instance);
            }

            @Override
            public void onUntake(DoseInstanceEntity instance, int position) {
                updateInstanceStatus(instance, "SCHEDULED");
            }
        });
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.recyclerView.setAdapter(mAdapter);
    }

    private void updateInstanceStatus(DoseInstanceEntity instance, String status) {
        instance.setStatus(status);
        instance.setActionTime(System.currentTimeMillis());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getDatabase(requireContext()).doseInstanceDao().update(instance);
        });
        
        Snackbar.make(mBinding.getRoot(), instance.getMedicationName() + " marked as " + status.toLowerCase(), Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> {
                    instance.setStatus("SCHEDULED");
                    instance.setActionTime(0);
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase.getDatabase(requireContext()).doseInstanceDao().update(instance);
                    });
                }).show();
    }

    private void showReschedulePicker(DoseInstanceEntity instance) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setTitleText("Reschedule " + instance.getMedicationName())
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(instance.getScheduledTime());
            cal.set(java.util.Calendar.HOUR_OF_DAY, picker.getHour());
            cal.set(java.util.Calendar.MINUTE, picker.getMinute());
            
            instance.setScheduledTime(cal.getTimeInMillis());
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase.getDatabase(requireContext()).doseInstanceDao().update(instance);
            });
        });

        picker.show(getChildFragmentManager(), "reschedule");
    }

    private void updateUiState(boolean isEmpty) {
        mBinding.emptyLayout.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mBinding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFabVisible(!isEmpty);
        }
    }

    private void setupSwipeRefresh() {
        mBinding.swipeRefresh.setOnRefreshListener(() -> {
            // LiveData handles refresh automatically when DB changes
            mBinding.swipeRefresh.setRefreshing(false);
        });
    }

    private void setupEmptyView() {
        mBinding.emptyLayout.btnEmptyAction.setOnClickListener(v -> {
            AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "AddMedBottomSheet");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }
}