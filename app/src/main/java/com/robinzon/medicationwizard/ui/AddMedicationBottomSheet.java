package com.robinzon.medicationwizard.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.entities.EForm;
import com.robinzon.medicationwizard.entities.EMeasurementUnit;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class AddMedicationBottomSheet extends BottomSheetDialogFragment {

    private LinearLayout timesContainer;
    private final Medication mMedication = new Medication();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_medication, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDropdowns(view);

        timesContainer = view.findViewById(R.id.times_container);

        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            dismiss();
            trySaveMedication(view);
        });


    }

    private void trySaveMedication(View view) {

        //1. Get the input from the medication name input text
        final TextInputEditText nameInputEditText = getNameInputEditText(view);
        try {
            mMedication.setCommercialName(Objects.requireNonNull(nameInputEditText.getText()).toString());
        } catch (NullPointerException ignored) {
        }

        //2. Get the input from the medication amount input text
        final TextInputEditText amountTextView = getAmountInputEditText(view);
        try {
            final float amount = Float.parseFloat(Objects.requireNonNull(amountTextView.getText()).toString());
            mMedication.setAmount(amount > 0 ? amount : 0);
        } catch (NullPointerException | NumberFormatException ignored) {

        }

        // 3. No need to check the form here because the view has a listener to the form chosen.

        // 4. Get the input from the medication frequency input text
        final AutoCompleteTextView frequencyInputEditText = getFrequencyInputEditText(view);
        try {
            final int frequency = Integer.parseInt(Objects.requireNonNull(frequencyInputEditText.getText()).toString());
            //TODO set frequency to med
            mMedication.setDailyFrequency(frequency);
        } catch (NullPointerException | NumberFormatException ignored) {

        }





        if (mMedication.isValid()){
            Logger.log(getClass().getSimpleName(), "Medication is valid");
        } else {
            Logger.log(getClass().getSimpleName(), "Medication is not valid");
        }
    }

    private AutoCompleteTextView getFrequencyInputEditText(View view) {
        return view.findViewById(R.id.dropdown_frequency);
    }


    private boolean isMedicationNowValid() {

        final TextInputEditText amountTextView = getAmountInputEditText(view);
        return true;

    }

    private TextInputEditText getAmountInputEditText(View view) {
        return view.findViewById(R.id.amount);
    }

    private TextInputEditText getNameInputEditText(View view) {
        return view.findViewById(R.id.med_name);
    }

    private void setupDropdowns(View view) {
        // 1. Form Dropdown
        setFormDropDown(view);
        // 2. Unit Dropdown
        setUnitDropDown(view);
        // 3. Frequency Dropdown
        AutoCompleteTextView dropdownFrequency = view.findViewById(R.id.dropdown_frequency);
        String[] frequencies = new String[]{"Once a day", "Twice a day", "3 times a day", "4 times a day", "5 times a day"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencies);
        dropdownFrequency.setAdapter(freqAdapter);

        // When Frequency changes, generate the Time Pickers!
        dropdownFrequency.setOnItemClickListener((parent, view12, position, id) -> {
            int timesPerDay = position + 1;
            generateTimePickers(timesPerDay);
            
        });
    }

    private void setFormDropDown(View view) {
        final AutoCompleteTextView dropdownForm = view.findViewById(R.id.dropdown_form);
        if (null == dropdownForm){
            return;
        }
        String[] forms = Arrays.stream(EForm.values())
                .map(Enum::name)
                .toArray(String[]::new);

        ArrayAdapter<String> formAdapter = null;
        try {
            formAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, forms);
        } catch (IllegalStateException e) {
            return;
        }
        dropdownForm.setAdapter(formAdapter);

        // Optional UX trick: Change the start icon when they pick a form
        final TextInputLayout layoutForm = view.findViewById(R.id.layout_med_form);
        if (null == layoutForm){
            return;
        }
        dropdownForm.setOnItemClickListener((parent, view1, position, id) -> {
            final EForm form = EForm.values()[position];
            mMedication.setForm(form);
            switch (form) {
                case Pill -> layoutForm.setStartIconDrawable(R.drawable.ic_med_pill);
                case Drops -> layoutForm.setStartIconDrawable(R.drawable.ic_med_drops);
                case Injection -> layoutForm.setStartIconDrawable(R.drawable.ic_med_injection);
                case Solution -> layoutForm.setStartIconDrawable(R.drawable.ic_med_solution);
                case Inhaler -> layoutForm.setStartIconDrawable(R.drawable.ic_med_inhaler);
                case Powder -> layoutForm.setStartIconDrawable(R.drawable.ic_med_powder);
                case Other -> layoutForm.setStartIconDrawable(R.drawable.ic_med_other);
            }
        });
    }

    private void setUnitDropDown(View view) {
        final AutoCompleteTextView dropdownUnit = view.findViewById(R.id.dropdown_unit);
        final ArrayList<String> measurementUnits = new ArrayList<>();
        for (EMeasurementUnit unit : EMeasurementUnit.values()) {
            final String unitShortName = unit.getName();
            measurementUnits.add(unitShortName);
        }
        ArrayAdapter<String> unitAdapter = null;
        try {
            unitAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, measurementUnits);
        } catch (IllegalStateException e) {
            return;
        }
        if (null != dropdownUnit) {
            dropdownUnit.setAdapter(unitAdapter);
            dropdownUnit.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    EMeasurementUnit measurementUnit = EMeasurementUnit.values()[i];
                    mMedication.setMeasurementUnit(measurementUnit);
                }
            });
        }
    }

    private void generateTimePickers(int amount) {
        timesContainer.removeAllViews(); // Clear old buttons

        for (int i = 1; i <= amount; i++) {
            // Create a nice Material 3 Button for each time slot
            Button timeButton = new Button(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            timeButton.setText("Select Time " + i);

            // Set margins so they aren't squished together
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            timeButton.setLayoutParams(params);

            // Open the Time Picker when clicked
            timeButton.setOnClickListener(v -> showTimePicker(timeButton));

            timesContainer.addView(timeButton);
        }
    }

    private void showTimePicker(Button buttonToUpdate) {
        // Build the modern Android Time Picker
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H) // Or CLOCK_12H
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Medication Time")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            // Format the time nicely (e.g., 08:30)
            String formattedTime = String.format("%02d:%02d", picker.getHour(), picker.getMinute());
            buttonToUpdate.setText("Time set: " + formattedTime);
        });

        picker.show(getParentFragmentManager(), "timePicker");
    }
}