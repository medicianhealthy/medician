package com.robinzon.medicationwizard.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.entities.Medication;

import java.util.ArrayList;
import java.util.List;

public class LogDoseBottomSheet extends MedicationWizardBottomSheet {

    private final List<Medication> allMeds = new ArrayList<>();
    private final List<Medication> filteredMeds = new ArrayList<>();
    private MedAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log_dose, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_meds);
        TextInputEditText searchEdit = view.findViewById(R.id.edit_search_med);

        allMeds.addAll(Medication.getSavedMedications(requireContext()));
        filteredMeds.addAll(allMeds);

        // Listen for new medications added while this sheet is open
        getParentFragmentManager().setFragmentResultListener("medication_added", getViewLifecycleOwner(), (key, bundle) -> {
            refreshMedsList();
        });

        adapter = new MedAdapter(filteredMeds, new MedAdapter.OnActionClickListener() {
            @Override
            public void onTakeNow(Medication med) {
                logDose(med, com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal());
                dismiss();
            }

            @Override
            public void onPickTime(Medication med) {
                showTimePicker(med);
            }

            @Override
            public void onAddNewClicked() {
                // DO NOT dismiss yet, we want to return here
                new AddMedicationBottomSheet().show(getParentFragmentManager(), "AddMedBottomSheet");
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });
    }

    private void filter(String query) {
        filteredMeds.clear();
        if (query.isEmpty()) {
            filteredMeds.addAll(allMeds);
        } else {
            String lower = query.toLowerCase();
            for (Medication m : allMeds) {
                if (m.getCommercialName().toLowerCase().contains(lower)) {
                    filteredMeds.add(m);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void refreshMedsList() {
        allMeds.clear();
        allMeds.addAll(Medication.getSavedMedications(requireContext()));
        filter(""); // Reset search and refresh adapter
    }

    private void logDose(Medication medication, long timestamp) {
        final android.content.Context appContext = requireContext().getApplicationContext();
        DoseInstanceEntity entity = new DoseInstanceEntity();
        entity.setMedicationId(medication.getId());
        entity.setMedicationName(medication.getCommercialName());
        entity.setAmount(medication.getAmount());
        entity.setStrength(medication.getStrength());
        entity.setUnit(medication.getMeasurementUnit() != null ? medication.getMeasurementUnit().getName() : null);
        entity.setForm(medication.getForm() != null ? medication.getForm().name() : null);
        entity.setScheduledTime(timestamp);
        entity.setActionTime(timestamp);
        entity.setStatus("TAKEN");
        entity.setInstruction(medication.getInstruction() != null ? medication.getInstruction().name() : null);
        entity.setPrn(medication.isAsNeeded());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getDatabase(appContext).doseInstanceDao().insert(entity);
            
            // Update last taken timestamp in definition
            medication.setLastTakenTimestamp(timestamp);
            medication.addToMedicationList(appContext);
        });
    }

    private void showTimePicker(Medication medication) {
        java.util.Calendar now = java.util.Calendar.getInstance();
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(now.get(java.util.Calendar.HOUR_OF_DAY))
                .setMinute(now.get(java.util.Calendar.MINUTE))
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTitleText(R.string.button_pick_time)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, picker.getHour());
            cal.set(java.util.Calendar.MINUTE, picker.getMinute());
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);

            // If time selected is in the future, assume it was yesterday (or just log it as is)
            // But usually PRN logging is for past actions.
            logDose(medication, cal.getTimeInMillis());
            dismiss();
        });

        picker.show(getChildFragmentManager(), "LogDoseTimePicker");
    }

    private static class MedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_MED = 0;
        private static final int TYPE_ADD_NEW = 1;

        private final List<Medication> meds;
        private final OnActionClickListener listener;

        MedAdapter(List<Medication> meds, OnActionClickListener listener) {
            this.meds = meds;
            this.listener = listener;
        }

        @Override
        public int getItemViewType(int position) {
            return (position < meds.size()) ? TYPE_MED : TYPE_ADD_NEW;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_MED) {
                return new MedVH(inflater.inflate(R.layout.item_log_dose_med, parent, false));
            } else {
                return new AddNewVH(inflater.inflate(R.layout.item_log_dose_add_new, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof MedVH) {
                Medication m = meds.get(position);
                MedVH medHolder = (MedVH) holder;
                medHolder.name.setText(m.getCommercialName());
                
                String details = "";
                if (m.getStrength() > 0) {
                    details += (m.getStrength() == (long)m.getStrength() ? (long)m.getStrength() : m.getStrength());
                    if (m.getMeasurementUnit() != null) details += " " + m.getMeasurementUnit().getLabel(holder.itemView.getContext());
                    details += " • ";
                }
                if (m.getForm() != null) details += m.getForm().getLabel(holder.itemView.getContext());
                medHolder.details.setText(details);

                // Set icon
                int iconRes = R.drawable.ic_med_pill;
                if (m.getForm() != null) {
                    iconRes = switch (m.getForm()) {
                        case Drops -> R.drawable.ic_med_drops;
                        case Injection -> R.drawable.ic_med_injection;
                        case Solution -> R.drawable.ic_med_solution;
                        case Inhaler -> R.drawable.ic_med_inhaler;
                        case Powder -> R.drawable.ic_med_powder;
                        case Other -> R.drawable.ic_med_other;
                        case Pill -> R.drawable.ic_med_pill;
                    };
                }
                medHolder.icon.setImageResource(iconRes);
                
                medHolder.btnTakeNow.setOnClickListener(v -> listener.onTakeNow(m));
                medHolder.btnPickTime.setOnClickListener(v -> listener.onPickTime(m));
            } else if (holder instanceof AddNewVH) {
                holder.itemView.setOnClickListener(v -> listener.onAddNewClicked());
            }
        }

        @Override
        public int getItemCount() {
            return meds.size() + 1; // Always show Add New at the end
        }

        static class MedVH extends RecyclerView.ViewHolder {
            final TextView name, details;
            final ImageView icon;
            final View btnTakeNow, btnPickTime;
            MedVH(View v) {
                super(v);
                name = v.findViewById(R.id.med_name);
                details = v.findViewById(R.id.med_details);
                icon = v.findViewById(R.id.med_icon);
                btnTakeNow = v.findViewById(R.id.btn_take_now);
                btnPickTime = v.findViewById(R.id.btn_pick_time);
            }
        }

        static class AddNewVH extends RecyclerView.ViewHolder {
            AddNewVH(View v) {
                super(v);
            }
        }

        interface OnActionClickListener {
            void onTakeNow(Medication med);
            void onPickTime(Medication med);
            void onAddNewClicked();
        }
    }
}
