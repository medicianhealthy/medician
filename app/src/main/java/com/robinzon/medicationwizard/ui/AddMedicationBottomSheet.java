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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * A highly interactive Material 3 BottomSheet for adding or editing medications.
 * <p>
 * This dialog handles the complex data entry flow for a {@link Medication} object, including:
 * - Real-time validation of fields (Name, Amount, Frequency).
 * - Dynamic generation of time picker buttons based on dose frequency.
 * - Integration with modern Material components like AutoComplete dropdowns and Chips.
 * - Dual-mode operation: "New Medication" and "Edit Medication".
 * </p>
 */
public class AddMedicationBottomSheet extends BottomSheetDialogFragment {

    /** Map of dose index to the user-selected time. */
    private final SparseArray<SimpleDayTime> mTimesInDay = new SparseArray<>();
    
    /** Layout container where dynamic time picker buttons are added. */
    private LinearLayout timesContainer;
    
    /** The medication object being built or edited. */
    private Medication mMedication = new Medication();
    
    /** Flag to enable aggressive validation after the first save attempt. */
    private boolean mSaveWasHitAlready;
    
    /** True if the sheet is in 'Edit' mode (modifying an existing medication). */
    private boolean mIsEditMode = false;

    /**
     * Factory method to create a new instance of the sheet, optionally pre-filled with 
     * medication data for editing.
     *
     * @param medication The medication to edit, or null for a new entry.
     * @return A configured fragment instance.
     */
    public static AddMedicationBottomSheet newInstance(@Nullable Medication medication) {
        AddMedicationBottomSheet fragment = new AddMedicationBottomSheet();
        if (medication != null) {
            Bundle args = new Bundle();
            args.putString("medication_json", medication.toJson().toString());
            fragment.setArguments(args);
        }
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_medication, container, false);
    }

    /**
     * Initializes the UI. 
     * Handles argument parsing for Edit mode and sets up all interactive listeners.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check for Edit Mode data
        if (getArguments() != null && getArguments().containsKey("medication_json")) {
            try {
                String json = getArguments().getString("medication_json");
                if (json != null) {
                    mMedication = Medication.fromJson(new JSONObject(json));
                    mIsEditMode = true;
                }
            } catch (JSONException ignored) {}
        }

        setupDropdowns(view);
        setupInstructions(view);
        setTextChangeListeners(view);
        timesContainer = view.findViewById(R.id.times_container);

        if (mIsEditMode) {
            preFillData(view);
        }

        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            trySaveMedication(view);
            mSaveWasHitAlready = true;
        });
    }

    /**
     * Populates all UI fields with data from an existing medication. 
     * Used exclusively in Edit mode.
     */
    private void preFillData(View view) {
        TextView titleView = getTitleTextView(view);
        if (titleView != null) titleView.setText(mMedication.getCommercialName());

        TextInputEditText nameEdit = getCommercialNameInputEditText(view);
        if (nameEdit != null) nameEdit.setText(mMedication.getCommercialName());

        TextInputEditText amountEdit = getAmountInputEditText(view);
        if (amountEdit != null) {
            String val = mMedication.getAmount() == (long) mMedication.getAmount() ?
                    String.valueOf((long) mMedication.getAmount()) : String.valueOf(mMedication.getAmount());
            amountEdit.setText(val);
        }

        AutoCompleteTextView formEdit = getFormInputEditText(view);
        if (formEdit != null && mMedication.getForm() != null) {
            formEdit.setText(mMedication.getForm().name(), false);
            final TextInputLayout layoutForm = view.findViewById(R.id.layout_med_form);
            if (layoutForm != null) {
                switch (mMedication.getForm()) {
                    case Pill -> layoutForm.setStartIconDrawable(R.drawable.ic_med_pill);
                    case Drops -> layoutForm.setStartIconDrawable(R.drawable.ic_med_drops);
                    case Injection -> layoutForm.setStartIconDrawable(R.drawable.ic_med_injection);
                    case Solution -> layoutForm.setStartIconDrawable(R.drawable.ic_med_solution);
                    case Inhaler -> layoutForm.setStartIconDrawable(R.drawable.ic_med_inhaler);
                    case Powder -> layoutForm.setStartIconDrawable(R.drawable.ic_med_powder);
                    case Other -> layoutForm.setStartIconDrawable(R.drawable.ic_med_other);
                }
            }
        }

        AutoCompleteTextView freqEdit = getFrequencyInputEditText(view);
        if (freqEdit != null && mMedication.getDailyFrequency() > 0) {
            String[] frequencies = new String[]{"Once a day", "Twice a day", "3 times a day", "4 times a day", "5 times a day"};
            if (mMedication.getDailyFrequency() <= frequencies.length) {
                freqEdit.setText(frequencies[mMedication.getDailyFrequency() - 1], false);
            }
            generateTimePickers(mMedication.getDailyFrequency());
            if (mMedication.getTimesADay() != null) {
                for (int i = 0; i < mMedication.getTimesADay().size(); i++) {
                    int key = mMedication.getTimesADay().keyAt(i);
                    SimpleDayTime time = mMedication.getTimesADay().valueAt(i);
                    mTimesInDay.put(key, time);
                    if (i < timesContainer.getChildCount()) {
                        Button btn = (Button) timesContainer.getChildAt(i);
                        btn.setText("Time set: " + time.toString());
                    }
                }
            }
        }

        TextInputEditText strengthEdit = view.findViewById(R.id.medication_strength);
        if (strengthEdit != null) {
            String val = mMedication.getStrength() == (long) mMedication.getStrength() ?
                    String.valueOf((long) mMedication.getStrength()) : String.valueOf(mMedication.getStrength());
            strengthEdit.setText(val);
        }

        AutoCompleteTextView unitEdit = view.findViewById(R.id.dropdown_unit);
        if (unitEdit != null && mMedication.getMeasurementUnit() != null) {
            unitEdit.setText(mMedication.getMeasurementUnit().getName(), false);
        }

        ChipGroup chipGroup = view.findViewById(R.id.chip_group_instructions);
        if (chipGroup != null && mMedication.getInstruction() != null) {
            int chipId = switch (mMedication.getInstruction()) {
                case BEFORE_EATING -> R.id.chip_before_eating;
                case AFTER_EATING -> R.id.chip_after_eating;
                case WHILE_EATING -> R.id.chip_while_eating;
                case BEFORE_SLEEP -> R.id.chip_before_sleep;
                case DOES_NOT_MATTER -> R.id.chip_does_not_matter;
            };
            chipGroup.check(chipId);
        }
    }


    /**
     * Attaches live validators to the text input fields.
     */
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

    /**
     * Factory for {@link TextWatcher} that handles both error highlighting and 
     * live title updates.
     */
    private TextWatcher getTextWatcher(@NonNull final EditText editText, @NonNull View mainView) {
        return new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                final String trimmedInput = editable.toString().trim();

                // Aggressive validation logic after first failed save attempt
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

                // Update the bottom sheet title live as the user types the name
                TextView titleView = getTitleTextView(mainView);
                if (titleView != null && editText.getId() == R.id.med_name_fragment_add_med) {
                    if (trimmedInput.isEmpty()){
                        titleView.setText(R.string.fragment_add_med_title);
                    } else {
                        titleView.setText(trimmedInput);
                    }
                }
            }

            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        };
    }



    /**
     * Configures the ChipGroup for consumption instructions.
     */
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


    /**
     * Initializes all AutoComplete dropdown menus (Form, Unit, Frequency).
     */
    private void setupDropdowns(View view) {
        setFormDropDown(view);
        setUnitDropDown(view);
        
        AutoCompleteTextView dropdownFrequency = view.findViewById(R.id.med_frequency_fragment_add_med);
        String[] frequencies = new String[]{"Once a day", "Twice a day", "3 times a day", "4 times a day", "5 times a day"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencies);
        dropdownFrequency.setAdapter(freqAdapter);

        // When Frequency changes, we must regenerate the specific time picker buttons
        dropdownFrequency.setOnItemClickListener((parent, view12, position, id) -> {
            int timesPerDay = position + 1;
            mMedication.setDailyFrequency(timesPerDay);
            generateTimePickers(timesPerDay);
        });
    }

    /**
     * Final validation and save logic.
     * <p>
     * Checks:
     * 1. Basic field validity via {@link Medication#isValid()}.
     * 2. Matches frequency to picked times (e.g., if "Twice a day", must have 2 times).
     * 3. Ensures no duplicate times are picked.
     * 4. Persists the medication and dismisses the sheet.
     * </p>
     */
    private void trySaveMedication(@NonNull View mainView) {

        populateMedication(mainView);

        if (!mMedication.isValid()) {
            showInputErrorsOnViews(mainView);
            return;
        }

        if (mMedication.getDailyFrequency() != mTimesInDay.size()) {
            showErrorDialog("Pick a time", "Just hit the select time button and pick a time");
            return;
        }

        // Logical validation: Doses must be at different times
        Set<SimpleDayTime> uniqueTimes = new HashSet<>();
        for (int i = 0; i < mTimesInDay.size(); i++) {
            if (!uniqueTimes.add(mTimesInDay.valueAt(i))) {
                showErrorDialog("Duplicate Times", "You have selected the same time more than once. Please choose different times.");
                return;
            }
        }

        mMedication.addTimeStampsForDay(mTimesInDay);
        mMedication.addToMedicationList(getContext());

        // Proactively ask for notification permissions on Android 13+ with a friendly rationale
        if (getActivity() != null) {
            com.robinzon.medicationwizard.notifications.NotificationManager.getInstance(getActivity()).requestWithRationale();
        }

        dismiss();
    }

    /**
     * Highlights all invalid fields with a shake animation and error text.
     */
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
     * Shows a robust Material Design 3 error dialog.
     */
    private void showErrorDialog(@NonNull String title, @NonNull String message) {
        Context context = getContext();
        if (context == null) return;

        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void highLightInValidFields(@Nullable View view, boolean isInvalid) {
        highLightInValidFields(view, isInvalid, true);
    }

    /**
     * Applies Material 3 error styling to a specific input field.
     * Uses {@link TextInputLayout#setError(CharSequence)} for standard Material feedback.
     */
    private void highLightInValidFields(@Nullable View view, boolean isInvalid, boolean animate) {
        if (null == view) return;

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
                View target = (layout != null) ? layout : view;
                ObjectAnimator animator = ObjectAnimator.ofFloat(target, "translationX", 0f, 15f, -15f, 10f, -10f, 5f, -5f, 0f);
                animator.setDuration(400);
                animator.start();
            }

            if (layout != null) {
                layout.setError("Required");
                layout.setErrorEnabled(true);
            }
        } else {
            if (layout != null) {
                layout.setError(null);
                layout.setErrorEnabled(false);
            }
        }
    }

    /**
     * Configures the drug form dropdown and updates the start icon dynamically 
     * based on selection (e.g., show a pill icon for "Pill").
     */
    private void setFormDropDown(View view) {
        final AutoCompleteTextView dropdownForm = view.findViewById(R.id.med_form_fragment_add_med);
        if (null == dropdownForm) return;

        String[] forms = Arrays.stream(EForm.values())
                .map(Enum::name)
                .toArray(String[]::new);

        dropdownForm.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, forms));

        final TextInputLayout layoutForm = view.findViewById(R.id.layout_med_form);
        if (null == layoutForm) return;

        dropdownForm.setOnItemClickListener((parent, view1, position, id) -> {
            final EForm form = EForm.values()[position];
            mMedication.setForm(form);
            int icon = switch (form) {
                case Pill -> R.drawable.ic_med_pill;
                case Drops -> R.drawable.ic_med_drops;
                case Injection -> R.drawable.ic_med_injection;
                case Solution -> R.drawable.ic_med_solution;
                case Inhaler -> R.drawable.ic_med_inhaler;
                case Powder -> R.drawable.ic_med_powder;
                case Other -> R.drawable.ic_med_other;
            };
            layoutForm.setStartIconDrawable(icon);
        });
    }

    private void setUnitDropDown(View view) {
        final AutoCompleteTextView dropdownUnit = view.findViewById(R.id.dropdown_unit);
        final ArrayList<String> measurementUnits = new ArrayList<>();
        for (EMeasurementUnit unit : EMeasurementUnit.values()) {
            measurementUnits.add(unit.getName());
        }
        
        if (null != dropdownUnit) {
            dropdownUnit.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, measurementUnits));
            dropdownUnit.setOnItemClickListener((adapterView, view1, i, l) -> {
                mMedication.setMeasurementUnit(EMeasurementUnit.values()[i]);
            });
        }
    }

    /**
     * Generates a list of buttons in the UI, one for each scheduled dose 
     * defined by the 'Frequency' dropdown.
     */
    private void generateTimePickers(final int amount) {
        timesContainer.removeAllViews(); 

        for (int i = 1; i <= amount; i++) {
            Button timeButton = (Button) LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_time_picker, timesContainer, false);

            timeButton.setText("Select Time " + i);
            int finalIndex = i;
            timeButton.setOnClickListener(v -> showTimePicker(finalIndex, timeButton));
            timesContainer.addView(timeButton);
        }
    }

    /**
     * Scrapes all manual text entry fields and populates the medication object.
     */
    private void populateMedication(@NonNull View view) {
        final TextInputEditText nameInputEditText = getCommercialNameInputEditText(view);
        if (nameInputEditText != null && nameInputEditText.getText() != null) {
            mMedication.setCommercialName(nameInputEditText.getText().toString());
        }

        final TextInputEditText amountTextView = getAmountInputEditText(view);
        if (amountTextView != null && amountTextView.getText() != null) {
            try {
                mMedication.setAmount(Float.parseFloat(amountTextView.getText().toString()));
            } catch (NumberFormatException ignored) {}
        }

        final TextInputEditText strengthEditText = view.findViewById(R.id.medication_strength);
        if (strengthEditText != null && strengthEditText.getText() != null) {
            try {
                mMedication.setStrength(Float.parseFloat(strengthEditText.getText().toString()));
            } catch (NumberFormatException ignored) {}
        }
    }

    /**
     * Opens the standard Android Material Time Picker. 
     * Upon confirmation, updates the medication's schedule and the button text.
     */
    private void showTimePicker(final int finalIndex, final Button buttonToUpdate) {
        final MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Medication Time")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute());
            buttonToUpdate.setText("Time set: " + formattedTime);
            mTimesInDay.put(finalIndex, new SimpleDayTime((byte) picker.getHour(), (byte) picker.getMinute()));
        });

        picker.show(getParentFragmentManager(), "timePicker");
    }

    @Nullable private TextInputEditText getCommercialNameInputEditText(@NonNull View view) { return view.findViewById(R.id.med_name_fragment_add_med); }
    @Nullable private TextInputEditText getAmountInputEditText(@NonNull View view) { return view.findViewById(R.id.med_amount); }
    @Nullable private AutoCompleteTextView getFrequencyInputEditText(@NonNull View view) { return view.findViewById(R.id.med_frequency_fragment_add_med); }
    @Nullable private AutoCompleteTextView getFormInputEditText(@NonNull View view) { return view.findViewById(R.id.med_form_fragment_add_med); }
    @Nullable private TextView getTitleTextView(View mainView) { return mainView.findViewById(R.id.title_fragment_add_med); }
}