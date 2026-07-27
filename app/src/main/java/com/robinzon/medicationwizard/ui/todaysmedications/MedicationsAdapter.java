package com.robinzon.medicationwizard.ui.todaysmedications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.text.BidiFormatter;
import androidx.recyclerview.widget.RecyclerView;

import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.entities.EForm;
import com.robinzon.medicationwizard.entities.EInstructions;
import com.robinzon.medicationwizard.entities.EMeasurementUnit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedicationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SINGLE = 0;
    private static final int TYPE_GROUP = 1;

    private List<DoseItem> items = new ArrayList<>();
    private OnMedicationActionListener actionListener;

    public MedicationsAdapter(List<DoseItem> dataSet) {
        this.items = dataSet;
    }

    public void setOnMedicationActionListener(OnMedicationActionListener listener) {
        this.actionListener = listener;
    }

    public void setData(List<DoseItem> newData) {
        this.items = newData;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof DoseItem.Group) ? TYPE_GROUP : TYPE_SINGLE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_GROUP) {
            View v = inflater.inflate(R.layout.item_dose_group, parent, false);
            return new GroupViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.medications_list_row, parent, false);
            return new SingleViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DoseItem item = items.get(position);
        if (holder instanceof GroupViewHolder) {
            ((GroupViewHolder) holder).bind((DoseItem.Group) item, position);
        } else {
            ((SingleViewHolder) holder).bind((DoseItem.Single) item, position);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface OnMedicationActionListener {
        void onTake(DoseInstanceEntity instance, int position);

        void onSkip(DoseInstanceEntity instance, int position);

        void onReschedule(DoseInstanceEntity instance, int position);

        void onUntake(DoseInstanceEntity instance, int position);

        void onUnskip(DoseInstanceEntity instance, int position);

        // Group actions
        void onTakeGroup(List<DoseInstanceEntity> doses, int position);

        void onSkipGroup(List<DoseInstanceEntity> doses, int position);

        void onRescheduleGroup(List<DoseInstanceEntity> doses, int position);

        void onUntakeGroup(List<DoseInstanceEntity> doses, int position);

        void onUnskipGroup(List<DoseInstanceEntity> doses, int position);
    }

    // --- ViewHolders ---

    class SingleViewHolder extends RecyclerView.ViewHolder {
        private final TextView medNameText, strengthText, quantityText, timeText, directionsText, formText;
        private final TextView scheduledSummaryText, actualActionSummaryText;
        private final View scheduledDetailsGroup;
        private final AppCompatButton skipButton, rescheduleButton, takeButton, untakeButton;
        private final ImageView medIconImage, doneIconImage;

        SingleViewHolder(View view) {
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

        void bind(DoseItem.Single single, int position) {
            DoseInstanceEntity instance = single.entity;
            BidiFormatter bidi = BidiFormatter.getInstance();
            medNameText.setText(bidi.unicodeWrap(instance.getMedicationName()));

            // Icon and Form logic
            int icon = R.drawable.ic_med_pill;
            String localizedForm = instance.getForm() != null ? instance.getForm() : "";
            if (instance.getForm() != null) {
                try {
                    EForm form = EForm.valueOf(instance.getForm());
                    localizedForm = form.getLabel(itemView.getContext());
                    icon = switch (form) {
                        case Drops -> R.drawable.ic_med_drops;
                        case Injection -> R.drawable.ic_med_injection;
                        case Solution -> R.drawable.ic_med_solution;
                        case Inhaler -> R.drawable.ic_med_inhaler;
                        case Powder -> R.drawable.ic_med_powder;
                        case Other -> R.drawable.ic_med_other;
                        case Pill -> R.drawable.ic_med_pill;
                    };
                } catch (Exception ignored) {
                }
            }
            medIconImage.setImageResource(icon);

            // Formatting
            String strength = instance.getStrength() > 0 ? (instance.getStrength() == (long) instance.getStrength() ? String.valueOf((long) instance.getStrength()) : String.valueOf(instance.getStrength())) : "";
            String unitLabel = EMeasurementUnit.getLabelByName(itemView.getContext(), instance.getUnit());
            strengthText.setText(bidi.unicodeWrap(strength + (unitLabel.isEmpty() ? "" : " " + unitLabel)));
            strengthText.setVisibility(instance.getStrength() > 0 ? View.VISIBLE : View.GONE);

            String amount = instance.getAmount() == (long) instance.getAmount() ? String.valueOf((long) instance.getAmount()) : String.valueOf(instance.getAmount());
            quantityText.setText(bidi.unicodeWrap(amount));

            formText.setText(bidi.unicodeWrap(localizedForm));

            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeText.setText(timeFmt.format(new Date(instance.getScheduledTime())));

            if (instance.getInstruction() != null && !java.util.Objects.equals(instance.getInstruction(), "DOES_NOT_MATTER")) {
                directionsText.setVisibility(View.VISIBLE);
                try {
                    directionsText.setText(EInstructions.valueOf(instance.getInstruction()).getDescription(itemView.getContext()));
                } catch (Exception e) {
                    directionsText.setText(instance.getInstruction());
                }
            } else {
                directionsText.setVisibility(View.GONE);
            }

            // Status UI
            boolean isScheduled = "SCHEDULED".equals(instance.getStatus());
            boolean isTaken = "TAKEN".equals(instance.getStatus());
            boolean isSkipped = "SKIPPED".equals(instance.getStatus());

            takeButton.setVisibility(isScheduled ? View.VISIBLE : View.GONE);
            skipButton.setVisibility(isScheduled ? View.VISIBLE : View.GONE);
            rescheduleButton.setVisibility(isScheduled ? View.VISIBLE : View.GONE);
            doneIconImage.setVisibility(!isScheduled ? View.VISIBLE : View.GONE);
            untakeButton.setVisibility(!isScheduled ? View.VISIBLE : View.GONE);
            untakeButton.setText(isTaken ? R.string.button_untake : R.string.button_unskip);

            scheduledDetailsGroup.setVisibility(isScheduled ? View.VISIBLE : View.GONE);
            itemView.setAlpha(isScheduled ? 1.0f : 0.6f);

            if (!isScheduled) {
                scheduledSummaryText.setVisibility(View.VISIBLE);
                actualActionSummaryText.setVisibility(View.VISIBLE);
                scheduledSummaryText.setText(itemView.getContext().getString(R.string.scheduled_for_format, timeFmt.format(new Date(instance.getScheduledTime()))));
                String actTime = timeFmt.format(new Date(instance.getActionTime() > 0 ? instance.getActionTime() : instance.getScheduledTime()));
                if (isTaken)
                    actualActionSummaryText.setText(itemView.getContext().getString(R.string.took_format, amount, localizedForm, actTime));
                else
                    actualActionSummaryText.setText(itemView.getContext().getString(R.string.skipped_at_format, actTime));
            } else {
                scheduledSummaryText.setVisibility(View.GONE);
                actualActionSummaryText.setVisibility(View.GONE);
            }

            // Listeners
            takeButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    itemView.animate().scaleX(1.05f).scaleY(1.05f).alpha(0f).setDuration(300).withEndAction(() -> {
                        actionListener.onTake(instance, getBindingAdapterPosition());
                        itemView.setScaleX(1f);
                        itemView.setScaleY(1f);
                        itemView.setAlpha(1f);
                    }).start();
                }
            });
            skipButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    itemView.animate().translationX(-itemView.getWidth()).alpha(0f).setDuration(300).withEndAction(() -> {
                        actionListener.onSkip(instance, getBindingAdapterPosition());
                        itemView.setTranslationX(0);
                        itemView.setAlpha(1f);
                    }).start();
                }
            });
            rescheduleButton.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onReschedule(instance, position);
            });
            untakeButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    if (isTaken) actionListener.onUntake(instance, position);
                    else actionListener.onUnskip(instance, position);
                }
            });
        }
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView timeText, statusSummaryText;
        private final LinearLayout medNamesContainer;
        private final ImageView doneIcon;
        private final AppCompatButton takeAllBtn, skipAllBtn, rescheduleAllBtn, untakeAllBtn;

        GroupViewHolder(View v) {
            super(v);
            timeText = v.findViewById(R.id.txt_group_time);
            statusSummaryText = v.findViewById(R.id.txt_group_status_summary);
            medNamesContainer = v.findViewById(R.id.container_med_names);
            doneIcon = v.findViewById(R.id.icon_done);
            takeAllBtn = v.findViewById(R.id.btn_take_all);
            skipAllBtn = v.findViewById(R.id.btn_skip_all);
            rescheduleAllBtn = v.findViewById(R.id.btn_reschedule_all);
            untakeAllBtn = v.findViewById(R.id.btn_untake_all);
        }

        void bind(DoseItem.Group group, int position) {
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeText.setText(itemView.getContext().getString(R.string.group_title_format, timeFmt.format(new Date(group.getScheduledTime()))));

            medNamesContainer.removeAllViews();
            for (DoseInstanceEntity d : group.doses) {
                View rowView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.item_group_medication_row, medNamesContainer, false);

                TextView nameTxt = rowView.findViewById(R.id.med_name);
                TextView statusTxt = rowView.findViewById(R.id.med_status);
                ImageView iconImg = rowView.findViewById(R.id.med_icon);
                ImageView doneImg = rowView.findViewById(R.id.icon_done);
                AppCompatButton takeBtn = rowView.findViewById(R.id.btn_take);
                AppCompatButton skipBtn = rowView.findViewById(R.id.btn_skip);
                AppCompatButton reschedBtn = rowView.findViewById(R.id.btn_reschedule);
                AppCompatButton untakeBtn = rowView.findViewById(R.id.btn_untake);
                TextView actualActionSummaryTxt = rowView.findViewById(R.id.txt_actual_action_summary);
                TextView directionsTxt = rowView.findViewById(R.id.directions);
                View divider = rowView.findViewById(R.id.divider);

                // Smart Detail String: "Med Name - Amount Form Strength"
                String amountStr = d.getAmount() == (long) d.getAmount() ? String.valueOf((long) d.getAmount()) : String.valueOf(d.getAmount());

                // Icon and Form logic
                int icon = R.drawable.ic_med_pill;
                String localizedForm = d.getForm() != null ? d.getForm() : "";
                if (d.getForm() != null) {
                    try {
                        EForm f = EForm.valueOf(d.getForm());
                        localizedForm = f.getLabel(itemView.getContext());
                        icon = switch (f) {
                            case Drops -> R.drawable.ic_med_drops;
                            case Injection -> R.drawable.ic_med_injection;
                            case Solution -> R.drawable.ic_med_solution;
                            case Inhaler -> R.drawable.ic_med_inhaler;
                            case Powder -> R.drawable.ic_med_powder;
                            case Other -> R.drawable.ic_med_other;
                            default -> R.drawable.ic_med_pill;
                        };
                    } catch (Exception ignored) {
                    }
                }
                iconImg.setImageResource(icon);

                String strengthStr = "";
                if (d.getStrength() > 0) {
                    String sVal = d.getStrength() == (long) d.getStrength() ? String.valueOf((long) d.getStrength()) : String.valueOf(d.getStrength());
                    strengthStr = sVal + (d.getUnit() != null ? " " + EMeasurementUnit.getLabelByName(itemView.getContext(), d.getUnit()) : "");
                }

                BidiFormatter bidi = BidiFormatter.getInstance();
                String name = bidi.unicodeWrap(d.getMedicationName());
                String details = bidi.unicodeWrap(amountStr + " " + localizedForm + (strengthStr.isEmpty() ? "" : ", " + strengthStr));
                String fullDetail = name + " (" + details + ")";
                nameTxt.setText(fullDetail);

                boolean isScheduled = "SCHEDULED".equals(d.getStatus());
                boolean isTaken = "TAKEN".equals(d.getStatus());
                boolean isSkipped = "SKIPPED".equals(d.getStatus());

                statusTxt.setText(isScheduled ? "" : (isTaken ? itemView.getContext().getString(R.string.take) : itemView.getContext().getString(R.string.button_skip)));
                doneImg.setVisibility(isScheduled ? View.GONE : View.VISIBLE);
                rowView.setAlpha(isScheduled ? 1.0f : 0.6f);

                if (!isScheduled) {
                    actualActionSummaryTxt.setVisibility(View.VISIBLE);
                    String actTime = timeFmt.format(new Date(d.getActionTime() > 0 ? d.getActionTime() : d.getScheduledTime()));
                    if (isTaken)
                        actualActionSummaryTxt.setText(itemView.getContext().getString(R.string.took_format, amountStr, localizedForm, actTime));
                    else
                        actualActionSummaryTxt.setText(itemView.getContext().getString(R.string.skipped_at_format, actTime));
                } else {
                    actualActionSummaryTxt.setVisibility(View.GONE);
                }

                if (d.getInstruction() != null && !java.util.Objects.equals(d.getInstruction(), "DOES_NOT_MATTER")) {
                    directionsTxt.setVisibility(View.VISIBLE);
                    try {
                        directionsTxt.setText(EInstructions.valueOf(d.getInstruction()).getDescription(itemView.getContext()));
                    } catch (Exception e) {
                        directionsTxt.setText(d.getInstruction());
                    }
                } else {
                    directionsTxt.setVisibility(View.GONE);
                }

                takeBtn.setVisibility(isScheduled ? View.VISIBLE : View.GONE);
                skipBtn.setVisibility(isScheduled ? View.VISIBLE : View.GONE);
                reschedBtn.setVisibility(isScheduled ? View.VISIBLE : View.GONE);
                untakeBtn.setVisibility(!isScheduled ? View.VISIBLE : View.GONE);
                untakeBtn.setText(isTaken ? R.string.button_untake : R.string.button_unskip);

                if (group.doses.indexOf(d) == group.doses.size() - 1) {
                    divider.setVisibility(View.GONE);
                }

                takeBtn.setOnClickListener(vRow -> {
                    if (actionListener != null) actionListener.onTake(d, position);
                });
                skipBtn.setOnClickListener(vRow -> {
                    if (actionListener != null) actionListener.onSkip(d, position);
                });
                reschedBtn.setOnClickListener(vRow -> {
                    if (actionListener != null) actionListener.onReschedule(d, position);
                });
                untakeBtn.setOnClickListener(vRow -> {
                    if (actionListener != null) {
                        if (isTaken) actionListener.onUntake(d, position);
                        else actionListener.onUnskip(d, position);
                    }
                });

                medNamesContainer.addView(rowView);
            }

            String status = group.getStatus();
            boolean isAllScheduled = "SCHEDULED".equals(status);
            boolean isAllTaken = "TAKEN".equals(status);
            boolean isAllSkipped = "SKIPPED".equals(status);

            boolean anyScheduled = false;
            for (DoseInstanceEntity d : group.doses) {
                if ("SCHEDULED".equals(d.getStatus())) {
                    anyScheduled = true;
                    break;
                }
            }

            takeAllBtn.setVisibility(isAllScheduled ? View.VISIBLE : View.GONE);
            skipAllBtn.setVisibility(isAllScheduled ? View.VISIBLE : View.GONE);
            rescheduleAllBtn.setVisibility(isAllScheduled ? View.VISIBLE : View.GONE);

            untakeAllBtn.setVisibility(isAllScheduled ? View.GONE : View.VISIBLE);
            if (!isAllScheduled) {
                if (isAllTaken) untakeAllBtn.setText(R.string.button_untake_all);
                else if (isAllSkipped) untakeAllBtn.setText(R.string.button_unskip_all);
                else untakeAllBtn.setText(R.string.button_reset_all);
            }

            doneIcon.setVisibility(anyScheduled ? View.GONE : View.VISIBLE);
            itemView.setAlpha(anyScheduled ? 1.0f : 0.6f);

            if (!isAllScheduled) {
                statusSummaryText.setVisibility(View.VISIBLE);
                if (isAllTaken) statusSummaryText.setText(R.string.group_status_taken);
                else if (isAllSkipped) statusSummaryText.setText(R.string.group_status_skipped);
                else statusSummaryText.setText(R.string.group_status_mixed);
            } else {
                statusSummaryText.setVisibility(View.GONE);
            }

            takeAllBtn.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onTakeGroup(group.doses, position);
            });
            skipAllBtn.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onSkipGroup(group.doses, position);
            });
            rescheduleAllBtn.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onRescheduleGroup(group.doses, position);
                }
            });
            untakeAllBtn.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onUntakeGroup(group.doses, position);
                }
            });
        }
    }
}
