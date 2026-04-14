package com.robinzon.medicationwizard.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.robinzon.medicationwizard.R;

public class AddMedicationBottomSheet extends BottomSheetDialogFragment {

    private LinearLayout timesContainer;

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

        view.findViewById(R.id.btn_save).setOnClickListener(v -> dismiss());
    }

    private void setupDropdowns(View view) {
        // 1. Form Dropdown
        AutoCompleteTextView dropdownForm = view.findViewById(R.id.dropdown_form);
        String[] forms = new String[]{"Pill", "Drop", "Injection", "Powder", "Inhaler"};
        ArrayAdapter<String> formAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, forms);
        dropdownForm.setAdapter(formAdapter);

        // Optional UX trick: Change the start icon when they pick a form
        TextInputLayout layoutForm = view.findViewById(R.id.layout_med_form);
        dropdownForm.setOnItemClickListener((parent, view1, position, id) -> {
            String selected = forms[position];
            // You can add your own custom drawable icons here!
            // if (selected.equals("Pill")) layoutForm.setStartIconDrawable(R.drawable.ic_pill);
        });

        // 2. Unit Dropdown
        AutoCompleteTextView dropdownUnit = view.findViewById(R.id.dropdown_unit);
        String[] units = new String[]{"mg", "g", "ml", "drops", "IU", "mcg"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, units);
        dropdownUnit.setAdapter(unitAdapter);

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