package com.robinzon.medicationwizard.ui.medicationslist;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.text.BidiFormatter;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.databinding.ItemMedicationListBinding;
import com.robinzon.medicationwizard.entities.EInstructions;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.ui.settings.FeatureRationalBottomSheet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter for the master medication library list.
 */
public class MedicationsListAdapter extends RecyclerView.Adapter<MedicationsListAdapter.MedicationViewHolder> {

    private final List<Medication> medications = new ArrayList<>();
    private final Set<String> expandedIds = new HashSet<>();
    private OnMedicationActionListener listener;

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
        Medication medication = medications.get(position);
        holder.bind(medication, expandedIds.contains(medication.getId()));
    }

    public void expandAll() {
        for (Medication med : medications) {
            expandedIds.add(med.getId());
        }
        notifyDataSetChanged();
    }

    public void collapseAll() {
        expandedIds.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return medications.size();
    }

    public interface OnMedicationActionListener {
        void onDelete(Medication medication, int position);
        void onEdit(Medication medication);
    }

    class MedicationViewHolder extends RecyclerView.ViewHolder {
        private final ItemMedicationListBinding binding;

        public MedicationViewHolder(ItemMedicationListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Medication medication, boolean isExpanded) {
            BidiFormatter bidi = BidiFormatter.getInstance();
            binding.medName.setText(bidi.unicodeWrap(medication.getCommercialName()));

            if (medication.getStrength() <= 0) {
                binding.medStrength.setVisibility(View.GONE);
            } else {
                binding.medStrength.setVisibility(View.VISIBLE);
                String strengthStr = medication.getStrength() == (long) medication.getStrength() ?
                        String.format(Locale.getDefault(), "%d", (long) medication.getStrength()) :
                        String.format(Locale.getDefault(), "%.1f", medication.getStrength());

                binding.medStrength.setText(bidi.unicodeWrap(String.format(Locale.getDefault(), "%s %s",
                        strengthStr,
                        medication.getMeasurementUnit() != null ? medication.getMeasurementUnit().getLabel(binding.getRoot().getContext()) : "")));
            }

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

                String amountStr = medication.getAmount() == (long) medication.getAmount() ?
                        String.format(Locale.getDefault(), "%d", (long) medication.getAmount()) :
                        String.format(Locale.getDefault(), "%.1f", medication.getAmount());
                String formStr = medication.getForm() != null ? medication.getForm().getLabel(binding.getRoot().getContext()) : "";
                String instrStr = "";
                if (medication.getInstruction() != null && medication.getInstruction() != EInstructions.DOES_NOT_MATTER) {
                    instrStr = medication.getInstruction().getDescription(binding.getRoot().getContext());
                }
                binding.medInstructions.setText(String.format("%s %s %s", bidi.unicodeWrap(amountStr), bidi.unicodeWrap(formStr), bidi.unicodeWrap(instrStr)).trim());

                StringBuilder timesBuilder = new StringBuilder();
                if (medication.getTimesADay() != null) {
                    for (int i = 0; i < medication.getTimesADay().size(); i++) {
                        timesBuilder.append(medication.getTimesADay().valueAt(i).toString());
                        if (i < medication.getTimesADay().size() - 1) timesBuilder.append(", ");
                    }
                }
                binding.medTimes.setText(binding.getRoot().getContext().getString(R.string.scheduled_at, timesBuilder.toString()));

                // Image Loading Logic
                if (medication.getImagePath() != null) {
                    File file = new File(medication.getImagePath());
                    if (file.exists()) {
                        binding.photoPlaceholder.setVisibility(View.GONE);
                        binding.medPhoto.setVisibility(View.VISIBLE);

                        // Detect orientation for correct ratio
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                        boolean isPortrait = options.outHeight > options.outWidth;

                        ConstraintSet constraintSet = new ConstraintSet();
                        constraintSet.clone(binding.photoContainer);
                        constraintSet.setDimensionRatio(binding.medPhoto.getId(), isPortrait ? "H,9:16" : "H,16:9");
                        constraintSet.setDimensionRatio(binding.photoPlaceholder.getId(), isPortrait ? "H,9:16" : "H,16:9");
                        constraintSet.setDimensionRatio(binding.layoutPhotoLocked.getId(), isPortrait ? "H,9:16" : "H,16:9");
                        constraintSet.applyTo(binding.photoContainer);

                        // --- Smart Lock Logic ---
                        boolean isLocked = !AppConfig.isFeatureUnlocked(binding.getRoot().getContext(), AppConfig.FeaturePassType.PHOTO);
                        binding.layoutPhotoLocked.setVisibility(isLocked ? View.VISIBLE : View.GONE);

                        if (isLocked) {
                            // Apply Grayscale + Dim Filter
                            ColorMatrix matrix = new ColorMatrix();
                            matrix.setSaturation(0); // Grayscale
                            float[] src = matrix.getArray();
                            src[4] -= 40; src[9] -= 40; src[14] -= 40; // Dimming
                            binding.medPhoto.setColorFilter(new ColorMatrixColorFilter(matrix));

                            binding.layoutPhotoLocked.setOnClickListener(v -> {
                                Context ctx = binding.getRoot().getContext();
                                if (ctx instanceof FragmentActivity) {
                                    FeatureRationalBottomSheet.newInstance(AppConfig.FeaturePassType.PHOTO)
                                            .show(((FragmentActivity) ctx).getSupportFragmentManager(), "UnlockPhoto");
                                }
                            });
                        } else {
                            binding.medPhoto.clearColorFilter();
                            binding.layoutPhotoLocked.setOnClickListener(null);
                        }

                        Glide.with(binding.getRoot().getContext())
                                .load(file)
                                .signature(new ObjectKey(file.lastModified()))
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .into(binding.medPhoto);
                    } else {
                        binding.medPhoto.setVisibility(View.GONE);
                        binding.photoPlaceholder.setVisibility(View.VISIBLE);
                        binding.layoutPhotoLocked.setVisibility(View.GONE);
                    }
                } else {
                    binding.medPhoto.setVisibility(View.GONE);
                    binding.photoPlaceholder.setVisibility(View.VISIBLE);
                    binding.layoutPhotoLocked.setVisibility(View.GONE);
                }
            }

            binding.cardView.setOnClickListener(v -> {
                String id = medication.getId();
                if (expandedIds.contains(id)) {
                    expandedIds.remove(id);
                } else {
                    expandedIds.add(id);
                }
                notifyItemChanged(getBindingAdapterPosition());
            });

            binding.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(medication);
            });

            binding.btnDelete.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    binding.cardView.animate()
                            .translationX(binding.cardView.getWidth() * 0.5f)
                            .alpha(0f)
                            .setDuration(400)
                            .withEndAction(() -> {
                                if (listener != null) listener.onDelete(medication, pos);
                                binding.cardView.setTranslationX(0);
                                binding.cardView.setAlpha(1f);
                            })
                            .start();
                }
            });
        }
    }
}
