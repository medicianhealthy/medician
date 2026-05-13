package com.robinzon.medicationwizard.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.entities.EForm;
import com.robinzon.medicationwizard.entities.EInstructions;
import com.robinzon.medicationwizard.entities.EMeasurementUnit;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.SimpleDayTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AddMedicationBottomSheet extends BottomSheetDialogFragment {

    private final SparseArray<SimpleDayTime> mTimesInDay = new SparseArray<>();
    private LinearLayout timesContainer;
    private final Medication mMedication = new Medication();
    private boolean mSaveWasHitAlready;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_medication, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDropdowns(view);
        setupInstructions(view);
        setTextChangeListeners(view);
        timesContainer = view.findViewById(R.id.times_container);

        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            trySaveMedication(view);
            mSaveWasHitAlready = true;
        });


    }


    private void setTextChangeListeners(@NonNull final View mainView) {
        final TextInputEditText commercialNameInputEditText = getCommercialNameInputEditText(mainView);
        if (null != commercialNameInputEditText) {
            commercialNameInputEditText.addTextChangedListener(getTextWatcher(commercialNameInputEditText, mainView));
        }

        final TextInputEditText amountEditText = getAmountInputEditText(mainView);
        if (null != amountEditText) {
            amountEditText.addTextChangedListener(getTextWatcher(amountEditText, mainView));
        }

        final AutoCompleteTextView frequencyEditText = getFrequencyInputEditText(mainView);
        if (null != frequencyEditText) {
            frequencyEditText.addTextChangedListener(getTextWatcher(frequencyEditText, mainView));
        }

        final AutoCompleteTextView formEditText = getFormInputEditText(mainView);
        if (null != formEditText) {
            formEditText.addTextChangedListener(getTextWatcher(formEditText, mainView));
        }

    }

    private TextWatcher getTextWatcher(@NonNull final EditText editText, @NonNull View mainView) {
        return new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                // 1. Get the trimmed string once
                final String trimmedInput = editable.toString().trim();

                // 2. Handle validation if the user already tried to save
                if (mSaveWasHitAlready) {
                    final int editTextId = editText.getId();
                    if (editTextId == R.id.med_name_fragment_add_med) {
                        highLightInValidFields(editText, trimmedInput.isEmpty(), false);
                    } else if (editTextId == R.id.med_amount) {
                        float amountVal = 0;
                        try { amountVal = Float.parseFloat(trimmedInput); } catch (Exception ignored) {}
                        highLightInValidFields(editText, amountVal <= 0, false);
                    } else if (editTextId == R.id.med_form_fragment_add_med || editTextId == R.id.med_frequency_fragment_add_med) {
                        highLightInValidFields(editText, trimmedInput.isEmpty(), false);
                    }
                }

                // 3. Update the Title live (only if we are editing the Name field)
                TextView titleView = getTitleTextView(mainView);
                if (titleView != null && editText.getId() == R.id.med_name_fragment_add_med) {
                    if (trimmedInput.isEmpty()){
                        titleView.setText(R.string.fragment_add_med_title);
                    } else {
                        titleView.setText(trimmedInput);
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
        };
    }



    private void setupInstructions(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_instructions);
        if (chipGroup == null) return;

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                mMedication.setInstruction(null);
                return;
            }

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_before_eating) {
                mMedication.setInstruction(EInstructions.BEFORE_EATING);
            } else if (checkedId == R.id.chip_after_eating) {
                mMedication.setInstruction(EInstructions.AFTER_EATING);
            } else if (checkedId == R.id.chip_while_eating) {
                mMedication.setInstruction(EInstructions.WHILE_EATING);
            } else if (checkedId == R.id.chip_before_sleep) {
                mMedication.setInstruction(EInstructions.BEFORE_SLEEP);
            } else if (checkedId == R.id.chip_does_not_matter) {
                mMedication.setInstruction(EInstructions.DOES_NOT_MATTER);
            }
        });
    }


    private void setupDropdowns(View view) {
        // 1. Form Dropdown
        setFormDropDown(view);
        // 2. Unit Dropdown
        setUnitDropDown(view);
        // 3. Frequency Dropdown
        AutoCompleteTextView dropdownFrequency = view.findViewById(R.id.med_frequency_fragment_add_med);
        String[] frequencies = new String[]{"Once a day", "Twice a day", "3 times a day", "4 times a day", "5 times a day"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencies);
        dropdownFrequency.setAdapter(freqAdapter);

        // When Frequency changes, generate the Time Pickers!
        dropdownFrequency.setOnItemClickListener((parent, view12, position, id) -> {
            int timesPerDay = position + 1;
            mMedication.setDailyFrequency(timesPerDay);
            generateTimePickers(timesPerDay);
        });
    }

    private void trySaveMedication(@NonNull View mainView) {

        populateMedication(mainView);

        if (mMedication.isValid()) {
            Logger.log(getClass().getSimpleName(), "Medication is valid");
        } else {
            showInputErrorsOnViews(mainView);
            Logger.log(getClass().getSimpleName(), "Medication is not valid");
            return;
        }

        if (mMedication.getDailyFrequency() != mTimesInDay.size()) {
            showErrorDialog("Pick a time", "Just hit the select time button and pick a time");
            Logger.log(getClass().getSimpleName(), "Medication is not valid");
            return;
        }

        // Check for duplicate times
        Set<SimpleDayTime> uniqueTimes = new HashSet<>();
        for (int i = 0; i < mTimesInDay.size(); i++) {
            if (!uniqueTimes.add(mTimesInDay.valueAt(i))) {
                showErrorDialog("Duplicate Times", "You have selected the same time more than once. Please choose different times.");
                return;
            }
        }

        mMedication.addTimeStampsForDay(mTimesInDay);
        if (mMedication.getDailyFrequency() != mMedication.getTimesADay().size()) {
            Logger.log(getClass().getSimpleName(), "Medication is not valid");
            return;
        }
        Logger.log(getClass().getSimpleName(), "Medication is valid");
        mMedication.addToMedicationList(getContext());
        dismiss();
    }

    private void showInputErrorsOnViews(@NonNull View mainView) {
        if (TextUtils.isEmpty(mMedication.getCommercialName())) {
            highLightInValidFields(getCommercialNameInputEditText(mainView), true);
        }
        if (mMedication.getAmount() == 0) {
            highLightInValidFields(getAmountInputEditText(mainView), true);
        }
        if (mMedication.getDailyFrequency() == 0) {
            highLightInValidFields(getFrequencyInputEditText(mainView), true);
        }
        if (mMedication.getForm() == null) {
            highLightInValidFields(getFormInputEditText(mainView), true);
        }
    }

    /**
     * Shows a robust Material Design 3 error dialog with an optional action button.
     *
     * @param title          The dialog title.
     * @param message        The dialog message.
     * @param actionText     (Optional) The text for the custom action button. Pass null to hide.
     * @param actionCallback (Optional) The callback to execute when the action is clicked. Pass null to hide.
     */
    private void showErrorDialog(@NonNull String title, @NonNull String message,
                                 @Nullable String actionText, @Nullable Runnable actionCallback) {

        // 1. Robustness check: Ensure the fragment is attached and has a context
        Context context = getContext();
        if (context == null) {
            Logger.log(getClass().getSimpleName(), "Cannot show dialog: Context is null");
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message);

        // 2. Configure buttons based on whether an action was provided
        if (actionText != null && actionCallback != null) {
            // M3 Standard: The primary action is the Positive Button, "OK/Cancel" is the Negative Button
            builder.setPositiveButton(actionText, (dialog, which) -> {
                actionCallback.run();
                dialog.dismiss();
            });
            // Using Android's native localized "OK" string
            builder.setNegativeButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        } else {
            // If no action is provided, just show an "OK" button to dismiss
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                dialog.dismiss();
            });
        }

        // 3. Robustness check: Catch BadTokenExceptions (happens if the activity is closing)
        try {
            builder.show();
        } catch (Exception e) {
            Logger.log(getClass().getSimpleName(), "Failed to show error dialog: " + e.getMessage());
        }
    }

    /**
     * Overloaded helper method for simple error dialogs that only need an "OK" dismiss button.
     */
    private void showErrorDialog(@NonNull String title, @NonNull String message) {
        showErrorDialog(title, message, null, null);
    }


// ... inside your AddMed class ...

    private void highLightInValidFields(@Nullable View view, boolean isInvalid) {
        highLightInValidFields(view, isInvalid, true);
    }

    private void highLightInValidFields(@Nullable View view, boolean isInvalid, boolean animate) {
        if (null == view) {
            return;
        }

        // 1. Try to find the parent TextInputLayout
        // Material components often wrap the EditText in a FrameLayout inside the TextInputLayout
        TextInputLayout layout = null;
        if (view instanceof TextInputLayout) {
            layout = (TextInputLayout) view;
        } else if (view.getParent() instanceof TextInputLayout) {
            layout = (TextInputLayout) view.getParent();
        } else if (view.getParent().getParent() instanceof TextInputLayout) {
            layout = (TextInputLayout) view.getParent().getParent();
        }

        if (isInvalid) {
            if (animate) {
                // Shake the whole layout if it exists, otherwise just the view
                View target = (layout != null) ? layout : view;
                ObjectAnimator animator = ObjectAnimator.ofFloat(target, "translationX", 0f, 15f, -15f, 10f, -10f, 5f, -5f, 0f);
                animator.setDuration(400);
                animator.start();
            }

            if (layout != null) {
                // 2. The Material Fix: Use setError.
                // This turns the border red and STAYS red even when focused.
                layout.setError("Required"); // Or pass a specific message
                layout.setErrorEnabled(true);
            } else {
                // Fallback for views not wrapped in TextInputLayout
                if (view.getTag() == null) {
                    view.setTag(view.getBackground());
                }
                view.setBackground(ContextCompat.getDrawable(view.getContext(), R.drawable.error_background));
            }

        } else {
            // Reset state
            if (layout != null) {
                layout.setError(null);
                layout.setErrorEnabled(false);
            } else {
                if (view.getTag() instanceof Drawable) {
                    view.setBackground((Drawable) view.getTag());
                }
            }
        }
    }

    private void setFormDropDown(View view) {
        final AutoCompleteTextView dropdownForm = view.findViewById(R.id.med_form_fragment_add_med);
        if (null == dropdownForm) {
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
        if (null == layoutForm) {
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

    private void generateTimePickers(final int amount) {
        timesContainer.removeAllViews(); // Clear old buttons

        for (int i = 1; i <= amount; i++) {

            // Inflate our beautiful custom M3 Tonal Button
            Button timeButton = (Button) LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_time_picker, timesContainer, false);

            timeButton.setText("Select Time " + i);

            // Open the Time Picker when clicked
            int finalIndex = i;
            timeButton.setOnClickListener(v -> {
                showTimePicker(finalIndex, timeButton);
            });

            timesContainer.addView(timeButton);
        }
    }

    private void populateMedication(@NonNull View view) {
        //1. Get the input from the medication name input text
        final TextInputEditText nameInputEditText = getCommercialNameInputEditText(view);
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

        // 4. No need to check the frequency here because the view has a listener to the form chosen.

        // 5. Strength
        final TextInputEditText strengthEditText = view.findViewById(R.id.medication_strength);
        if (strengthEditText != null && strengthEditText.getText() != null) {
            try {
                mMedication.setStrength(Float.parseFloat(strengthEditText.getText().toString()));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void showTimePicker(final int finalIndex, final Button buttonToUpdate) {
        // Build the modern Android Time Picker
        // TODO time format like the system
        final MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H) // Or CLOCK_12H
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Medication Time")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            // Format the time nicely (e.g., 08:30)
            String formattedTime = String.format("%02d:%02d", picker.getHour(), picker.getMinute());
            buttonToUpdate.setText("Time set: " + formattedTime);
            mTimesInDay.put(finalIndex, new SimpleDayTime((byte) picker.getHour(), (byte) picker.getMinute()));
        });

        picker.show(getParentFragmentManager(), "timePicker");
    }

    @Nullable
    private TextInputEditText getCommercialNameInputEditText(@NonNull View view) {
        return view.findViewById(R.id.med_name_fragment_add_med);
    }

    @Nullable
    private TextInputEditText getAmountInputEditText(@NonNull View view) {
        return view.findViewById(R.id.med_amount);
    }

    @Nullable
    private AutoCompleteTextView getFrequencyInputEditText(@NonNull View view) {
        return view.findViewById(R.id.med_frequency_fragment_add_med);
    }

    @Nullable
    private AutoCompleteTextView getFormInputEditText(@NonNull View view) {
        return view.findViewById(R.id.med_form_fragment_add_med);
    }

    @Nullable
    private TextView getTitleTextView(View mainView) {
        return mainView.findViewById(R.id.title_fragment_add_med);
    }
}