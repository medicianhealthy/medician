package com.robinzon.medicationwizard.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.api.NLMClient;
import com.robinzon.medicationwizard.api.models.RxNormSpellingResponse;
import com.robinzon.medicationwizard.entities.EForm;
import com.robinzon.medicationwizard.entities.EInstructions;
import com.robinzon.medicationwizard.entities.EMeasurementUnit;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.ui.settings.FeatureRationalBottomSheet;
import com.robinzon.medicationwizard.utils.SimpleDayTime;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A highly interactive Material 3 BottomSheet for adding or editing medications.
 */
public class AddMedicationBottomSheet extends MedicationWizardBottomSheet {

    private final SparseArray<SimpleDayTime> dosesInDay = new SparseArray<>();
    private ConstraintLayout timesContainer;
    private Medication medication = new Medication();
    private boolean hasAttemptedSave;
    private boolean isEditMode = false;

    // Search & Autocomplete
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;
    private boolean isSelectionInProgress = false;

    // Photo related
    private Uri tempCameraUri;
    private ConstraintLayout photoContainer;
    private ShapeableImageView imgPreview;
    private View layoutPlaceholder;
    private View btnRotateLeft, btnRotateRight, btnRemovePhoto;
    private com.google.android.material.materialswitch.MaterialSwitch switchCritical;
    private View crownCritical, badgeCritical;
    private Runnable pendingPhotoAction;

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    if (pendingPhotoAction != null) {
                        pendingPhotoAction.run();
                        pendingPhotoAction = null;
                    }
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_LONG).show();
                }
            }
    );

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && tempCameraUri != null) {
                    processAndSetImage(tempCameraUri);
                }
            }
    );

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processAndSetImage(uri);
                }
            }
    );

    public static AddMedicationBottomSheet newInstance(@Nullable Medication medication) {
        AddMedicationBottomSheet fragment = new AddMedicationBottomSheet();
        if (medication != null) {
            Bundle args = new Bundle();
            args.putString("medication_json", medication.toJson().toString());
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog);
    }

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_medication, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("medication_state")) {
                try {
                    medication = Medication.fromJson(new JSONObject(savedInstanceState.getString("medication_state")));
                    if (medication.getTimesADay() != null) {
                        dosesInDay.clear();
                        for (int i = 0; i < medication.getTimesADay().size(); i++) {
                            int key = medication.getTimesADay().keyAt(i);
                            dosesInDay.put(key, medication.getTimesADay().valueAt(i));
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            if (savedInstanceState.containsKey("temp_camera_uri")) {
                tempCameraUri = savedInstanceState.getParcelable("temp_camera_uri");
            }
            isEditMode = savedInstanceState.getBoolean("is_edit_mode", false);
        } else if (getArguments() != null && getArguments().containsKey("medication_json")) {
            try {
                String json = getArguments().getString("medication_json");
                if (json != null) {
                    medication = Medication.fromJson(new JSONObject(json));
                    isEditMode = true;
                    if (medication.getTimesADay() != null) {
                        for (int i = 0; i < medication.getTimesADay().size(); i++) {
                            int key = medication.getTimesADay().keyAt(i);
                            dosesInDay.put(key, medication.getTimesADay().valueAt(i));
                        }
                    }
                }
            } catch (JSONException ignored) {
            }
        }

        setupDropdowns(view);
        TextInputLayout layoutForm = view.findViewById(R.id.layout_med_form);
        if (layoutForm != null) layoutForm.setHint(R.string.hint_form);

        MaterialAutoCompleteTextView nameEdit = getCommercialNameInputEditText(view);
        if (nameEdit != null) {
            nameEdit.setOnItemClickListener((parent, v, position, id) -> {
                isSelectionInProgress = true;
                String selected = (String) parent.getItemAtPosition(position);
                nameEdit.setText(selected, false);
                hideKeyboard(nameEdit);
            });
        }

        setupInstructions(view);
        setTextChangeListeners(view);
        timesContainer = view.findViewById(R.id.times_container);
        photoContainer = view.findViewById(R.id.photo_inner_container);
        imgPreview = view.findViewById(R.id.img_med_photo_preview);
        layoutPlaceholder = view.findViewById(R.id.layout_photo_placeholder);
        btnRotateLeft = view.findViewById(R.id.btn_rotate_left);
        btnRotateRight = view.findViewById(R.id.btn_rotate_right);
        btnRemovePhoto = view.findViewById(R.id.btn_remove_photo);
        switchCritical = view.findViewById(R.id.switch_critical);
        crownCritical = view.findViewById(R.id.crown_critical);
        badgeCritical = view.findViewById(R.id.badge_active_critical);

        setupPhotoButtons(view);
        setupCriticalToggle(view);

        if (isEditMode) {
            preFillData(view);
        }

        updatePhotoUi(false);

        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            trySaveMedication(view);
            hasAttemptedSave = true;
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (getView() != null) {
            populateMedication(getView());
        }
        outState.putString("medication_state", medication.toJson().toString());
        if (tempCameraUri != null) {
            outState.putParcelable("temp_camera_uri", tempCameraUri);
        }
        outState.putBoolean("is_edit_mode", isEditMode);
    }

    private void setupCriticalToggle(View view) {
        updateCriticalEntitlement();

        view.findViewById(R.id.btn_critical_toggle).setOnClickListener(v -> {
            if (AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.CRITICAL)) {
                boolean current = switchCritical.isChecked();
                switchCritical.setChecked(!current);
                medication.setCritical(!current);
            } else {
                FeatureRationalBottomSheet.newInstance(AppConfig.FeaturePassType.CRITICAL).show(getChildFragmentManager(), "CriticalRational");
                getChildFragmentManager().setFragmentResultListener("feature_unlocked", getViewLifecycleOwner(), (key, bundle) -> {
                    if (AppConfig.FeaturePassType.CRITICAL.name().equals(bundle.getString("feature_type"))) {
                        updateCriticalEntitlement();
                        switchCritical.setChecked(true);
                        medication.setCritical(true);
                    }
                });
            }
        });
    }

    private void updateCriticalEntitlement() {
        boolean purchased = AppConfig.isPremiumPurchased(requireContext());
        boolean unlocked = AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.CRITICAL);

        if (crownCritical != null) crownCritical.setVisibility(purchased ? View.GONE : View.VISIBLE);
        if (badgeCritical != null) {
            boolean showBadge = !purchased && unlocked;
            badgeCritical.setVisibility(showBadge ? View.VISIBLE : View.GONE);
            if (showBadge && badgeCritical instanceof TextView) {
                ((TextView) badgeCritical).setText(R.string.active_for_next_reminder);
            }
        }
        
        if (switchCritical != null) {
            switchCritical.setChecked(medication.isCritical());
        }
    }

    private void setupPhotoButtons(View view) {
        view.findViewById(R.id.btn_camera).setOnClickListener(v -> checkSystemPermissionAndAct(Manifest.permission.CAMERA, () -> {
            checkFeaturePassAndAct(() -> {
                File tempFile = new File(requireContext().getCacheDir(), "temp_camera_" + System.currentTimeMillis() + ".jpg");
                tempCameraUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", tempFile);
                cameraLauncher.launch(tempCameraUri);
            });
        }));

        view.findViewById(R.id.btn_gallery).setOnClickListener(v -> checkFeaturePassAndAct(() -> {
            galleryLauncher.launch("image/*");
        }));

        if (btnRotateLeft != null) {
            btnRotateLeft.setOnClickListener(v -> checkFeaturePassAndAct(() -> rotateImage(-90)));
        }
        if (btnRotateRight != null) {
            btnRotateRight.setOnClickListener(v -> checkFeaturePassAndAct(() -> rotateImage(90)));
        }
        if (btnRemovePhoto != null) {
            btnRemovePhoto.setOnClickListener(v -> removePhoto());
        }
    }

    private void checkSystemPermissionAndAct(String permission, Runnable action) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            action.run();
        } else {
            pendingPhotoAction = action;
            permissionLauncher.launch(permission);
        }
    }

    private void checkFeaturePassAndAct(Runnable action) {
        if (AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.PHOTO)) {
            action.run();
        } else {
            FeatureRationalBottomSheet.newInstance(AppConfig.FeaturePassType.PHOTO).show(getChildFragmentManager(), "PhotoRational");
            getChildFragmentManager().setFragmentResultListener("feature_unlocked", getViewLifecycleOwner(), (key, bundle) -> {
                String type = bundle.getString("feature_type");
                if (AppConfig.FeaturePassType.PHOTO.name().equals(type)) {
                    action.run();
                }
            });
        }
    }

    private void processAndSetImage(Uri uri) {
        String savedPath = saveImageLocally(uri);
        if (savedPath != null) {
            medication.setImagePath(savedPath);
            updatePhotoUi(true);
        }
    }

    private void updatePhotoUi(boolean animate) {
        if (imgPreview == null || layoutPlaceholder == null) return;

        if (medication.getImagePath() != null) {
            imgPreview.setVisibility(View.VISIBLE);
            layoutPlaceholder.setVisibility(View.GONE);
            if (btnRotateLeft != null) btnRotateLeft.setVisibility(View.VISIBLE);
            if (btnRotateRight != null) btnRotateRight.setVisibility(View.VISIBLE);
            if (btnRemovePhoto != null) btnRemovePhoto.setVisibility(View.VISIBLE);

            File file = new File(medication.getImagePath());

            // Detect orientation for correct ratio
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            updatePreviewRatio(options.outHeight > options.outWidth, animate);

            Glide.with(this)
                    .load(file)
                    .signature(new ObjectKey(file.lastModified()))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(imgPreview);
        } else {
            imgPreview.setVisibility(View.GONE);
            layoutPlaceholder.setVisibility(View.VISIBLE);
            if (btnRotateLeft != null) btnRotateLeft.setVisibility(View.GONE);
            if (btnRotateRight != null) btnRotateRight.setVisibility(View.GONE);
            if (btnRemovePhoto != null) btnRemovePhoto.setVisibility(View.GONE);
        }
    }

    private void removePhoto() {
        if (medication.getImagePath() != null) {
            File file = new File(medication.getImagePath());
            if (file.exists()) {
                file.delete();
            }
            medication.setImagePath(null);
            updatePhotoUi(true);
        }
    }

    private void updatePreviewRatio(boolean isPortrait, boolean animate) {
        if (photoContainer == null || imgPreview == null) return;

        if (animate) {
            TransitionManager.beginDelayedTransition(photoContainer);
        }

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(photoContainer);
        // Force wrap_content for height when in portrait to allow the container to grow, 
        // but keep the ratio constraint to define the box shape.
        constraintSet.setDimensionRatio(imgPreview.getId(), isPortrait ? "H,9:16" : "H,16:9");
        constraintSet.applyTo(photoContainer);
    }

    private void rotateImage(int degrees) {
        if (medication.getImagePath() == null || imgPreview == null) return;

        // 1. Visual Animation
        imgPreview.animate()
                .rotationBy(degrees)
                .setDuration(300)
                .withEndAction(() -> {
                    try {
                        // 2. Physical Rotation
                        File file = new File(medication.getImagePath());
                        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        if (bitmap == null) return;

                        Matrix matrix = new Matrix();
                        matrix.postRotate(degrees);
                        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                        FileOutputStream out = new FileOutputStream(file);
                        rotated.compress(Bitmap.CompressFormat.JPEG, 85, out);
                        out.close();

                        // 3. Reset View Properties & Reload
                        imgPreview.setRotation(0); // Reset rotation property because image itself is now rotated
                        updatePhotoUi(true); // This will also handle the ratio update

                        bitmap.recycle();
                        rotated.recycle();
                    } catch (IOException e) {
                        Toast.makeText(requireContext(), "Error rotating image", Toast.LENGTH_SHORT).show();
                    }
                })
                .start();
    }

    private String saveImageLocally(Uri sourceUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) return null;
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            if (original == null) return null;

            int width = original.getWidth();
            int height = original.getHeight();
            float ratio = (float) width / (float) height;
            if (width > 1280 || height > 1280) {
                if (width > height) {
                    width = 1280;
                    height = (int) (width / ratio);
                } else {
                    height = 1280;
                    width = (int) (height * ratio);
                }
            }
            Bitmap resized = Bitmap.createScaledBitmap(original, width, height, true);
            File dir = new File(requireContext().getFilesDir(), "med_photos");
            if (!dir.exists()) dir.mkdirs();
            String fileName = "med_" + UUID.randomUUID().toString() + ".jpg";
            File file = new File(dir, fileName);
            FileOutputStream out = new FileOutputStream(file);
            resized.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private void preFillData(View view) {
        TextView titleView = getTitleTextView(view);
        if (titleView != null) titleView.setText(medication.getCommercialName());
        MaterialAutoCompleteTextView nameEdit = getCommercialNameInputEditText(view);
        if (nameEdit != null) nameEdit.setText(medication.getCommercialName());
        TextInputEditText amountEdit = getAmountInputEditText(view);
        if (amountEdit != null) {
            String val = medication.getAmount() == (long) medication.getAmount() ?
                    String.valueOf((long) medication.getAmount()) : String.valueOf(medication.getAmount());
            amountEdit.setText(val);
        }
        AutoCompleteTextView formEdit = getFormInputEditText(view);
        if (formEdit != null && medication.getForm() != null) {
            formEdit.setText(getString(medication.getForm().getLabelResId()), false);
            final TextInputLayout layoutForm = view.findViewById(R.id.layout_med_form);
            if (layoutForm != null) {
                int icon = switch (medication.getForm()) {
                    case Pill -> R.drawable.ic_med_pill;
                    case Drops -> R.drawable.ic_med_drops;
                    case Injection -> R.drawable.ic_med_injection;
                    case Solution -> R.drawable.ic_med_solution;
                    case Inhaler -> R.drawable.ic_med_inhaler;
                    case Powder -> R.drawable.ic_med_powder;
                    case Other -> R.drawable.ic_med_other;
                };
                layoutForm.setStartIconDrawable(icon);
                int onSurfaceAttr = com.google.android.material.R.attr.colorOnSurface;
                int iconColor = com.google.android.material.color.MaterialColors.getColor(requireContext(), onSurfaceAttr, Color.BLACK);
                layoutForm.setStartIconTintList(ColorStateList.valueOf(iconColor));
            }
        }

        AutoCompleteTextView freqEdit = getFrequencyInputEditText(view);
        if (freqEdit != null && medication.getDailyFrequency() >= 0) {
            String[] frequencies = new String[]{
                    getString(R.string.frequency_as_needed),
                    getString(R.string.frequency_once),
                    getString(R.string.frequency_twice),
                    getString(R.string.frequency_3_times),
                    getString(R.string.frequency_4_times),
                    getString(R.string.frequency_5_times)
            };
            if (medication.getDailyFrequency() < frequencies.length) {
                freqEdit.setText(frequencies[medication.getDailyFrequency()], false);
            }
            if (medication.getDailyFrequency() > 0) {
                generateTimePickers(medication.getDailyFrequency());
                if (medication.getTimesADay() != null) {
                    for (int k = 0; k < medication.getTimesADay().size(); k++) {
                        int key = medication.getTimesADay().keyAt(k);
                        SimpleDayTime time = medication.getTimesADay().valueAt(k);
                        dosesInDay.put(key, time);
                        if (k < timesContainer.getChildCount()) {
                            View child = timesContainer.getChildAt(k);
                            if (child instanceof Button) {
                                ((Button) child).setText(getString(R.string.time_set_format, time.toString()));
                            }
                        }
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
            unitEdit.setText(medication.getMeasurementUnit().getLabel(requireContext()), false);
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
        if (switchCritical != null) {
            switchCritical.setChecked(medication.isCritical());
        }
        updatePhotoUi(false);
    }

    private void setTextChangeListeners(@NonNull final View mainView) {
        final MaterialAutoCompleteTextView commercialNameInputEditText = getCommercialNameInputEditText(mainView);
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
                final String trimmedInput = editable.toString().trim();
                if (hasAttemptedSave) {
                    final int editTextId = editText.getId();
                    if (editTextId == R.id.med_name_fragment_add_med) {
                        highLightInValidFields(editText, trimmedInput.isEmpty(), false);
                    } else if (editTextId == R.id.med_amount) {
                        float amountVal = 0;
                        try {
                            amountVal = Float.parseFloat(trimmedInput);
                        } catch (Exception ignored) {
                        }
                        highLightInValidFields(editText, amountVal <= 0, false);
                    } else if (editTextId == R.id.med_form_fragment_add_med || editTextId == R.id.med_frequency_fragment_add_med) {
                        highLightInValidFields(editText, trimmedInput.isEmpty(), false);
                    }
                }
                TextView titleView = getTitleTextView(mainView);
                MaterialAutoCompleteTextView nameEdit = getCommercialNameInputEditText(mainView);
                if (titleView != null && nameEdit != null && editText.getId() == R.id.med_name_fragment_add_med) {
                    if (trimmedInput.isEmpty()) {
                        titleView.setText(R.string.fragment_add_med_title);
                    } else {
                        titleView.setText(trimmedInput);
                    }

                    if (isSelectionInProgress) {
                        searchHandler.removeCallbacksAndMessages(null);
                        isSelectionInProgress = false;
                        return;
                    }

                    // Trigger Search with Debounce & Length check
                    searchHandler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> performMedicationSearch(trimmedInput, nameEdit);
                    if (trimmedInput.length() >= 3) {
                        searchHandler.postDelayed(searchRunnable, 300);
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
    }

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

    private void setupDropdowns(View view) {
        setFormDropDown(view);
        setUnitDropDown(view);
        AutoCompleteTextView dropdownFrequency = view.findViewById(R.id.med_frequency_fragment_add_med);
        String[] frequencies = new String[]{
                getString(R.string.frequency_as_needed),
                getString(R.string.frequency_once),
                getString(R.string.frequency_twice),
                getString(R.string.frequency_3_times),
                getString(R.string.frequency_4_times),
                getString(R.string.frequency_5_times)
        };
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown_menu, frequencies);
        dropdownFrequency.setAdapter(freqAdapter);
        dropdownFrequency.setThreshold(Integer.MAX_VALUE);
        dropdownFrequency.setOnClickListener(v -> dropdownFrequency.showDropDown());
        dropdownFrequency.setOnItemClickListener((parent, itemView, position, itemId) -> {
            hideKeyboard(dropdownFrequency);
            int timesPerDay = position; // 0 = As Needed, 1 = Once, etc.
            medication.setDailyFrequency(timesPerDay);
            
            if (timesPerDay == 0) {
                timesContainer.removeAllViews();
                dosesInDay.clear();
            } else {
                generateTimePickers(timesPerDay);
            }
            
            timesContainer.postDelayed(() -> {
                if (timesContainer.getChildCount() > 0) {
                    View row = timesContainer.getChildAt(0);
                    if (row instanceof MaterialButton) {
                        row.requestFocus();
                    }
                }
            }, 100);
        });
    }

    private void hideKeyboard(View view) {
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void trySaveMedication(@NonNull View mainView) {
        populateMedication(mainView);
        if (!medication.isValid()) {
            showInputErrorsOnViews(mainView);
            return;
        }
        final int frequency = medication.getDailyFrequency();
        final SparseArray<SimpleDayTime> activeTimes = new SparseArray<>();

        if (frequency > 0) {
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
        }

        medication.addTimeStampsForDay(activeTimes);
        medication.addToMedicationList(requireContext().getApplicationContext());

        // Notify potential listeners (like LogDoseBottomSheet) that a new med was added
        Bundle result = new Bundle();
        result.putString("medication_id", medication.getId());
        getParentFragmentManager().setFragmentResult("medication_added", result);

        dismiss();
        if (getActivity() instanceof com.robinzon.medicationwizard.MainActivity) {
            final com.robinzon.medicationwizard.MainActivity mainActivity = (com.robinzon.medicationwizard.MainActivity) getActivity();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (mainActivity.isFinishing() || mainActivity.isDestroyed()) return;
                mainActivity.getAdsManager().showInterstitialAd();
                com.robinzon.medicationwizard.notifications.NotificationManager.getInstance(mainActivity).requestWithRationale();
            }, 1200);
        }
    }

    private void showInputErrorsOnViews(@NonNull View mainView) {
        if (TextUtils.isEmpty(medication.getCommercialName())) {
            highLightInValidFields(getCommercialNameInputEditText(mainView), true);
        }
        if (medication.getAmount() == 0) {
            highLightInValidFields(getAmountInputEditText(mainView), true);
        }
        AutoCompleteTextView freqView = getFrequencyInputEditText(mainView);
        if (freqView != null && TextUtils.isEmpty(freqView.getText())) {
            highLightInValidFields(freqView, true);
        }
        if (medication.getForm() == null) {
            highLightInValidFields(getFormInputEditText(mainView), true);
        }
    }

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

    private void setFormDropDown(View view) {
        final AutoCompleteTextView dropdownForm = view.findViewById(R.id.med_form_fragment_add_med);
        if (null == dropdownForm) return;
        dropdownForm.setOnTouchListener((v, event) -> {
            hideKeyboard(v);
            return false;
        });
        String[] forms = new String[]{
                getString(R.string.form_pill),
                getString(R.string.form_solution),
                getString(R.string.form_injection),
                getString(R.string.form_powder),
                getString(R.string.form_drops),
                getString(R.string.form_inhaler),
                getString(R.string.form_other)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown_menu, forms);
        dropdownForm.setAdapter(adapter);
        dropdownForm.setThreshold(Integer.MAX_VALUE);
        if (TextUtils.isEmpty(dropdownForm.getText())) {
            dropdownForm.setText(null);
        }
        final TextInputLayout layoutForm = view.findViewById(R.id.layout_med_form);
        if (null == layoutForm) return;
        dropdownForm.setOnClickListener(v -> dropdownForm.showDropDown());
        dropdownForm.setOnItemClickListener((parent, itemView, position, itemId) -> {
            hideKeyboard(dropdownForm);
            final String selectedString = (String) parent.getItemAtPosition(position);
            EForm selectedForm = EForm.Other;
            for (EForm f : EForm.values()) {
                if (getString(f.getLabelResId()).equals(selectedString)) {
                    selectedForm = f;
                    break;
                }
            }
            medication.setForm(selectedForm);
            View next = view.findViewById(R.id.med_frequency_fragment_add_med);
            if (next != null) next.requestFocus();
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
            int onSurfaceAttr = com.google.android.material.R.attr.colorOnSurface;
            int iconColor = com.google.android.material.color.MaterialColors.getColor(requireContext(), onSurfaceAttr, Color.BLACK);
            layoutForm.setStartIconTintList(ColorStateList.valueOf(iconColor));
        });
    }

    private void setUnitDropDown(View view) {
        final AutoCompleteTextView dropdownUnit = view.findViewById(R.id.dropdown_unit);
        final ArrayList<String> measurementUnits = new ArrayList<>();
        for (EMeasurementUnit unit : EMeasurementUnit.values()) {
            measurementUnits.add(unit.getLabel(requireContext()));
        }
        if (null != dropdownUnit) {
            dropdownUnit.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_dropdown_menu, measurementUnits));
            dropdownUnit.setOnClickListener(v -> dropdownUnit.showDropDown());
            dropdownUnit.setOnItemClickListener((parent, itemView, position, itemId) -> {
                hideKeyboard(dropdownUnit);
                medication.setMeasurementUnit(EMeasurementUnit.values()[position]);
                View next = view.findViewById(R.id.chip_group_instructions);
                if (next != null) next.requestFocus();
            });
        }
    }

    private void generateTimePickers(final int amount) {
        timesContainer.removeAllViews();
        final Context context = requireContext();
        final float density = context.getResources().getDisplayMetrics().density;
        final int gap = (int) (8 * density);
        Flow flow = new Flow(context);
        flow.setId(View.generateViewId());
        flow.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT));
        flow.setHorizontalGap(gap);
        flow.setVerticalGap(gap);
        flow.setWrapMode(Flow.WRAP_CHAIN);
        flow.setHorizontalStyle(Flow.CHAIN_PACKED);
        flow.setHorizontalBias(0f);
        int[] viewIds = new int[amount];
        for (int i = 1; i <= amount; i++) {
            MaterialButton timeButton = (MaterialButton) LayoutInflater.from(context)
                    .inflate(R.layout.item_time_picker, timesContainer, false);
            int viewId = View.generateViewId();
            timeButton.setId(viewId);
            viewIds[i - 1] = viewId;
            SimpleDayTime existingTime = dosesInDay.get(i);
            if (existingTime != null) {
                timeButton.setText(getString(R.string.time_set_format, existingTime.toString()));
            } else {
                timeButton.setText(getString(R.string.select_time_index_format, i));
            }
            int finalIndex = i;
            timeButton.setOnClickListener(v -> showTimePicker(finalIndex, timeButton));
            timesContainer.addView(timeButton);
        }
        flow.setReferencedIds(viewIds);
        timesContainer.addView(flow);
    }

    private void populateMedication(@NonNull View view) {
        final MaterialAutoCompleteTextView nameInputEditText = getCommercialNameInputEditText(view);
        if (nameInputEditText != null && nameInputEditText.getText() != null) {
            medication.setCommercialName(nameInputEditText.getText().toString());
        }
        final TextInputEditText amountTextView = getAmountInputEditText(view);
        if (amountTextView != null && amountTextView.getText() != null) {
            String amountStr = amountTextView.getText().toString().replace(',', '.').trim();
            if (amountStr.isEmpty()) {
                medication.setAmount(0);
            } else {
                try {
                    medication.setAmount(Float.parseFloat(amountStr));
                } catch (NumberFormatException ignored) {
                    medication.setAmount(0);
                }
            }
        }

        // Robust mapping for Form from UI text
        final AutoCompleteTextView formDropdown = getFormInputEditText(view);
        if (formDropdown != null && !TextUtils.isEmpty(formDropdown.getText())) {
            String currentFormText = formDropdown.getText().toString().trim();
            for (EForm f : EForm.values()) {
                if (getString(f.getLabelResId()).trim().equalsIgnoreCase(currentFormText)) {
                    medication.setForm(f);
                    break;
                }
            }
        }

        // Robust mapping for Frequency from UI text
        final AutoCompleteTextView freqDropdown = getFrequencyInputEditText(view);
        if (freqDropdown != null && !TextUtils.isEmpty(freqDropdown.getText())) {
            String currentFreqText = freqDropdown.getText().toString().trim();
            String[] frequencies = new String[]{
                    getString(R.string.frequency_as_needed).trim(),
                    getString(R.string.frequency_once).trim(),
                    getString(R.string.frequency_twice).trim(),
                    getString(R.string.frequency_3_times).trim(),
                    getString(R.string.frequency_4_times).trim(),
                    getString(R.string.frequency_5_times).trim()
            };
            for (int i = 0; i < frequencies.length; i++) {
                if (frequencies[i].equalsIgnoreCase(currentFreqText)) {
                    medication.setDailyFrequency(i); // 0 = As Needed
                    break;
                }
            }
        }

        final TextInputEditText strengthEditText = view.findViewById(R.id.medication_strength);
        if (strengthEditText != null && strengthEditText.getText() != null) {
            String strengthStr = strengthEditText.getText().toString().replace(',', '.').trim();
            if (strengthStr.isEmpty()) {
                medication.setStrength(0);
            } else {
                try {
                    medication.setStrength(Float.parseFloat(strengthStr));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        final AutoCompleteTextView unitDropdown = view.findViewById(R.id.dropdown_unit);
        if (unitDropdown != null && unitDropdown.getText() != null) {
            String unitName = unitDropdown.getText().toString();
            for (EMeasurementUnit unit : EMeasurementUnit.values()) {
                if (unit.getLabel(requireContext()).equalsIgnoreCase(unitName)) {
                    medication.setMeasurementUnit(unit);
                    break;
                }
            }
        }

        // Sync dosesInDay back to medication object
        if (dosesInDay.size() > 0) {
            medication.addTimeStampsForDay(dosesInDay);
        }
        if (switchCritical != null) {
            medication.setCritical(switchCritical.isChecked());
        }
    }

    private void showTimePicker(final int finalIndex, final Button buttonToUpdate) {
        final MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(12)
                .setMinute(0)
                .setInputMode(com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTitleText(R.string.time_picker_med_title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute());
            buttonToUpdate.setText(getString(R.string.time_set_format, formattedTime));
            dosesInDay.put(finalIndex, new SimpleDayTime((byte) picker.getHour(), (byte) picker.getMinute()));
            buttonToUpdate.requestFocus();
            if (finalIndex == medication.getDailyFrequency()) {
                View nextView = getView() != null ? getView().findViewById(R.id.medication_strength) : null;
                if (nextView != null) {
                    nextView.postDelayed(nextView::requestFocus, 100);
                }
            }
        });
        picker.show(getParentFragmentManager(), "timePicker");
    }

    @Nullable
    private MaterialAutoCompleteTextView getCommercialNameInputEditText(@NonNull View view) {
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

    private void performMedicationSearch(String query, MaterialAutoCompleteTextView autoCompleteTextView) {
        if (query.length() < 3) return;

        NLMClient.getService().searchMedications(query).enqueue(new Callback<List<Object>>() {
            @Override
            public void onResponse(@NonNull Call<List<Object>> call, @NonNull Response<List<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().size() > 1) {
                    // RxTerms response: [total, [names], null, [details]]
                    List<String> names = (List<String>) response.body().get(1);
                    if (names != null && !names.isEmpty()) {
                        updateAutoCompleteAdapter(names, autoCompleteTextView);
                    } else {
                        // No results, try fuzzy matching (spelling suggestions)
                        performSpellingSearch(query, autoCompleteTextView);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Object>> call, @NonNull Throwable t) {
                // Graceful failure (Offline or server error)
            }
        });
    }

    private void performSpellingSearch(String query, MaterialAutoCompleteTextView autoCompleteTextView) {
        NLMClient.getService().getSpellingSuggestions(query).enqueue(new Callback<RxNormSpellingResponse>() {
            @Override
            public void onResponse(@NonNull Call<RxNormSpellingResponse> call, @NonNull Response<RxNormSpellingResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().suggestionGroup != null) {
                    RxNormSpellingResponse.SuggestionList suggestionList = response.body().suggestionGroup.suggestionList;
                    if (suggestionList != null && suggestionList.suggestions != null && !suggestionList.suggestions.isEmpty()) {
                        updateAutoCompleteAdapter(suggestionList.suggestions, autoCompleteTextView);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<RxNormSpellingResponse> call, @NonNull Throwable t) {
                // Graceful failure
            }
        });
    }

    private void updateAutoCompleteAdapter(List<String> suggestions, MaterialAutoCompleteTextView autoCompleteTextView) {
        if (!isAdded() || getContext() == null) return;

        List<String> formattedSuggestions = new ArrayList<>();
        for (String s : suggestions) {
            formattedSuggestions.add(toTitleCase(s));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown_menu, formattedSuggestions);
        autoCompleteTextView.setAdapter(adapter);
        // Only show if user is still focused and typing
        if (autoCompleteTextView.hasFocus()) {
            autoCompleteTextView.showDropDown();
        }
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        
        String clean = input.trim();
        if (clean.endsWith(",")) clean = clean.substring(0, clean.length() - 1);
        
        StringBuilder titleCase = new StringBuilder(clean.length());
        boolean nextTitleCase = true;

        for (char c : clean.toLowerCase().toCharArray()) {
            if (Character.isSpaceChar(c) || c == '-' || c == '/' || c == '(' || c == ')' || c == '[' || c == ']' || c == ',' || c == '.' || c == ':') {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toUpperCase(c);
                nextTitleCase = false;
            }
            titleCase.append(c);
        }

        return titleCase.toString();
    }
}
