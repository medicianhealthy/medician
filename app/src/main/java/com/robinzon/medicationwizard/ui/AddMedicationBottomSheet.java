package com.robinzon.medicationwizard.ui;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.entities.EForm;
import com.robinzon.medicationwizard.entities.EInstructions;
import com.robinzon.medicationwizard.entities.EMeasurementUnit;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.utils.SimpleDayTime;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
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
 * <p>
 * Performance: Uses customized window attributes to ensure a sharp, centered 
 * card look that opens fully on all devices.
 * </p>
 */
public class AddMedicationBottomSheet extends BottomSheetDialogFragment {

    /**
     * Standard lifecycle method to define the dialog's visual style.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog);
    }

    /**
     * Configures the behavior and constraints of the bottom sheet once it starts.
     * <p>
     * Performance: Forces full expansion and adheres to Material 3 standard anchoring.
     * </p>
     */
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    /** Map of dose index to the user-selected time. */
    private final SparseArray<SimpleDayTime> dosesInDay = new SparseArray<>();
    
    /** Layout container where dynamic time picker buttons are added. */
    private LinearLayout timesContainer;
    
    /** The medication object being built or edited. */
    private Medication medication = new Medication();
    
    /** Flag to enable aggressive validation after the first save attempt. */
    private boolean hasAttemptedSave;
    
    /** True if the sheet is in 'Edit' mode (modifying an existing medication). */
    private boolean isEditMode = false;

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
                    medication = Medication.fromJson(new JSONObject(json));
                    isEditMode = true;
                }
            } catch (JSONException ignored) {}
        }

        setupDropdowns(view);
        
        // Fix for disappearing hint: explicitly re-set it after dropdown setup
        TextInputLayout layoutForm = view.findViewById(R.id.layout_med_form);
        if (layoutForm != null) {
            layoutForm.setHint(R.string.hint_form);
        }

        setupInstructions(view);
        setTextChangeListeners(view);
        timesContainer = view.findViewById(R.id.times_container);

        if (isEditMode) {
            preFillData(view);
        }

        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            trySaveMedication(view);
            hasAttemptedSave = true;
        });
    }

    /**
     * Populates all UI fields with data from an existing medication. 
     * Used exclusively in Edit mode.
     */
    private void preFillData(View view) {
        TextView titleView = getTitleTextView(view);
        if (titleView != null) titleView.setText(medication.getCommercialName());

        TextInputEditText nameEdit = getCommercialNameInputEditText(view);
        if (nameEdit != null) nameEdit.setText(medication.getCommercialName());

        TextInputEditText amountEdit = getAmountInputEditText(view);
        if (amountEdit != null) {
            String val = medication.getAmount() == (long) medication.getAmount() ?
                    String.valueOf((long) medication.getAmount()) : String.valueOf(medication.getAmount());
            amountEdit.setText(val);
        }

        AutoCompleteTextView formEdit = getFormInputEditText(view);
        if (formEdit != null && medication.getForm() != null) {
            formEdit.setText(medication.getForm().name(), false);
            final TextInputLayout layoutForm = view.findViewById(R.id.layout_med_form);
            if (layoutForm != null) {
                switch (medication.getForm()) {
                    case Pill -> layoutForm.setStartIconDrawable(R.drawable.ic_med_pill);
                    case Drops -> layoutForm.setStartIconDrawable(R.drawable.ic_med_drops);
                    case Injection -> layoutForm.setStartIconDrawable(R.drawable.ic_med_injection);
                    case Solution -> layoutForm.setStartIconDrawable(R.drawable.ic_med_solution);
                    case Inhaler -> layoutForm.setStartIconDrawable(R.drawable.ic_med_inhaler);
                    case Powder -> layoutForm.setStartIconDrawable(R.drawable.ic_med_powder);
                    case Other -> layoutForm.setStartIconDrawable(R.drawable.ic_med_other);
                }
                int onSurfaceAttr = com.google.android.material.R.attr.colorOnSurface;
                int iconColor = com.google.android.material.color.MaterialColors.getColor(requireContext(), onSurfaceAttr, Color.BLACK);
                layoutForm.setStartIconTintList(ColorStateList.valueOf(iconColor));
            }
        }

        AutoCompleteTextView freqEdit = getFrequencyInputEditText(view);
        if (freqEdit != null && medication.getDailyFrequency() > 0) {
            String[] frequencies = new String[]{
                    getString(R.string.frequency_once),
                    getString(R.string.frequency_twice),
                    getString(R.string.frequency_3_times),
                    getString(R.string.frequency_4_times),
                    getString(R.string.frequency_5_times)
            };
            if (medication.getDailyFrequency() <= frequencies.length) {
                freqEdit.setText(frequencies[medication.getDailyFrequency() - 1], false);
            }
            generateTimePickers(medication.getDailyFrequency());
            if (medication.getTimesADay() != null) {
                for (int k = 0; k < medication.getTimesADay().size(); k++) {
                    int key = medication.getTimesADay().keyAt(k);
                    SimpleDayTime time = medication.getTimesADay().valueAt(k);
                    dosesInDay.put(key, time);
                    if (k < timesContainer.getChildCount()) {
                        Button btn = (Button) timesContainer.getChildAt(k);
                        btn.setText(getString(R.string.time_set_format, time.toString()));
                    }
                }
            }
        }

        TextInputEditText strengthEdit = view.findViewById(R.id.medication_strength);
        if (strengthEdit != null) {
            String val = medication.getStrength() == (long) medication.getStrength() ?
                    String.valueOf((long) medication.getStrength()) : String.valueOf(medication.getStrength());
            strengthEdit.setText(val);
        }

        AutoCompleteTextView unitEdit = view.findViewById(R.id.dropdown_unit);
        if (unitEdit != null && medication.getMeasurementUnit() != null) {
            unitEdit.setText(medication.getMeasurementUnit().getName(), false);
        }

        ChipGroup chipGroup = view.findViewById(R.id.chip_group_instructions);
        if (chipGroup != null && medication.getInstruction() != null) {
            int chipId = switch (medication.getInstruction()) {
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
                if (hasAttemptedSave) {
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

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
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
                medication.setInstruction(null);
                return;
            }

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_before_eating) {
                medication.setInstruction(EInstructions.BEFORE_EATING);
            } else if (checkedId == R.id.chip_after_eating) {
                medication.setInstruction(EInstructions.AFTER_EATING);
            } else if (checkedId == R.id.chip_while_eating) {
                medication.setInstruction(EInstructions.WHILE_EATING);
            } else if (checkedId == R.id.chip_before_sleep) {
                medication.setInstruction(EInstructions.BEFORE_SLEEP);
            } else if (checkedId == R.id.chip_does_not_matter) {
                medication.setInstruction(EInstructions.DOES_NOT_MATTER);
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
        String[] frequencies = new String[]{
                getString(R.string.frequency_once),
                getString(R.string.frequency_twice),
                getString(R.string.frequency_3_times),
                getString(R.string.frequency_4_times),
                getString(R.string.frequency_5_times)
        };
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencies);
        dropdownFrequency.setAdapter(freqAdapter);
        dropdownFrequency.setThreshold(Integer.MAX_VALUE); // Disable filtering

        // When Frequency changes, we must regenerate the specific time picker buttons
        dropdownFrequency.setOnItemClickListener((parent, itemView, position, itemId) -> {
            int timesPerDay = position + 1;
            medication.setDailyFrequency(timesPerDay);
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

        if (!medication.isValid()) {
            showInputErrorsOnViews(mainView);
            return;
        }

        final int frequency = medication.getDailyFrequency();
        final SparseArray<SimpleDayTime> activeTimes = new SparseArray<>();
        
        for (int i = 1; i <= frequency; i++) {
            SimpleDayTime time = dosesInDay.get(i);
            if (time == null) {
                showErrorDialog(getString(R.string.error_pick_time_title), getString(R.string.error_pick_time_message));
                return;
            }
            activeTimes.put(i, time);
        }

        // Logical validation: Doses must be at different times
        Set<SimpleDayTime> uniqueTimes = new HashSet<>();
        for (int i = 0; i < activeTimes.size(); i++) {
            if (!uniqueTimes.add(activeTimes.valueAt(i))) {
                showErrorDialog(getString(R.string.error_duplicate_times_title), getString(R.string.error_duplicate_times_message));
                return;
            }
        }

        medication.addTimeStampsForDay(activeTimes);
        medication.addToMedicationList(requireContext().getApplicationContext());

        dismiss();
        
        // Handle post-save logic (Ads and Permissions) after a delay to allow the database to commit
        // and the BottomSheet to finish its exit animation, ensuring a clean UI transition.
        // We wait 800ms to ensure the background fragment is fully settled.
        if (getActivity() instanceof com.robinzon.medicationwizard.MainActivity) {
            final com.robinzon.medicationwizard.MainActivity mainActivity = (com.robinzon.medicationwizard.MainActivity) getActivity();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (mainActivity.isFinishing() || mainActivity.isDestroyed()) return;
                
                // 1. Trigger interstitial ad (if eligible)
                mainActivity.getAdsManager().showInterstitialAd();
                
                // 2. Request notification permissions (Android 13+) 
                // We do this after the ad trigger to avoid overlapping system dialogs.
                com.robinzon.medicationwizard.notifications.NotificationManager.getInstance(mainActivity).requestWithRationale();
            }, 800);
        }
    }

    /**
     * Highlights all invalid fields with a shake animation and error text.
     */
    private void showInputErrorsOnViews(@NonNull View mainView) {
        if (TextUtils.isEmpty(medication.getCommercialName())) {
            highLightInValidFields(getCommercialNameInputEditText(mainView), true);
        }
        if (medication.getAmount() == 0) {
            highLightInValidFields(getAmountInputEditText(mainView), true);
        }
        if (medication.getDailyFrequency() == 0) {
            highLightInValidFields(getFrequencyInputEditText(mainView), true);
        }
        if (medication.getForm() == null) {
            highLightInValidFields(getFormInputEditText(mainView), true);
        }
    }

    /**
     * Shows a robust Material Design 3 error dialog.
     */
    private void showErrorDialog(@NonNull String title, @NonNull String message) {
        Context context = getContext();
        if (context == null) return;

        com.robinzon.medicationwizard.ui.CustomMaterialDialog errorDialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(context);
        errorDialog.setTitle(title);
        errorDialog.setMessage(message);
        errorDialog.setPositiveButton(getString(android.R.string.ok), (dialog, buttonIndex) -> dialog.dismiss());
        errorDialog.show();
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
                layout.setError(getString(R.string.error_required));
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

        String[] forms = new String[]{
                getString(R.string.form_pill),
                getString(R.string.form_solution),
                getString(R.string.form_injection),
                getString(R.string.form_powder),
                getString(R.string.form_drops),
                getString(R.string.form_inhaler),
                getString(R.string.form_other)
        };

        // Use a standard layout and disable filtering to prevent hint/text clearance issues
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_dropdown_item_1line, forms);
        dropdownForm.setAdapter(adapter);
        dropdownForm.setThreshold(Integer.MAX_VALUE); // Disable filtering by typing
        
        // Ensure hint is visible initially if no value
        if (TextUtils.isEmpty(dropdownForm.getText())) {
            dropdownForm.setText(null);
        }

        final TextInputLayout layoutForm = view.findViewById(R.id.layout_med_form);
        if (null == layoutForm) return;

        dropdownForm.setOnItemClickListener((parent, itemView, position, itemId) -> {
            final EForm selectedForm = EForm.values()[position];
            medication.setForm(selectedForm);
            int icon = switch (selectedForm) {
                case Pill -> R.drawable.ic_med_pill;
                case Drops -> R.drawable.ic_med_drops;
                case Injection -> R.drawable.ic_med_injection;
                case Solution -> R.drawable.ic_med_solution;
                case Inhaler -> R.drawable.ic_med_inhaler;
                case Powder -> R.drawable.ic_med_powder;
                case Other -> R.drawable.ic_med_other;
            };
            layoutForm.setStartIconDrawable(icon);
            
            // Apply theme-aware tint instead of hardcoded white
            int onSurfaceAttr = com.google.android.material.R.attr.colorOnSurface;
            int iconColor = com.google.android.material.color.MaterialColors.getColor(requireContext(), onSurfaceAttr, Color.BLACK);
            layoutForm.setStartIconTintList(ColorStateList.valueOf(iconColor));
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
            dropdownUnit.setOnItemClickListener((parent, itemView, position, itemId) -> {
                medication.setMeasurementUnit(EMeasurementUnit.values()[position]);
            });
        }
    }

    /**
     * Generates a list of buttons in the UI, one for each scheduled dose 
     * defined by the 'Frequency' dropdown.
     */
    private void generateTimePickers(final int amount) {
        // Clear all previous views immediately to avoid ghost buttons
        timesContainer.removeAllViews();
        
        final Context context = requireContext();
        final float density = context.getResources().getDisplayMetrics().density;
        final int margin = (int) (8 * density);

        // We use a post() to ensure we have the container width for row-fitting logic
        timesContainer.post(() -> {
            if (getView() == null) return;
            
            // Double check clear inside post to handle rapid frequency changes
            timesContainer.removeAllViews();

            int containerWidth = timesContainer.getWidth();
            if (containerWidth <= 0) {
                containerWidth = (int) (context.getResources().getDisplayMetrics().widthPixels - (32 * density));
            }

            LinearLayout currentRow = createNewRow(context);
            timesContainer.addView(currentRow);
            int currentLineWidth = 0;

            for (int i = 1; i <= amount; i++) {
                MaterialButton timeButton = (MaterialButton) LayoutInflater.from(context)
                        .inflate(R.layout.item_time_picker, currentRow, false);

                // Preserve existing time if available
                SimpleDayTime existingTime = dosesInDay.get(i);
                if (existingTime != null) {
                    timeButton.setText(getString(R.string.time_set_format, existingTime.toString()));
                } else {
                    timeButton.setText(getString(R.string.select_time_index_format, i));
                }

                int finalIndex = i;
                timeButton.setOnClickListener(v -> showTimePicker(finalIndex, timeButton));

                // Measure button with constraints to get a realistic width
                int maxWidthSpec = View.MeasureSpec.makeMeasureSpec(containerWidth, View.MeasureSpec.AT_MOST);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                timeButton.measure(maxWidthSpec, heightSpec);
                int btnWidth = timeButton.getMeasuredWidth();

                // If adding this button exceeds width, start a new row
                if (currentLineWidth + btnWidth + margin > containerWidth && currentLineWidth > 0) {
                    currentRow = createNewRow(context);
                    timesContainer.addView(currentRow);
                    currentLineWidth = 0;
                }

                // Add margin to the button
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) timeButton.getLayoutParams();
                layoutParams.setMargins(0, 0, margin, margin);
                timeButton.setLayoutParams(layoutParams);

                currentRow.addView(timeButton);
                currentLineWidth += btnWidth + margin;
            }
        });
    }

    private LinearLayout createNewRow(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    /**
     * Scrapes all manual text entry fields and populates the medication object.
     */
    private void populateMedication(@NonNull View view) {
        final TextInputEditText nameInputEditText = getCommercialNameInputEditText(view);
        if (nameInputEditText != null && nameInputEditText.getText() != null) {
            medication.setCommercialName(nameInputEditText.getText().toString());
        }

        final TextInputEditText amountTextView = getAmountInputEditText(view);
        if (amountTextView != null && amountTextView.getText() != null) {
            try {
                medication.setAmount(Float.parseFloat(amountTextView.getText().toString()));
            } catch (NumberFormatException ignored) {}
        }

        final TextInputEditText strengthEditText = view.findViewById(R.id.medication_strength);
        if (strengthEditText != null && strengthEditText.getText() != null) {
            try {
                medication.setStrength(Float.parseFloat(strengthEditText.getText().toString()));
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
                .setTitleText(R.string.time_picker_med_title)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute());
            buttonToUpdate.setText(getString(R.string.time_set_format, formattedTime));
            dosesInDay.put(finalIndex, new SimpleDayTime((byte) picker.getHour(), (byte) picker.getMinute()));
        });

        picker.show(getParentFragmentManager(), "timePicker");
    }

    @Nullable private TextInputEditText getCommercialNameInputEditText(@NonNull View view) { return view.findViewById(R.id.med_name_fragment_add_med); }
    @Nullable private TextInputEditText getAmountInputEditText(@NonNull View view) { return view.findViewById(R.id.med_amount); }
    @Nullable private AutoCompleteTextView getFrequencyInputEditText(@NonNull View view) { return view.findViewById(R.id.med_frequency_fragment_add_med); }
    @Nullable private AutoCompleteTextView getFormInputEditText(@NonNull View view) { return view.findViewById(R.id.med_form_fragment_add_med); }
    @Nullable private TextView getTitleTextView(View mainView) { return mainView.findViewById(R.id.title_fragment_add_med); }
}