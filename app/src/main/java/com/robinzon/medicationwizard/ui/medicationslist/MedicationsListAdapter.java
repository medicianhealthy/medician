package com.robinzon.medicationwizard.ui.medicationslist;

import android.media.AudioManager;
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

public class MedicationsListAdapter extends RecyclerView.Adapter<MedicationsListAdapter.MedicationViewHolder> {

    private final List<Medication> medications = new ArrayList<>();
    private int expandedPosition = -1;
    private OnMedicationActionListener listener;

    public interface OnMedicationActionListener {
        void onDelete(Medication medication, int position);
        void onEdit(Medication medication);
    }

    public void setOnMedicationActionListener(OnMedicationActionListener listener) {
        this.listener = listener;
    }

    public void setMedications(List<Medication> medications) {
        this.medications.clear();
        this.medications.addAll(medications);
        notifyDataSetChanged();
    }

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

    class MedicationViewHolder extends RecyclerView.ViewHolder {
        private final ItemMedicationListBinding binding;

        public MedicationViewHolder(ItemMedicationListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Medication medication, boolean isExpanded) {
            binding.medName.setText(medication.getCommercialName());
            
            // Smart Strength Formatting & Visibility
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

            // Set icon based on form for both Header and Instructions
            int formIconRes = R.drawable.ic_med_pill; // Default
            if (medication.getForm() != null) {
                switch (medication.getForm()) {
                    case Drops: formIconRes = R.drawable.ic_med_drops; break;
                    case Injection: formIconRes = R.drawable.ic_med_injection; break;
                    case Solution: formIconRes = R.drawable.ic_med_solution; break;
                    case Inhaler: formIconRes = R.drawable.ic_med_inhaler; break;
                    case Powder: formIconRes = R.drawable.ic_med_powder; break;
                    case Other: formIconRes = R.drawable.ic_med_other; break;
                    case Pill: default: formIconRes = R.drawable.ic_med_pill; break;
                }
            }
            binding.medIcon.setImageResource(formIconRes);
            binding.medInstructionsIcon.setImageResource(formIconRes);

            // Expanded details
            binding.expandedDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            binding.expandIcon.setRotation(isExpanded ? 180f : 0f);

            if (isExpanded) {
                String frequencyText;
                if (medication.getDailyFrequency() == 1) {
                    frequencyText = binding.getRoot().getContext().getString(R.string.once_a_day);
                } else if (medication.getDailyFrequency() == 2) {
                    frequencyText = binding.getRoot().getContext().getString(R.string.twice_a_day);
                } else {
                    frequencyText = binding.getRoot().getContext().getString(R.string.times_a_day, medication.getDailyFrequency());
                }
                binding.medFrequency.setText(frequencyText);

                // Modern combined instruction: "Amount Form Instruction"
                String amountStr = medication.getAmount() == (long) medication.getAmount() ?
                        String.format(Locale.getDefault(), "%d", (long) medication.getAmount()) :
                        String.format(Locale.getDefault(), "%.1f", medication.getAmount());

                String formStr = medication.getForm() != null ? medication.getForm().name().toLowerCase() : "";
                if (medication.getAmount() > 1 && !formStr.isEmpty()) {
                    formStr += "s"; // Simple pluralization
                }

                String instrStr = "";
                if (medication.getInstruction() != null && medication.getInstruction() != EInstructions.DOES_NOT_MATTER) {
                    instrStr = medication.getInstruction().getDescription().toLowerCase();
                } else if (medication.getInstruction() == null) {
                    instrStr = binding.getRoot().getContext().getString(R.string.no_instructions).toLowerCase();
                }

                binding.medInstructions.setText(String.format("%s %s %s", amountStr, formStr, instrStr).trim());

                StringBuilder timesBuilder = new StringBuilder();
                if (medication.getTimesADay() != null) {
                    for (int i = 0; i < medication.getTimesADay().size(); i++) {
                        timesBuilder.append(medication.getTimesADay().valueAt(i).toString());
                        if (i < medication.getTimesADay().size() - 1) timesBuilder.append(", ");
                    }
                }
                binding.medTimes.setText(binding.getRoot().getContext().getString(R.string.scheduled_at, timesBuilder.toString()));
            }

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
                    // Play a modern delete sound (Triple beep/Prop tone)
                    try {
                        ToneGenerator tg = new ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100);
                        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                    } catch (Exception ignored) {}

                    // Modern "Slide & Fade" Animation
                    binding.cardView.animate()
                            .translationX(binding.cardView.getWidth() * 0.5f)
                            .alpha(0f)
                            .setDuration(400)
                            .withEndAction(() -> {
                                if (listener != null) {
                                    listener.onDelete(medication, pos);
                                }
                                // Reset properties so recycled views aren't invisible!
                                binding.cardView.setTranslationX(0);
                                binding.cardView.setAlpha(1f);
                            })
                            .start();
                }
            });
        }
    }
}