package com.robinzon.medicationwizard.entities;


import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.utils.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Base fragment class for the Medication Wizard project.
 * <p>
 * This class provides shared layout logic for all feature screens (Home, List, History).
 * Its primary responsibility is managing UI "clearance" to ensure that scrollable 
 * content (like {@link RecyclerView}) is not obscured by permanent UI elements 
 * like the bottom Ad banner or the Floating Action Button (FAB).
 * </p>
 */
public class MedicationWizardFragment extends Fragment {

    private final List<AnimatorSet> mActiveAnimators = new ArrayList<>();
    
    /**
     * Triggers the synchronized animations for empty states (Breathing button + Twinkling stars).
     * <p>
     * Performance: Animators are tracked in a list and automatically cleaned up in onDestroyView.
     * Checks if animations are already running to avoid redundant animator objects.
     * </p>
     *
     * @param root The root view of the fragment to search for animated subviews.
     */
    /**
     * Animates the empty state UI with breathing and mascot rotations.
     */
    protected void startEmptyStateAnimations(View root) {
        if (root == null || !mActiveAnimators.isEmpty()) return;
        
        MaterialButton actionButton = root.findViewById(R.id.btn_empty_action);
        if (actionButton == null) return;

        // 1. Breath Animation (Continuous scale pulse)
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(actionButton, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(actionButton, "scaleY", 1f, 1.05f, 1f);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setDuration(2500);
        scaleY.setDuration(2500);
        
        AnimatorSet breathSet = new AnimatorSet();
        breathSet.playTogether(scaleX, scaleY);
        breathSet.setInterpolator(new AccelerateDecelerateInterpolator());
        breathSet.start();
        mActiveAnimators.add(breathSet);

        // 2. Sparkling Stars (Twinkling effects)
        startTwinkleAnimation(root.findViewById(R.id.empty_sparkle_1), 0);
        startTwinkleAnimation(root.findViewById(R.id.empty_sparkle_2), 400);
        startTwinkleAnimation(root.findViewById(R.id.empty_sparkle_3), 800);
        startTwinkleAnimation(root.findViewById(R.id.empty_sparkle_4), 1200);
    }

    /**
     * Helper to start a twinkling animation for a background star.
     * <p>
     * Performance: Uses scale and alpha animations which are hardware-accelerated on Android.
     * </p>
     *
     * @param view  The star ImageView.
     * @param delay Animation start delay in ms to create a staggered effect.
     */
    private void startTwinkleAnimation(View view, long delay) {
        if (view == null) return;
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.2f, 1.0f, 0.2f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.7f, 1.2f, 0.7f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.7f, 1.2f, 0.7f);
        
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setDuration(2200);
        scaleX.setDuration(2200);
        scaleY.setDuration(2200);
        alpha.setStartDelay(delay);
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, scaleX, scaleY);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
        mActiveAnimators.add(set);
    }

    /**
     * Stops and clears all active animations to prevent memory leaks.
     */
    protected void stopEmptyStateAnimations() {
        for (AnimatorSet set : mActiveAnimators) {
            set.cancel();
        }
        mActiveAnimators.clear();
    }

    /**
     * Resizes the empty state mascot and container for compact screens (like History).
     * <p>
     * Performance: Direct layout parameter modification is used instead of re-inflating views.
     * </p>
     *
     * @param root The root view of the fragment.
     */
    protected void applyCompactEmptyState(View root) {
        if (root == null) return;
        
        View container = root.findViewById(R.id.empty_state_container);
        View mascotContainer = root.findViewById(R.id.mascot_container);
        View mascot = root.findViewById(R.id.empty_mascot);
        View glow = root.findViewById(R.id.mascot_glow);

        float density = Screen.getDensity(getResources());

        if (container != null) {
            container.setPadding(
                    container.getPaddingLeft(),
                    (int) (4 * density),
                    container.getPaddingRight(),
                    (int) (100 * density) // Even more bottom padding for the button
            );
        }

        if (mascotContainer != null) {
            mascotContainer.getLayoutParams().height = (int) (80 * density); // reduced from 100
        }

        if (mascot != null) {
            mascot.getLayoutParams().width = (int) (60 * density); // reduced from 80
            mascot.getLayoutParams().height = (int) (60 * density);
        }

        if (glow != null) {
            glow.getLayoutParams().width = (int) (60 * density);
            glow.getLayoutParams().height = (int) (60 * density);
        }
    }

    /**
     * Interface for handling delayed status updates after user confirmation.
     */
    protected interface StatusUpdateCallback {
        void onApply();
    }

    /**
     * Validates if the user is taking a medication too early or too late based on 
     * Remote Config thresholds. Shows a warning dialog if necessary.
     *
     * @param instance The dose being marked as taken.
     * @param callback Logic to execute if the timing is valid or user confirms.
     */
    /**
     * Checks if a "Take" action is significantly early or late and prompts for confirmation.
     *
     * @param instance The dose instance being taken.
     * @param callback Logic to execute if confirmed.
     */
    protected void checkAndClarifyTakeTiming(com.robinzon.medicationwizard.database.DoseInstanceEntity instance, StatusUpdateCallback callback) {
        long now = System.currentTimeMillis();
        long scheduled = instance.getScheduledTime();
        
        // Difference in minutes: positive if late, negative if early
        long diffMins = (now - scheduled) / 60000;

        com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager rcm = 
                com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance();
        
        int earlyThreshold = rcm.getEarlyTakeThresholdMins();
        int lateThreshold = rcm.getLateTakeThresholdMins();

        // Fallback to defaults if values are not yet fetched or invalid
        if (earlyThreshold <= 0) earlyThreshold = 60;
        if (lateThreshold <= 0) lateThreshold = 180;

        if (diffMins < -earlyThreshold) {
            showTimingClarificationDialog(instance, callback, getString(R.string.take_early_warning));
        } else if (diffMins > lateThreshold) {
            showTimingClarificationDialog(instance, callback, getString(R.string.take_late_warning));
        } else {
            callback.onApply();
        }
    }

    private void showTimingClarificationDialog(com.robinzon.medicationwizard.database.DoseInstanceEntity instance, StatusUpdateCallback callback, String message) {
        com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = 
                new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        dialog.setTitle(instance.getMedicationName());
        dialog.setMessage(message);
        dialog.setPositiveButton(getString(R.string.button_sure), (d, which) -> callback.onApply());
        dialog.setNeutralButton(getString(R.string.button_pick_time), (d, which) -> {
            showTimePickerForTimingClarification(instance, callback);
        });
        dialog.setNegativeButton(getString(R.string.button_not_now), (d, which) -> {
            if (getActivity() instanceof com.robinzon.medicationwizard.MainActivity main) {
                main.addInteractionScore(1.0f);
            }
        });
        dialog.show();
    }

    private void showTimePickerForTimingClarification(com.robinzon.medicationwizard.database.DoseInstanceEntity instance, StatusUpdateCallback callback) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);

        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(requireContext(), (view, hourOfDay, minuteOfHour) -> {
            java.util.Calendar pickedCalendar = java.util.Calendar.getInstance();
            pickedCalendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
            pickedCalendar.set(java.util.Calendar.MINUTE, minuteOfHour);
            pickedCalendar.set(java.util.Calendar.SECOND, 0);
            pickedCalendar.set(java.util.Calendar.MILLISECOND, 0);

            instance.setActionTime(pickedCalendar.getTimeInMillis());
            callback.onApply();
        }, hour, minute, android.text.format.DateFormat.is24HourFormat(requireContext()));
        timePickerDialog.show();
    }

    @Override
    public void onDestroyView() {
        stopEmptyStateAnimations();
        super.onDestroyView();
    }


    /**
     * Applies dynamic bottom padding to a view to clear both the Ad banner and the FAB.
     * <p>
     * Performance: Calculates dimensions once per call to avoid redundant system queries.
     * </p>
     *
     * @param rootView The view to apply padding to (usually a RecyclerView).
     */
    /**
     * Applies standard bottom padding to a view to prevent clipping with the bottom navigation bar.
     */
    protected void setPaddingForRecyclerView(@NonNull final View rootView) {
        setPaddingForRecyclerView(rootView, true);
    }

    /**
     * Applies dynamic bottom padding to a view, with an option to exclude FAB clearance.
     * <p>
     * Implementation:
     * 1. Add FAB clearance if requested.
     * 2. Converts DP to Pixels based on device density.
     * 3. Updates the view's bottom padding while preserving existing horizontal/top padding.
     * 4. If the view is a {@link RecyclerView}, it automatically disables 'clipToPadding'.
     * </p>
     *
     * @param rootView The view to apply padding to.
     * @param withFab  True if the screen features a Floating Action Button.
     */
    protected void setPaddingForRecyclerView(@NonNull final View rootView, final boolean withFab) {

        int paddingBottomPx = 0;
        if (withFab) {
            paddingBottomPx = getResources().getDimensionPixelSize(R.dimen.fab_clearance_padding);
        }

        // Update padding safely without overwriting other sides
        rootView.setPadding(
                rootView.getPaddingLeft(),
                rootView.getPaddingTop(),
                rootView.getPaddingRight(),
                paddingBottomPx
        );

        if (rootView instanceof RecyclerView) {
            // 2. Critically important: allows items to scroll 'under' the fab and reach the bottom
            ((RecyclerView)rootView).setClipToPadding(false);
        }
    }

}