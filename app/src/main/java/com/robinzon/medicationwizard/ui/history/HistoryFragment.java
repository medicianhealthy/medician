package com.robinzon.medicationwizard.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.databinding.FragmentHistoryBinding;
import com.robinzon.medicationwizard.entities.MedicationWizardFragment;
import com.robinzon.medicationwizard.ui.AddMedicationBottomSheet;
import com.robinzon.medicationwizard.ui.todaysmedications.DoseItem;
import com.robinzon.medicationwizard.ui.todaysmedications.MedicationsAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A fragment providing a historical log of medication doses.
 */
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
            ((MainActivity) getActivity()).getAdsManager().showInterstitialAd();
        }

        setupRecyclerView();
        setupCalendar();
        setupEmptyView();

        applyCompactEmptyState(binding.getRoot());

        viewModel.getHistory().observe(getViewLifecycleOwner(), instances -> {
            List<DoseItem> grouped = groupDoses(instances);
            adapter.setData(grouped);

            boolean isEmpty = instances == null || instances.isEmpty();
            updateUiState(isEmpty, instances);
        });
    }

    private void updateUiState(boolean isEmpty, List<DoseInstanceEntity> instances) {
        binding.emptyLayout.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            boolean hasAnyMeds = com.robinzon.medicationwizard.entities.Medication.hasMedications(requireContext());
            if (hasAnyMeds) {
                // Show "No records for this day" state
                binding.emptyLayout.emptyTitle.setText(R.string.history_empty);
                binding.emptyLayout.emptySubtitle.setText(R.string.history_empty_subtitle);
                binding.emptyLayout.btnEmptyAction.setVisibility(View.GONE);
            } else {
                // Show "First med" state
                binding.emptyLayout.emptyTitle.setText(R.string.empty_meds_title);
                binding.emptyLayout.emptySubtitle.setText(R.string.empty_meds_subtitle);
                binding.emptyLayout.btnEmptyAction.setVisibility(View.VISIBLE);
            }

            startEmptyStateAnimations(binding.getRoot());
            triggerScrollHintCheck(binding.emptyLayout.emptyScrollView, binding.emptyLayout.emptyScrollHint, "hint_seen_history");
            binding.cardSummary.setVisibility(View.GONE);
        } else {
            stopEmptyStateAnimations();
            updateSummaryCard(instances);
        }
    }

    private List<DoseItem> groupDoses(List<DoseInstanceEntity> instances) {
        if (instances == null) return new ArrayList<>();
        Map<Long, List<DoseInstanceEntity>> groupedMap = new LinkedHashMap<>();
        for (DoseInstanceEntity e : instances) {
            long time = e.getScheduledTime();
            List<DoseInstanceEntity> group = groupedMap.get(time);
            if (group == null) {
                group = new ArrayList<>();
                groupedMap.put(time, group);
            }
            group.add(e);
        }
        List<DoseItem> result = new ArrayList<>();
        for (List<DoseInstanceEntity> group : groupedMap.values()) {
            if (group.size() > 1) result.add(new DoseItem.Group(group));
            else if (!group.isEmpty()) result.add(new DoseItem.Single(group.get(0)));
        }
        return result;
    }

    private void updateSummaryCard(List<DoseInstanceEntity> instances) {
        int total = instances.size();
        int taken = 0;
        for (DoseInstanceEntity e : instances) {
            if ("TAKEN".equals(e.getStatus())) taken++;
        }

        int percent = total > 0 ? (int) (((float) taken / total) * 100) : 0;

        binding.cardSummary.setVisibility(View.VISIBLE);
        binding.progressCompletion.setProgress(percent, true);
        binding.txtCompletionTitle.setText(getString(R.string.history_percent_format, percent));
        binding.txtCompletionSubtitle.setText(getString(R.string.history_doses_format, taken, total));

        if (percent == 100) {
            binding.progressCompletion.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary));
        } else {
            binding.progressCompletion.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_secondary));
        }
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
            }

            @Override
            public void onUntake(DoseInstanceEntity instance, int position) {
                updateStatus(instance, "SCHEDULED");
            }

            @Override
            public void onUnskip(DoseInstanceEntity instance, int position) {
                updateStatus(instance, "SCHEDULED");
            }

            @Override
            public void onTakeGroup(List<DoseInstanceEntity> doses, int position) {
                for (DoseInstanceEntity d : doses) applyStatusUpdate(d, "TAKEN");
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).addInteractionScore(2.0f);
                }
            }

            @Override
            public void onSkipGroup(List<DoseInstanceEntity> doses, int position) {
                for (DoseInstanceEntity d : doses) applyStatusUpdate(d, "SKIPPED");
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).addInteractionScore(1.0f);
                }
            }

            @Override
            public void onRescheduleGroup(List<DoseInstanceEntity> doses, int position) {
            }

            @Override
            public void onUntakeGroup(List<DoseInstanceEntity> doses, int position) {
                for (DoseInstanceEntity d : doses) applyStatusUpdate(d, "SCHEDULED");
            }

            @Override
            public void onUnskipGroup(List<DoseInstanceEntity> doses, int position) {
                for (DoseInstanceEntity d : doses) applyStatusUpdate(d, "SCHEDULED");
            }
        });
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(adapter);
    }

    private void updateStatus(DoseInstanceEntity instance, String status) {
        if ("TAKEN".equals(status)) {
            final boolean[] success = {false};
            checkAndClarifyTakeTiming(instance, () -> {
                applyStatusUpdate(instance, status);
                success[0] = true;
            }, dialog -> {
                if (success[0] && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).addInteractionScore(1.5f);
                }
            });
        } else {
            applyStatusUpdate(instance, status);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).addInteractionScore(1.5f);
            }
        }
    }

    private void applyStatusUpdate(DoseInstanceEntity instance, String status) {
        instance.setStatus(status);
        if ("TAKEN".equals(status)) {
            if (instance.getActionTime() <= 0) {
                instance.setActionTime(System.currentTimeMillis());
            }
        } else if ("SCHEDULED".equals(status)) {
            instance.setActionTime(0);
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getDatabase(requireContext()).doseInstanceDao().update(instance);
            if (!"SCHEDULED".equals(status)) {
                com.robinzon.medicationwizard.reminders.ReminderManager.cancelReminder(requireContext(), instance.getId());
            } else {
                com.robinzon.medicationwizard.reminders.ReminderManager.scheduleReminder(requireContext(), instance);
            }
        });
    }

    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            viewModel.selectDate(cal.getTimeInMillis());
        });
    }

    private void setupEmptyView() {
        binding.emptyLayout.btnEmptyAction.setOnClickListener(v -> {
            AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "AddMedBottomSheet");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
