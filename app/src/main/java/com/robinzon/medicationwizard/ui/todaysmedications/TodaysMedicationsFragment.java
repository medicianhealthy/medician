package com.robinzon.medicationwizard.ui.todaysmedications;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButton;
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

/**
 * The primary dashboard fragment of the application. 
 * <p>
 * This fragment displays all medication doses scheduled for the current calendar day. 
 * It provides the core user interface for daily health management, allowing users to:
 * - Mark doses as taken or skipped.
 * - Reschedule future doses using a Material Time Picker.
 * - Sort the daily list by name, scheduled time, or actual action time.
 * - Undo accidental actions via a snackbar.
 * </p>
 * <p>
 * It uses {@link TodaysMedicationsViewModel} for reactive data fetching from the Room database.
 * </p>
 */
public class TodaysMedicationsFragment extends MedicationWizardFragment {

    private FragmentTodaysMedicationsBinding mBinding;
    private TodaysMedicationsViewModel mViewModel;
    private MedicationsAdapter mAdapter;
    
    private final Handler mInactivityHandler = new Handler(Looper.getMainLooper());
    private Runnable mInactivityRunnable;
    private ValueAnimator mLightningAnimator;

    /**
     * Initializes the binding and ViewModel for the fragment.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(TodaysMedicationsViewModel.class);
        mBinding = FragmentTodaysMedicationsBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    /**
     * Sets up the UI components after the view has been created.
     * Logic includes RecyclerView initialization, ChipGroup sorting listeners, 
     * and observing the LiveData from the ViewModel.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        com.robinzon.medicationwizard.utils.Logger.log("TodaysMedicationsFragment", "onViewCreated");
        setPaddingForRecyclerView(mBinding.recyclerView);
        setPaddingForRecyclerView(mBinding.emptyLayout.emptyStateContainer);
        setupSwipeRefresh();
        setupEmptyView();
        setupRecyclerView();
        setupSortChips();

        // Reactive observation: UI updates automatically when DB changes
        mViewModel.getTodaysMedications().observe(getViewLifecycleOwner(), instances -> {
            mAdapter.setMedications(instances);
            updateUiState(instances.isEmpty());
            mBinding.swipeRefresh.setRefreshing(false);
            
            // Engagement: Recalculate health streak on data changes
            updateStreakBadge();
        });
    }

    /**
     * Calculates the current health streak and updates the UI badge.
     * <p>
     * Performance: Uses StreakManager's background calculation to ensure 
     * no UI stutter during database queries.
     * </p>
     */
    private void updateStreakBadge() {
        com.robinzon.medicationwizard.utils.StreakManager.calculateCurrentStreak(requireContext(), streakCount -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (streakCount >= 2) {
                    mBinding.cardStreak.setVisibility(View.VISIBLE);
                    mBinding.txtStreak.setText(getString(R.string.streak_format, streakCount));
                } else {
                    mBinding.cardStreak.setVisibility(View.GONE);
                }
            });
        });
    }

    /**
     * Configures the M3 ChipGroup to handle list sorting.
     * Tapping a chip triggers a ViewModel re-query with a different SortOrder.
     */
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

    /**
     * Initializes the RecyclerView with a custom Adapter and action listeners.
     */
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
        
        int columns = getResources().getInteger(R.integer.medication_grid_columns);
        if (columns > 1) {
            mBinding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), columns));
        } else {
            mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        mBinding.recyclerView.setAdapter(mAdapter);
    }

    /**
     * Updates the status of a specific dose in the database.
     * It also records the timestamp of the action for history tracking and 
     * displays an "Undo" snackbar.
     *
     * @param instance The dose record to update.
     * @param status   The new status (TAKEN, SKIPPED, or SCHEDULED).
     */
    private void updateInstanceStatus(DoseInstanceEntity instance, String status) {
        instance.setStatus(status);
        // Record the time of the action for the "Took at HH:mm" summary
        instance.setActionTime(System.currentTimeMillis());
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getDatabase(requireContext()).doseInstanceDao().update(instance);
        });

        // Track achievements for In-App Review eligibility
        com.robinzon.medicationwizard.utils.Statisticator.incrementDosesLogged(requireContext());
        
        Snackbar.make(mBinding.getRoot(), instance.getMedicationName() + " marked as " + status.toLowerCase(), Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> {
                    // Revert status and clear action time
                    instance.setStatus("SCHEDULED");
                    instance.setActionTime(0);
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase.getDatabase(requireContext()).doseInstanceDao().update(instance);
                    });
                }).show();
        
        // Monetization: Show interstitial ad after completing a task (Take/Skip)
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).getAdsManager().showInterstitialAd();
            
            // Satisfaction Check: Ask for review after logging a dose if user is happy
            final android.app.Activity activity = getActivity();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                com.robinzon.medicationwizard.utils.ReviewManager.requestReviewIfEligible(activity);
            }, 1000L);
        }
    }

    /**
     * Displays a Material Time Picker to allow the user to change the 
     * scheduled time for a single dose.
     *
     * @param instance The dose record to reschedule.
     */
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
        
        if (isEmpty) {
            startEmptyStateAnimations(mBinding.getRoot());
            startLightningLogic();
        } else {
            stopEmptyStateAnimations();
            stopLightningLogic();
        }

        // Hide/Show FAB based on empty state for cleaner M3 aesthetics
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFabVisible(!isEmpty);
        }
    }

    private void startLightningLogic() {
        MaterialButton actionButton = mBinding.emptyLayout.btnEmptyAction;
        
        mInactivityRunnable = () -> {
            if (mLightningAnimator == null) {
                mLightningAnimator = ValueAnimator.ofInt(0, 10, 0);
                mLightningAnimator.setDuration(1500);
                mLightningAnimator.setRepeatCount(3);
                mLightningAnimator.addUpdateListener(animation -> 
                    actionButton.setStrokeWidth((int) animation.getAnimatedValue()));

                ObjectAnimator mascotAnim = ObjectAnimator.ofFloat(mBinding.emptyLayout.emptyMascot, "rotation", 0f, 10f, -10f, 0f);
                mascotAnim.setDuration(1000);

                AnimatorSet set = new AnimatorSet();
                set.playTogether(mLightningAnimator, mascotAnim);
                set.start();
                
                mInactivityHandler.postDelayed(mInactivityRunnable, 15000);
            }
        };
        mInactivityHandler.postDelayed(mInactivityRunnable, 10000);
    }

    private void stopLightningLogic() {
        mInactivityHandler.removeCallbacksAndMessages(null);
        if (mLightningAnimator != null) mLightningAnimator.cancel();
        mLightningAnimator = null;
    }

    /**
     * Configures the pull-to-refresh behavior.
     */
    private void setupSwipeRefresh() {
        mBinding.swipeRefresh.setOnRefreshListener(() -> {
            // LiveData handles refresh automatically when DB changes, 
            // so we just stop the animation immediately.
            mBinding.swipeRefresh.setRefreshing(false);
        });
    }

    /**
     * Binds the action button in the empty state view to open the add medication flow.
     * <p>
     * Performance: Starts breathing animation for UI engagement when empty.
     * </p>
     */
    private void setupEmptyView() {
        mBinding.emptyLayout.btnEmptyAction.setOnClickListener(v -> {
            com.robinzon.medicationwizard.utils.Logger.log("TodaysMedicationsFragment", "Empty state action clicked");
            AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "AddMedBottomSheet");
        });
    }

    /**
     * Cleans up the binding and stops animations to avoid memory leaks.
     */
    @Override
    public void onDestroyView() {
        stopEmptyStateAnimations();
        stopLightningLogic();
        super.onDestroyView();
        mBinding = null;
    }
}