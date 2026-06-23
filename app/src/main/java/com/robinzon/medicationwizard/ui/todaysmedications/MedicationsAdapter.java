package com.robinzon.medicationwizard.ui.todaysmedications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;

import java.util.List;

/**
 * A specialized RecyclerView Adapter for displaying medication dose instances.
 * <p>
 * This adapter is used by both the "Today's Medications" and "History" fragments. 
 * It dynamically adjusts its UI based on the status of the medication (Scheduled vs. Completed) 
 * and handles complex animations for user interactions.
 * </p>
 */
public class MedicationsAdapter extends RecyclerView.Adapter<MedicationsAdapter.ViewHolder> {
    
    private List<DoseInstanceEntity> doses;
    private OnMedicationActionListener actionListener;

    /**
     * Interface for handling user interactions on a specific dose item.
     */
    public interface OnMedicationActionListener {
        /** Called when the user clicks 'Take'. */
        void onTake(DoseInstanceEntity instance, int position);
        /** Called when the user clicks 'Skip'. */
        void onSkip(DoseInstanceEntity instance, int position);
        /** Called when the user clicks 'Reschedule'. */
        void onReschedule(DoseInstanceEntity instance, int position);
        /** Called when the user clicks 'Un-take' on a taken item. */
        void onUntake(DoseInstanceEntity instance, int position);
        /** Called when the user clicks 'Un-skip' on a skipped item. */
        void onUnskip(DoseInstanceEntity instance, int position);
    }

    /**
     * Sets the listener for medication actions.
     *
     * @param listener The action listener.
     */
    public void setOnMedicationActionListener(OnMedicationActionListener listener) {
        actionListener = listener;
    }

    /**
     * Constructs the adapter with an initial dataset.
     *
     * @param dataSet The list of doses to display.
     */
    public MedicationsAdapter(List<DoseInstanceEntity> dataSet) {
        doses = dataSet;
    }

    /**
     * Updates the dataset and refreshes the entire list.
     *
     * @param medications The new list of doses.
     */
    public void setMedications(List<DoseInstanceEntity> medications) {
        this.doses = medications;
        notifyDataSetChanged();
    }


    /**
     * ViewHolder pattern to hold references to individual item views for performance.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView medNameText;
        private final TextView strengthText;
        private final TextView quantityText;
        private final TextView timeText;
        private final TextView directionsText;
        private final TextView formText;
        private final TextView scheduledSummaryText;
        private final TextView actualActionSummaryText;
        private final View scheduledDetailsGroup;
        private final AppCompatButton skipButton;
        private final AppCompatButton rescheduleButton;
        private final AppCompatButton takeButton;
        private final AppCompatButton untakeButton;
        private final android.widget.ImageView medIconImage;
        private final android.widget.ImageView doneIconImage;

        /**
         * Finds and caches all sub-views for the row layout.
         */
        public ViewHolder(View view) {
            super(view);
            medNameText = view.findViewById(R.id.med_name);
            strengthText = view.findViewById(R.id.med_strength);
            quantityText = view.findViewById(R.id.quantity);
            timeText = view.findViewById(R.id.time);
            directionsText = view.findViewById(R.id.directions);
            formText = view.findViewById(R.id.form);
            scheduledSummaryText = view.findViewById(R.id.txt_scheduled_summary);
            actualActionSummaryText = view.findViewById(R.id.txt_actual_action_summary);
            scheduledDetailsGroup = view.findViewById(R.id.group_scheduled_details);
            skipButton = view.findViewById(R.id.skip_btn);
            rescheduleButton = view.findViewById(R.id.reschedule_btn);
            takeButton = view.findViewById(R.id.take_btn);
            untakeButton = view.findViewById(R.id.untake_btn);
            medIconImage = view.findViewById(R.id.med_icon);
            doneIconImage = view.findViewById(R.id.icon_done);
        }

        public TextView getTxtScheduledSummary() { return scheduledSummaryText; }
        public TextView getTxtActualActionSummary() { return actualActionSummaryText; }
        public View getGroupScheduledDetails() { return scheduledDetailsGroup; }
        public AppCompatButton getTakeButton() { return takeButton; }
        public AppCompatButton getUntakeButton() { return untakeButton; }

        public android.widget.ImageView getIconDone() {
            return doneIconImage;
        }

        public TextView getForm() {
            return formText;
        }

        public android.widget.ImageView getMedIcon() {
            return medIconImage;
        }

        public TextView getMedName() {
            return medNameText;
        }

        public TextView getStrength() {
            return strengthText;
        }

        public TextView getQuantity() {
            return quantityText;
        }

        public TextView getTime() {
            return timeText;
        }

        public TextView getDirections() {
            return directionsText;
        }

        public AppCompatButton getSkip() {
            return skipButton;
        }

        public AppCompatButton getReschedule() {
            return rescheduleButton;
        }
    }


    /**
     * Inflates the M3 card layout for each medication row.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.medications_list_row, viewGroup, false);

        return new ViewHolder(view);
    }

    /**
     * Binds data from a {@link DoseInstanceEntity} to the UI views.
     * <p>
     * Logic:
     * 1. Smart Formatting: Truncates decimal zeros (e.g., 2.0 -> 2) for amount/strength.
     * 2. Instruction Logic: Hides the directions field if "Does Not Matter" is selected.
     * 3. State Management: Toggles between "Scheduled" views (buttons + quantity) and 
     *    "Completed" views (Summaries + Done icon + Translucency).
     * 4. Action Animations: Implements scale/fade for 'Take' and slide/fade for 'Skip'.
     * </p>
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {

        final DoseInstanceEntity instance = doses.get(position);
        viewHolder.getMedName().setText(instance.getMedicationName());

        // Set icon based on form definition
        int formIconRes = R.drawable.ic_med_pill; // Default
        if (instance.getForm() != null) {
            try {
                com.robinzon.medicationwizard.entities.EForm form = com.robinzon.medicationwizard.entities.EForm.valueOf(instance.getForm());
                formIconRes = switch (form) {
                    case Drops -> R.drawable.ic_med_drops;
                    case Injection -> R.drawable.ic_med_injection;
                    case Solution -> R.drawable.ic_med_solution;
                    case Inhaler -> R.drawable.ic_med_inhaler;
                    case Powder -> R.drawable.ic_med_powder;
                    case Other -> R.drawable.ic_med_other;
                    case Pill -> R.drawable.ic_med_pill;
                };
            } catch (IllegalArgumentException ignored) {}
        }
        viewHolder.getMedIcon().setImageResource(formIconRes);

        // 1. Smart Strength Formatting
        if (instance.getStrength() <= 0) {
            viewHolder.getStrength().setVisibility(View.GONE);
        } else {
            viewHolder.getStrength().setVisibility(View.VISIBLE);
            String strengthStr = instance.getStrength() == (long) instance.getStrength() ?
                    String.valueOf((long) instance.getStrength()) :
                    String.valueOf(instance.getStrength());
            viewHolder.getStrength().setText(strengthStr.concat(" ").concat(instance.getUnit() != null ? instance.getUnit() : ""));
        }

        // 2. Populate Quantity and Form (with simple pluralization)
        String amountStr = instance.getAmount() == (long) instance.getAmount() ?
                String.valueOf((long) instance.getAmount()) : String.valueOf(instance.getAmount());
        viewHolder.getQuantity().setText(amountStr);

        String formName = instance.getForm() != null ? instance.getForm() : "";
        if (instance.getAmount() > 1 && !formName.isEmpty()) {
            if (!formName.toLowerCase().endsWith("s")) formName += "s";
        }
        viewHolder.getForm().setText(formName);

        // 3. Format Scheduled Time
        java.text.SimpleDateFormat timeFmt = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        viewHolder.getTime().setText(timeFmt.format(new java.util.Date(instance.getScheduledTime())));

        // 4. Smart Directions/Instructions Formatting
        if (instance.getInstruction() == null ||
                instance.getInstruction().equals("DOES_NOT_MATTER")) {
            viewHolder.getDirections().setVisibility(View.GONE);
        } else {
            viewHolder.getDirections().setVisibility(View.VISIBLE);
            try {
                com.robinzon.medicationwizard.entities.EInstructions instr = com.robinzon.medicationwizard.entities.EInstructions.valueOf(instance.getInstruction());
                viewHolder.getDirections().setText(instr.getDescription());
            } catch (IllegalArgumentException e) {
                viewHolder.getDirections().setText(instance.getInstruction());
            }
        }

        // 5. Handle Status UI (Scheduled vs. Completed)
        boolean isActionable = "SCHEDULED".equals(instance.getStatus());
        viewHolder.getTakeButton().setVisibility(isActionable ? View.VISIBLE : View.GONE);
        viewHolder.getSkip().setVisibility(isActionable ? View.VISIBLE : View.GONE);
        viewHolder.getReschedule().setVisibility(isActionable ? View.VISIBLE : View.GONE);
        viewHolder.getIconDone().setVisibility(!isActionable ? View.VISIBLE : View.GONE);

        boolean isTaken = "TAKEN".equals(instance.getStatus());
        boolean isSkipped = "SKIPPED".equals(instance.getStatus());
        viewHolder.getUntakeButton().setVisibility((isTaken || isSkipped) ? View.VISIBLE : View.GONE);
        if (isTaken || isSkipped) {
            viewHolder.getUntakeButton().setText(isTaken ? R.string.button_untake : R.string.button_unskip);
        }
        
        // Group visibility for "Take X At HH:MM" line
        viewHolder.getGroupScheduledDetails().setVisibility(isActionable ? View.VISIBLE : View.GONE);
        
        if (!isActionable) {
            // Visual feedback for completed tasks
            viewHolder.itemView.setAlpha(0.6f);
            
            String scheduledTimeStr = timeFmt.format(new java.util.Date(instance.getScheduledTime()));
            String actionTimeStr = instance.getActionTime() > 0 ? 
                    timeFmt.format(new java.util.Date(instance.getActionTime())) : scheduledTimeStr;

            // Line 1: Summary of planned time
            viewHolder.getTxtScheduledSummary().setText(viewHolder.itemView.getContext().getString(R.string.scheduled_for_format, scheduledTimeStr));
            viewHolder.getTxtScheduledSummary().setVisibility(View.VISIBLE);

            // Line 2: Detailed summary of actual user action
            if ("TAKEN".equals(instance.getStatus())) {
                String pluralForm = instance.getForm() != null ? instance.getForm().toLowerCase() : "pill";
                if (instance.getAmount() > 1 && !pluralForm.endsWith("s")) pluralForm += "s";
                
                viewHolder.getTxtActualActionSummary().setText(viewHolder.itemView.getContext().getString(R.string.took_format, amountStr, pluralForm, actionTimeStr));
            } else {
                viewHolder.getTxtActualActionSummary().setText(viewHolder.itemView.getContext().getString(R.string.skipped_at_format, actionTimeStr));
            }
            viewHolder.getTxtActualActionSummary().setVisibility(View.VISIBLE);
            
        } else {
            // Restore default state for scheduled items
            viewHolder.itemView.setAlpha(1.0f);
            viewHolder.getTxtScheduledSummary().setVisibility(View.GONE);
            viewHolder.getTxtActualActionSummary().setVisibility(View.GONE);
        }

        // 6. Interaction Listeners with M3-styled animations
        viewHolder.getTakeButton().setOnClickListener(v -> {
            if (actionListener != null) {
                // Scale success animation
                viewHolder.itemView.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            actionListener.onTake(instance, viewHolder.getBindingAdapterPosition());
                            viewHolder.itemView.setScaleX(1f);
                            viewHolder.itemView.setScaleY(1f);
                            viewHolder.itemView.setAlpha(1f);
                        }).start();
            }
        });

        viewHolder.getSkip().setOnClickListener(v -> {
            if (actionListener != null) {
                // Slide dismiss animation
                viewHolder.itemView.animate()
                        .translationX(-viewHolder.itemView.getWidth())
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            actionListener.onSkip(instance, viewHolder.getBindingAdapterPosition());
                            viewHolder.itemView.setTranslationX(0);
                            viewHolder.itemView.setAlpha(1f);
                        }).start();
            }
        });

        viewHolder.getReschedule().setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onReschedule(instance, viewHolder.getBindingAdapterPosition());
            }
        });

        viewHolder.getUntakeButton().setOnClickListener(v -> {
            if (actionListener != null) {
                if ("TAKEN".equals(instance.getStatus())) {
                    actionListener.onUntake(instance, viewHolder.getBindingAdapterPosition());
                } else {
                    actionListener.onUnskip(instance, viewHolder.getBindingAdapterPosition());
                }
            }
        });
    }

    /**
     * @return Number of items in the filtered list.
     */
    @Override
    public int getItemCount() {
        return doses.size();
    }
}
