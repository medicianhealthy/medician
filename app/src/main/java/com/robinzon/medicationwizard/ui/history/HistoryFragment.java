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
import com.robinzon.medicationwizard.ui.todaysmedications.MedicationsAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
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
            
            // Monetization: Show interstitial ad when entering History
            ((MainActivity) getActivity()).getAdsManager().showInterstitialAd();
        }

        setupRecyclerView();
        setupCalendar();
        setupEmptyView();
        
        // Performance: Adjust empty state layout for screen density and calendar overlap
        applyCompactEmptyState(binding.getRoot());

        // Observe history for the selected date
        viewModel.getHistory().observe(getViewLifecycleOwner(), instances -> {
            adapter.setMedications(instances);
            boolean isEmpty = instances == null || instances.isEmpty();
            binding.emptyLayout.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            
            if (isEmpty) {
                // Performance: Start animations only when the UI is in empty state
                startEmptyStateAnimations(binding.getRoot());
                binding.cardSummary.setVisibility(View.GONE);
            } else {
                stopEmptyStateAnimations();
                updateSummaryCard(instances);
            }
        });
    }

    /**
     * Calculates the daily performance metrics and updates the summary header.
     * <p>
     * Performance: Performs simple list iteration to calculate percentages 
     * without creating heavy intermediate objects.
     * </p>
     *
     * @param instances The list of medication instances for the day.
     */
    private void updateSummaryCard(List<DoseInstanceEntity> instances) {
        int total = instances.size();
        int taken = 0;
        for (DoseInstanceEntity e : instances) {
            if ("TAKEN".equals(e.getStatus())) taken++;
        }

        int percent = (int) (((float) taken / total) * 100);
        
        binding.cardSummary.setVisibility(View.VISIBLE);
        binding.progressCompletion.setProgress(percent, true);
        binding.txtCompletionTitle.setText(getString(R.string.history_percent_format, percent));
        binding.txtCompletionSubtitle.setText(getString(R.string.history_doses_format, taken, total));

        // Visual reward: Change progress color if 100% complete
        if (percent == 100) {
            binding.progressCompletion.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary));
        } else {
            binding.progressCompletion.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_secondary));
        }
    }

    /**
     * Initializes the RecyclerView with the standard {@link MedicationsAdapter}.
     * Reuses the same action logic as the Today's Medications screen.
     * <p>
     * Performance: Reuses the existing Adapter class to minimize binary size.
     * </p>
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

            @Override
            public void onUnskip(DoseInstanceEntity instance, int position) {
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
        if ("TAKEN".equals(status)) {
            checkAndClarifyTakeTiming(instance, () -> applyStatusUpdate(instance, status));
        } else {
            applyStatusUpdate(instance, status);
        }
    }

    private void applyStatusUpdate(DoseInstanceEntity instance, String status) {
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
     * Binds the action button in the empty state view to open the add medication flow.
     * <p>
     * Performance: Starts breathing animation for UI engagement when empty.
     * </p>
     */
    private void setupEmptyView() {
        binding.emptyLayout.btnEmptyAction.setOnClickListener(v -> {
            com.robinzon.medicationwizard.utils.Logger.log("HistoryFragment", "Empty state action clicked");
            AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "AddMedBottomSheet");
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