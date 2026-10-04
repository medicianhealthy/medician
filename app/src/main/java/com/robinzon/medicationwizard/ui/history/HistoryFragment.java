package com.robinzon.medicationwizard.ui.history;

import android.content.Context;
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
 * Allows users to view and interact with doses logged on previous dates via a calendar interface.
 */
public class HistoryFragment extends MedicationWizardFragment {

    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private MedicationsAdapter adapter;

    /**
     * Initializes the ViewModel and inflates the layout using view binding.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root view of the inflated layout.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Configures the user interface elements and establishes data observation.
     * Hides the global floating action button and attempts to show an interstitial ad on creation.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
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

    /**
     * Toggles visibility between the empty state view and the historical data list.
     * Updates the empty state messaging based on whether the user has any medications defined globally.
     *
     * @param isEmpty   True if there are no dose records for the selected date.
     * @param instances The list of dose instances for the selected date.
     */
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

    /**
     * Groups a flat list of dose instances into a hierarchical list of DoseItem objects.
     * Doses sharing the exact same scheduled time are grouped together.
     *
     * @param instances The flat list of raw database entities.
     * @return A list of view-ready DoseItem objects (either Single or Group).
     */
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

    /**
     * Recalculates and updates the daily summary card indicating the completion percentage.
     *
     * @param instances The list of dose instances for the day.
     */
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

    /**
     * Initializes the RecyclerView adapter and establishes callback hooks for user interaction.
     * Handles state changes like take, skip, and undo.
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

    /**
     * Processes a user request to update the status of a single dose instance.
     * Triggers verification logic if the user is marking a dose as taken.
     *
     * @param instance The target dose instance.
     * @param status   The requested new status (e.g., "TAKEN", "SKIPPED", "SCHEDULED").
     */
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

    /**
     * Applies a state change to a dose instance and persists it to the local database.
     * Handles "as needed" (PRN) specific logic, such as physical deletion upon cancellation.
     *
     * @param instance The dose instance being modified.
     * @param status   The new status to apply.
     */
    private void applyStatusUpdate(DoseInstanceEntity instance, String status) {
        final Context appContext = requireContext().getApplicationContext();
        if ("SCHEDULED".equals(status) && instance.isPrn()) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(appContext);
                db.doseInstanceDao().deleteByIdInternal(instance.getId());
                
                // Update library stopwatch
                DoseInstanceEntity latest = db.doseInstanceDao().getLatestTakenInstance(instance.getMedicationId());
                List<com.robinzon.medicationwizard.entities.Medication> allMeds = 
                        com.robinzon.medicationwizard.entities.Medication.getSavedMedications(appContext);
                for (com.robinzon.medicationwizard.entities.Medication m : allMeds) {
                    if (m.getId().equals(instance.getMedicationId())) {
                        m.setLastTakenTimestamp(latest != null ? latest.getActionTime() : null);
                        m.addToMedicationList(appContext);
                        break;
                    }
                }
            });
            return;
        }

        instance.setStatus(status);
        if ("TAKEN".equals(status)) {
            if (instance.getActionTime() <= 0) {
                instance.setActionTime(com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal());
            }
        } else if ("SCHEDULED".equals(status)) {
            instance.setActionTime(0);
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getDatabase(appContext).doseInstanceDao().update(instance);
            if (!"SCHEDULED".equals(status)) {
                com.robinzon.medicationwizard.reminders.ReminderManager.cancelReminder(appContext, instance.getId());
            } else {
                com.robinzon.medicationwizard.reminders.ReminderManager.scheduleReminder(appContext, instance);
            }
        });
    }

    /**
     * Binds the date change listener to the CalendarView to update the ViewModel.
     */
    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            viewModel.selectDate(cal.getTimeInMillis());
        });
    }

    /**
     * Wires the action button on the empty state layout to open the "Add Medication" wizard.
     */
    private void setupEmptyView() {
        binding.emptyLayout.btnEmptyAction.setOnClickListener(v -> {
            AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "AddMedBottomSheet");
        });
    }

    /**
     * Cleans up view bindings to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
