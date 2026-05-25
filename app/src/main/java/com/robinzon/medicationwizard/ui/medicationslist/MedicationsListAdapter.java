package com.robinzon.medicationwizard.ui.medicationslist;

import android.media.ToneGenerator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.databinding.ItemMedicationListBinding;
import com.robinzon.medicationwizard.entities.EInstructions;
import com.robinzon.medicationwizard.entities.Medication;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the master medication library list.
 * <p>
 * This adapter handles complex Material 3 interactions, including:
 * - Expandable cards to show/hide detailed medication info.
 * - Dynamic icon switching based on medication form.
 * - Smart formatting of strength, quantity, and instructions.
 * - High-quality delete animations (slide + fade) with audio feedback.
 * </p>
 */
public class MedicationsListAdapter extends RecyclerView.Adapter<MedicationsListAdapter.MedicationViewHolder> {

    /** Current list of medications to display. */
    private final List<Medication> medications = new ArrayList<>();
    
    /** The position of the currently expanded card (-1 if none). */
    private int expandedPosition = -1;
    
    /** Listener for edit and delete actions. */
    private OnMedicationActionListener listener;

    /**
     * Interface for handling actions triggered from individual medication cards.
     */
    public interface OnMedicationActionListener {
        /**
         * Triggered when the user clicks the delete button.
         * @param medication The medication to delete.
         * @param position   The adapter position.
         */
        void onDelete(Medication medication, int position);
        
        /**
         * Triggered when the user clicks the edit button.
         * @param medication The medication to edit.
         */
        void onEdit(Medication medication);
    }

    /**
     * Sets the action listener.
     */
    public void setOnMedicationActionListener(OnMedicationActionListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the full dataset and refreshes the list.
     *
     * @param medications The new list of medications.
     */
    public void setMedications(List<Medication> medications) {
        this.medications.clear();
        this.medications.addAll(medications);
        notifyDataSetChanged();
    }

    /**
     * Removes an item from the internal list and triggers the removal animation.
     *
     * @param position The position to remove.
     */
    public void removeItem(int position) {
        if (position >= 0 && position < medications.size()) {
            medications.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMedicationListBinding binding = ItemMedicationListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MedicationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        holder.bind(medications.get(position), position == expandedPosition);
    }

    @Override
    public int getItemCount() {
        return medications.size();
    }

    /**
     * ViewHolder that manages the lifecycle and binding logic for a single medication card.
     */
    class MedicationViewHolder extends RecyclerView.ViewHolder {
        private final ItemMedicationListBinding binding;

        public MedicationViewHolder(ItemMedicationListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds medication data to the card UI.
         *
         * @param medication The medication data.
         * @param isExpanded True if the detailed view should be visible.
         */
        public void bind(Medication medication, boolean isExpanded) {
            binding.medName.setText(medication.getCommercialName());
            
            // Smart Strength Formatting: Hides 0 values and formats decimals nicely.
            if (medication.getStrength() <= 0) {
                binding.medStrength.setVisibility(View.GONE);
            } else {
                binding.medStrength.setVisibility(View.VISIBLE);
                String strengthStr = medication.getStrength() == (long) medication.getStrength() ?
                        String.format(Locale.getDefault(), "%d", (long) medication.getStrength()) :
                        String.format(Locale.getDefault(), "%.1f", medication.getStrength());

                binding.medStrength.setText(String.format(Locale.getDefault(), "%s %s",
                        strengthStr,
                        medication.getMeasurementUnit() != null ? medication.getMeasurementUnit().getName() : ""));
            }

            // Sync icon with medication form (e.g., Drops icon for drops)
            int formIconRes = R.drawable.ic_med_pill;
            if (medication.getForm() != null) {
                formIconRes = switch (medication.getForm()) {
                    case Drops -> R.drawable.ic_med_drops;
                    case Injection -> R.drawable.ic_med_injection;
                    case Solution -> R.drawable.ic_med_solution;
                    case Inhaler -> R.drawable.ic_med_inhaler;
                    case Powder -> R.drawable.ic_med_powder;
                    case Other -> R.drawable.ic_med_other;
                    case Pill -> R.drawable.ic_med_pill;
                };
            }
            binding.medIcon.setImageResource(formIconRes);
            binding.medInstructionsIcon.setImageResource(formIconRes);

            // Handle card expansion visibility
            binding.expandedDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            binding.expandIcon.setRotation(isExpanded ? 180f : 0f);

            if (isExpanded) {
                // Populate human-readable frequency
                String frequencyText;
                if (medication.getDailyFrequency() == 1) {
                    frequencyText = binding.getRoot().getContext().getString(R.string.once_a_day);
                } else if (medication.getDailyFrequency() == 2) {
                    frequencyText = binding.getRoot().getContext().getString(R.string.twice_a_day);
                } else {
                    frequencyText = binding.getRoot().getContext().getString(R.string.times_a_day, medication.getDailyFrequency());
                }
                binding.medFrequency.setText(frequencyText);

                // Build modern instruction string: "Quantity Form Instruction"
                String amountStr = medication.getAmount() == (long) medication.getAmount() ?
                        String.format(Locale.getDefault(), "%d", (long) medication.getAmount()) :
                        String.format(Locale.getDefault(), "%.1f", medication.getAmount());

                String formStr = medication.getForm() != null ? medication.getForm().name().toLowerCase() : "";
                if (medication.getAmount() > 1 && !formStr.isEmpty()) {
                    if (!formStr.endsWith("s")) formStr += "s";
                }

                String instrStr = "";
                if (medication.getInstruction() != null && medication.getInstruction() != EInstructions.DOES_NOT_MATTER) {
                    instrStr = medication.getInstruction().getDescription().toLowerCase();
                }

                binding.medInstructions.setText(String.format("%s %s %s", amountStr, formStr, instrStr).trim());

                // Build a list of all daily scheduled times
                StringBuilder timesBuilder = new StringBuilder();
                if (medication.getTimesADay() != null) {
                    for (int i = 0; i < medication.getTimesADay().size(); i++) {
                        timesBuilder.append(medication.getTimesADay().valueAt(i).toString());
                        if (i < medication.getTimesADay().size() - 1) timesBuilder.append(", ");
                    }
                }
                binding.medTimes.setText(binding.getRoot().getContext().getString(R.string.scheduled_at, timesBuilder.toString()));
            }

            // Click listener for expansion/collapsing
            binding.cardView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                int previousExpanded = expandedPosition;
                if (isExpanded) {
                    expandedPosition = -1;
                } else {
                    expandedPosition = position;
                }
                notifyItemChanged(previousExpanded);
                notifyItemChanged(expandedPosition);
            });

            binding.btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(medication);
                }
            });

            binding.btnDelete.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    // Play a modern feedback sound
                    try {
                        ToneGenerator tg = new ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100);
                        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                    } catch (Exception ignored) {}

                    // Material "Slide & Fade" removal animation
                    binding.cardView.animate()
                            .translationX(binding.cardView.getWidth() * 0.5f)
                            .alpha(0f)
                            .setDuration(400)
                            .withEndAction(() -> {
                                if (listener != null) {
                                    listener.onDelete(medication, pos);
                                }
                                // Reset for view recycling
                                binding.cardView.setTranslationX(0);
                                binding.cardView.setAlpha(1f);
                            })
                            .start();
                }
            });
        }
    }
}