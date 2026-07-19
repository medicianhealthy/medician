package com.robinzon.medicationwizard.ui.todaysmedications;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.databinding.FragmentTodaysMedicationsBinding;
import com.robinzon.medicationwizard.entities.MedicationWizardFragment;
import com.robinzon.medicationwizard.ui.AddMedicationBottomSheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The primary dashboard fragment of the application.
 */
public class TodaysMedicationsFragment extends MedicationWizardFragment {

    private final Handler mInactivityHandler = new Handler(Looper.getMainLooper());
    private FragmentTodaysMedicationsBinding mBinding;
    private TodaysMedicationsViewModel mViewModel;
    private MedicationsAdapter mAdapter;
    private TodaysMedicationsViewModel.SortOrder mCurrentSortOrder;
    private Runnable mInactivityRunnable;
    private ValueAnimator mLightningAnimator;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(TodaysMedicationsViewModel.class);
        mBinding = FragmentTodaysMedicationsBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCurrentSortOrder = mViewModel.getSortOrder();

        if (mBinding != null) {
            if (mBinding.recyclerView != null) setPaddingForRecyclerView(mBinding.recyclerView);
            if (mBinding.emptyLayout != null && mBinding.emptyLayout.emptyStateContainer != null) {
                setPaddingForRecyclerView(mBinding.emptyLayout.emptyStateContainer);
            }
        }

        setupSwipeRefresh();
        setupEmptyView();
        setupRecyclerView();
        setupSortChips();
        syncSortUi();
        setupDataObservation();
    }

    private void syncSortUi() {
        if (mBinding == null || mCurrentSortOrder == null) return;
        int chipId = switch (mCurrentSortOrder) {
            case TIME -> R.id.chip_sort_time;
            case NAME -> R.id.chip_sort_name;
            case ACTION_TIME -> R.id.chip_sort_action;
        };
        mBinding.chipGroupSort.check(chipId);
    }

    private void setupDataObservation() {
        mViewModel.getTodaysMedications().observe(getViewLifecycleOwner(), instances -> {
            if (mBinding == null) return;
            List<DoseItem> grouped = groupDoses(instances, mCurrentSortOrder);
            mAdapter.setData(grouped);
            
            // Logic: Show sort chips only if there are more than 1 record
            boolean showSort = instances != null && instances.size() > 1;
            mBinding.scrollChips.setVisibility(showSort ? View.VISIBLE : View.GONE);
            if (mBinding.txtSortHint != null) {
                mBinding.txtSortHint.setVisibility(showSort ? View.VISIBLE : View.GONE);
            }

            updateUiState(instances == null || instances.isEmpty());
            mBinding.swipeRefresh.setRefreshing(false);

            updateTodayStats(instances);
            updateStreakBadge();
            updateDailyTip();
        });
    }

    private void updateTodayStats(List<DoseInstanceEntity> instances) {
        if (mBinding == null) return;

        // Use direct find to handle tablet landscape vs other layouts
        View progressCard = mBinding.getRoot().findViewById(R.id.card_today_progress);
        if (progressCard == null) return;

        if (instances == null || instances.isEmpty()) {
            progressCard.setVisibility(View.GONE);
            return;
        }

        progressCard.setVisibility(View.VISIBLE);
        int total = instances.size();
        int taken = 0;
        for (DoseInstanceEntity e : instances) {
            if ("TAKEN".equals(e.getStatus())) taken++;
        }

        com.google.android.material.progressindicator.CircularProgressIndicator progress =
                mBinding.getRoot().findViewById(R.id.progress_today);
        TextView summary = mBinding.getRoot().findViewById(R.id.txt_progress_summary);

        if (progress != null) {
            progress.setMax(total);
            progress.setProgress(taken, true);
        }
        if (summary != null) {
            summary.setText(getString(R.string.history_doses_format, taken, total));
        }
    }

    private void updateDailyTip() {
        if (mBinding == null) return;
        // Search in hierarchy because view might be nested or from include
        TextView tipContent = mBinding.getRoot().findViewById(R.id.history_tip_content);
        if (tipContent == null) return;

        int[] tips = {R.string.history_tip_1, R.string.history_tip_2, R.string.history_tip_3, R.string.history_tip_4, R.string.history_tip_5, R.string.history_tip_6};
        int dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR);
        tipContent.setText(tips[dayOfYear % tips.length]);
    }

    private void updateStreakBadge() {
        if (mBinding == null) return;
        com.robinzon.medicationwizard.utils.StreakManager.calculateCurrentStreak(requireContext(), streakCount -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (mBinding == null) return;

                // Re-lookup text view to ensure we have the live one (especially for tablets)
                TextView streakText = mBinding.getRoot().findViewById(R.id.txtStreak);
                if (streakText == null) return;

                boolean isTablet = getResources().getBoolean(R.bool.is_tablet);
                if (isTablet) {
                    if (streakCount >= 1) {
                        mBinding.cardStreak.setVisibility(View.VISIBLE);
                        streakText.setText(getString(R.string.streak_format, streakCount));
                    } else {
                        // Hide when 0 to avoid confusing the user with placeholders
                        mBinding.cardStreak.setVisibility(View.GONE);
                    }
                } else {
                    if (streakCount >= 2) {
                        mBinding.cardStreak.setVisibility(View.VISIBLE);
                        streakText.setText(getString(R.string.streak_format, streakCount));
                    } else {
                        mBinding.cardStreak.setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.refresh();
        if (mViewModel.getTodaysMedications().getValue() != null) {
            updateUiState(mViewModel.getTodaysMedications().getValue().isEmpty());
        }
    }

    private List<DoseItem> groupDoses(List<DoseInstanceEntity> instances, TodaysMedicationsViewModel.SortOrder sortOrder) {
        if (sortOrder != TodaysMedicationsViewModel.SortOrder.TIME || instances == null) {
            List<DoseItem> result = new ArrayList<>();
            if (instances != null) {
                for (DoseInstanceEntity e : instances) result.add(new DoseItem.Single(e));
            }
            return result;
        }
        Map<Long, List<DoseInstanceEntity>> groupedMap = new LinkedHashMap<>();
        for (DoseInstanceEntity e : instances) {
            long time = e.getScheduledTime();
            List<DoseInstanceEntity> group = groupedMap.computeIfAbsent(time, k -> new ArrayList<>());
            group.add(e);
        }
        List<DoseItem> result = new ArrayList<>();
        for (List<DoseInstanceEntity> group : groupedMap.values()) {
            if (group.size() > 1) result.add(new DoseItem.Group(group));
            else result.add(new DoseItem.Single(group.get(0)));
        }
        return result;
    }

    private void setupSortChips() {
        mBinding.chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_sort_time)
                mCurrentSortOrder = TodaysMedicationsViewModel.SortOrder.TIME;
            else if (checkedId == R.id.chip_sort_name)
                mCurrentSortOrder = TodaysMedicationsViewModel.SortOrder.NAME;
            else if (checkedId == R.id.chip_sort_action)
                mCurrentSortOrder = TodaysMedicationsViewModel.SortOrder.ACTION_TIME;
            mViewModel.setSortOrder(mCurrentSortOrder);
        });
    }

    private void setupRecyclerView() {
        mAdapter = new MedicationsAdapter(new ArrayList<>());
        mAdapter.setOnMedicationActionListener(new MedicationsAdapter.OnMedicationActionListener() {
            @Override
            public void onTake(DoseInstanceEntity i, int p) {
                updateInstanceStatus(i, "TAKEN");
            }

            @Override
            public void onSkip(DoseInstanceEntity i, int p) {
                updateInstanceStatus(i, "SKIPPED");
            }

            @Override
            public void onReschedule(DoseInstanceEntity i, int p) {
                showReschedulePicker(i);
            }

            @Override
            public void onUntake(DoseInstanceEntity i, int p) {
                updateInstanceStatus(i, "SCHEDULED");
            }

            @Override
            public void onUnskip(DoseInstanceEntity i, int p) {
                updateInstanceStatus(i, "SCHEDULED");
            }

            @Override
            public void onTakeGroup(List<DoseInstanceEntity> d, int p) {
                // Find the first med that requires timing clarification
                DoseInstanceEntity repMed = null;
                for (DoseInstanceEntity x : d) {
                    if (isTimingClarificationRequired(x)) {
                        repMed = x;
                        break;
                    }
                }

                if (repMed != null) {
                    final boolean[] success = {false};
                    // Show confirmation for the whole group, representing with the problematic one
                    checkAndClarifyTakeTiming(repMed, () -> {
                        for (DoseInstanceEntity x : d) applyStatusUpdate(x, "TAKEN");
                        success[0] = true;
                    }, dialog -> {
                        if (success[0]) {
                            // Add group interaction score once
                            if (getActivity() instanceof MainActivity main) {
                                main.addInteractionScore(2.0f);
                            }
                            triggerAdForGroupAction(d.size());
                        }
                    });
                } else {
                    for (DoseInstanceEntity x : d) applyStatusUpdate(x, "TAKEN");
                    if (getActivity() instanceof MainActivity main) {
                        main.addInteractionScore(2.0f);
                    }
                    triggerAdForGroupAction(d.size());
                }
            }

            @Override
            public void onSkipGroup(List<DoseInstanceEntity> d, int p) {
                for (DoseInstanceEntity x : d) applyStatusUpdate(x, "SKIPPED");
                if (getActivity() instanceof MainActivity main) {
                    main.addInteractionScore(1.0f);
                }
                triggerAdForGroupAction(d.size());
            }

            @Override
            public void onRescheduleGroup(List<DoseInstanceEntity> d, int p) {
                showGroupReschedulePicker(d);
            }

            @Override
            public void onUntakeGroup(List<DoseInstanceEntity> d, int p) {
                for (DoseInstanceEntity x : d) applyStatusUpdate(x, "SCHEDULED");
                triggerAdForGroupAction(d.size());
            }

            @Override
            public void onUnskipGroup(List<DoseInstanceEntity> d, int p) {
                for (DoseInstanceEntity x : d) applyStatusUpdate(x, "SCHEDULED");
                triggerAdForGroupAction(d.size());
            }
        });
        int cols = getResources().getInteger(R.integer.medication_grid_columns);
        mBinding.recyclerView.setLayoutManager(cols > 1 ? new GridLayoutManager(requireContext(), cols) : new LinearLayoutManager(requireContext()));
        mBinding.recyclerView.setAdapter(mAdapter);
    }

    private boolean isTimingClarificationRequired(DoseInstanceEntity instance) {
        long now = System.currentTimeMillis();
        long scheduled = instance.getScheduledTime();
        long diffMins = (now - scheduled) / 60000;

        com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager rcm =
                com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance();

        int defaultEarly = rcm.getEarlyTakeThresholdMins();
        int defaultLate = rcm.getLateTakeThresholdMins();
        if (defaultEarly <= 0) defaultEarly = 60;
        if (defaultLate <= 0) defaultLate = 180;

        int earlyThreshold = defaultEarly;
        int lateThreshold = defaultLate;

        boolean unlocked = com.robinzon.medicationwizard.AppConfig.isFeatureUnlocked(requireContext(), com.robinzon.medicationwizard.AppConfig.FeaturePassType.DOSE_WINDOW);
        if (unlocked) {
            com.robinzon.medicationwizard.utils.SharedPreferencesManager sp = com.robinzon.medicationwizard.utils.SharedPreferencesManager.getInstance(requireContext());
            earlyThreshold = sp.getInt(com.robinzon.medicationwizard.ui.settings.SettingsViewModel.KEY_CUSTOM_EARLY_THRESHOLD, defaultEarly);
            lateThreshold = sp.getInt(com.robinzon.medicationwizard.ui.settings.SettingsViewModel.KEY_CUSTOM_LATE_THRESHOLD, defaultLate);
        }

        return diffMins < -earlyThreshold || diffMins > lateThreshold;
    }

    private void updateInstanceStatus(DoseInstanceEntity instance, String status) {
        if ("TAKEN".equals(status)) {
            final boolean[] success = {false};
            checkAndClarifyTakeTiming(instance, () -> {
                applyStatusUpdate(instance, status);
                success[0] = true;
            }, dialog -> {
                if (success[0]) {
                    if (getActivity() instanceof MainActivity main) {
                        main.addInteractionScore(1.5f);
                    }
                    triggerAdIfEligible();
                }
            });
        } else {
            applyStatusUpdate(instance, status);
            if (getActivity() instanceof MainActivity main) {
                main.addInteractionScore(1.5f);
            }
            triggerAdIfEligible();
        }
    }

    private void applyStatusUpdate(DoseInstanceEntity instance, String status) {
        instance.setStatus(status);
        if (instance.getActionTime() <= 0) {
            instance.setActionTime(System.currentTimeMillis());
        }
        final Context appContext = requireContext().getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getDatabase(appContext).doseInstanceDao().update(instance);
            if (!"SCHEDULED".equals(status)) {
                com.robinzon.medicationwizard.reminders.ReminderManager.cancelReminder(appContext, instance.getId());
            }
        });
        com.robinzon.medicationwizard.utils.Statisticator.incrementDosesLogged(appContext);
        String localizedStatus = status;
        if ("TAKEN".equals(status)) localizedStatus = getString(R.string.take);
        else if ("SKIPPED".equals(status)) localizedStatus = getString(R.string.button_skip);

        Snackbar.make(mBinding.getRoot(), getString(R.string.medication_status_format, instance.getMedicationName(), localizedStatus), Snackbar.LENGTH_LONG)
                .setAction(R.string.button_undo, v -> {
                    instance.setStatus("SCHEDULED");
                    instance.setActionTime(0);
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase.getDatabase(appContext).doseInstanceDao().update(instance);
                        com.robinzon.medicationwizard.reminders.ReminderManager.scheduleReminder(appContext, instance);
                    });
                }).show();
        requestReviewIfEligible();
    }

    private void requestReviewIfEligible() {
        final android.app.Activity activity = getActivity();
        if (activity != null)
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> com.robinzon.medicationwizard.utils.ReviewManager.requestReviewIfEligible(activity), 1000L);
    }

    private void triggerAdIfEligible() {
        if (getActivity() instanceof MainActivity main) {
            if (com.robinzon.medicationwizard.utils.Statisticator.incrementActionsAndCheckAdEligibility(requireContext()))
                main.getAdsManager().showInterstitialAdWithCooldownOnly();
        }
    }

    private void triggerAdForGroupAction(int groupSize) {
        if (getActivity() instanceof MainActivity main) {
            int threshold = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance().getActionsPerInterstitial();
            if (groupSize >= threshold) main.getAdsManager().showInterstitialAdWithCooldownOnly();
        }
    }

    private void showReschedulePicker(DoseInstanceEntity instance) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setHour(12).setMinute(0).setTitleText(getString(R.string.button_reschedule) + " " + instance.getMedicationName()).build();
        final Context appContext = requireContext().getApplicationContext();
        picker.addOnPositiveButtonClickListener(v -> {
            java.util.Calendar now = java.util.Calendar.getInstance();
            java.util.Calendar target = java.util.Calendar.getInstance();
            target.set(java.util.Calendar.HOUR_OF_DAY, picker.getHour());
            target.set(java.util.Calendar.MINUTE, picker.getMinute());
            target.set(java.util.Calendar.SECOND, 0);
            target.set(java.util.Calendar.MILLISECOND, 0);
            if (target.before(now)) target.add(java.util.Calendar.DAY_OF_YEAR, 1);
            com.robinzon.medicationwizard.reminders.ReminderManager.cancelReminder(appContext, instance.getId());
            instance.setScheduledTime(target.getTimeInMillis());
            instance.setStatus("SCHEDULED");
            instance.setActionTime(0);
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase.getDatabase(appContext).doseInstanceDao().update(instance);
                com.robinzon.medicationwizard.reminders.ReminderManager.scheduleReminder(appContext, instance);
            });
            Snackbar.make(mBinding.getRoot(), getString(R.string.medication_status_format, instance.getMedicationName(), getString(R.string.button_reschedule)), Snackbar.LENGTH_SHORT).show();
        });
        picker.show(getChildFragmentManager(), "reschedule");
    }

    private void showGroupReschedulePicker(List<DoseInstanceEntity> doses) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setHour(12).setMinute(0).setTitleText(R.string.button_reschedule_all).build();
        final Context appContext = requireContext().getApplicationContext();
        picker.addOnPositiveButtonClickListener(v -> {
            java.util.Calendar now = java.util.Calendar.getInstance();
            java.util.Calendar target = java.util.Calendar.getInstance();
            target.set(java.util.Calendar.HOUR_OF_DAY, picker.getHour());
            target.set(java.util.Calendar.MINUTE, picker.getMinute());
            target.set(java.util.Calendar.SECOND, 0);
            target.set(java.util.Calendar.MILLISECOND, 0);
            if (target.before(now)) target.add(java.util.Calendar.DAY_OF_YEAR, 1);
            for (DoseInstanceEntity d : doses) {
                com.robinzon.medicationwizard.reminders.ReminderManager.cancelReminder(appContext, d.getId());
                d.setScheduledTime(target.getTimeInMillis());
                d.setStatus("SCHEDULED");
                d.setActionTime(0);
            }
            AppDatabase.databaseWriteExecutor.execute(() -> {
                for (DoseInstanceEntity d : doses) {
                    AppDatabase.getDatabase(appContext).doseInstanceDao().update(d);
                    com.robinzon.medicationwizard.reminders.ReminderManager.scheduleReminder(appContext, d);
                }
            });
            Snackbar.make(mBinding.getRoot(), R.string.button_reschedule_all, Snackbar.LENGTH_SHORT).show();
        });
        picker.show(getChildFragmentManager(), "reschedule_group");
    }

    private void updateUiState(boolean isEmpty) {
        if (mBinding == null) return;
        if (mBinding.emptyLayout != null && mBinding.emptyLayout.emptyView != null)
            mBinding.emptyLayout.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (mBinding.recyclerView != null)
            mBinding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        boolean hasAnyMeds = com.robinzon.medicationwizard.entities.Medication.hasMedications(requireContext());
        if (isEmpty && mBinding.emptyLayout != null) {
            if (hasAnyMeds) {
                if (mBinding.emptyLayout.emptyTitle != null)
                    mBinding.emptyLayout.emptyTitle.setText(R.string.history_empty);
                if (mBinding.emptyLayout.emptySubtitle != null)
                    mBinding.emptyLayout.emptySubtitle.setText(R.string.history_empty_subtitle);
                if (mBinding.emptyLayout.btnEmptyAction != null)
                    mBinding.emptyLayout.btnEmptyAction.setVisibility(View.GONE);
            } else {
                if (mBinding.emptyLayout.emptyTitle != null)
                    mBinding.emptyLayout.emptyTitle.setText(R.string.empty_meds_title);
                if (mBinding.emptyLayout.emptySubtitle != null)
                    mBinding.emptyLayout.emptySubtitle.setText(R.string.empty_meds_subtitle);
                if (mBinding.emptyLayout.btnEmptyAction != null)
                    mBinding.emptyLayout.btnEmptyAction.setVisibility(View.VISIBLE);
            }
            startEmptyStateAnimations(mBinding.getRoot());
            triggerScrollHintCheck(mBinding.emptyLayout.emptyScrollView, mBinding.emptyLayout.emptyScrollHint, "hint_seen_today");
            startLightningLogic();
        } else {
            stopEmptyStateAnimations();
            stopLightningLogic();
        }
        if (getActivity() instanceof MainActivity main) main.setFabVisible(!isEmpty || hasAnyMeds);
    }

    private final List<AnimatorSet> mLightningAnimators = new ArrayList<>();

    private void startLightningLogic() {
        MaterialButton actionButton = mBinding.emptyLayout.btnEmptyAction;
        mInactivityRunnable = () -> {
            if (mBinding == null || !isAdded()) return;

            mLightningAnimator = ValueAnimator.ofInt(0, 10, 0);
            mLightningAnimator.setDuration(1500);
            mLightningAnimator.setRepeatCount(3);
            mLightningAnimator.addUpdateListener(animation -> {
                if (mBinding != null) actionButton.setStrokeWidth((int) animation.getAnimatedValue());
            });

            ObjectAnimator mascotAnim = ObjectAnimator.ofFloat(mBinding.emptyLayout.emptyMascot, "rotation", 0f, 10f, -10f, 0f);
            mascotAnim.setDuration(1000);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(mLightningAnimator, mascotAnim);
            set.start();
            mLightningAnimators.add(set);

            mInactivityHandler.postDelayed(mInactivityRunnable, 15000);
        };
        mInactivityHandler.postDelayed(mInactivityRunnable, 10000);
    }

    private void stopLightningLogic() {
        mInactivityHandler.removeCallbacksAndMessages(null);
        for (AnimatorSet set : mLightningAnimators) {
            set.cancel();
        }
        mLightningAnimators.clear();
        if (mLightningAnimator != null) mLightningAnimator.cancel();
        mLightningAnimator = null;
    }

    private void setupSwipeRefresh() {
        mBinding.swipeRefresh.setOnRefreshListener(() -> mBinding.swipeRefresh.setRefreshing(false));
    }

    private void setupEmptyView() {
        mBinding.emptyLayout.btnEmptyAction.setOnClickListener(v -> {
            if (getActivity() != null) {
                AddMedicationBottomSheet bs = new AddMedicationBottomSheet();
                bs.show(getChildFragmentManager(), "AddMedBottomSheet");
            }
        });
    }

    @Override
    public void onDestroyView() {
        stopEmptyStateAnimations();
        stopLightningLogic();
        super.onDestroyView();
        mBinding = null;
    }
}
