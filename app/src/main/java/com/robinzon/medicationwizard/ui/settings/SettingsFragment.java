package com.robinzon.medicationwizard.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.databinding.FragmentSettingsBinding;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.entities.MedicationWizardFragment;
import com.robinzon.medicationwizard.notifications.NotificationManager;

/**
 * Fragment that provides the user interface for all application settings.
 * <p>
 * This screen follows the Material 3 design guidelines and manages:
 * - Application Theme (Light, Dark, System).
 * - Notification Permissions and Alert Details.
 * - Custom Reminder Sounds and Volume Control.
 * - Quiet Hours scheduling to suppress alerts at night.
 * - Data Management (Wiping medications and history).
 * - System information like version numbers.
 * </p>
 */
public class SettingsFragment extends MedicationWizardFragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;

    /**
     * Initializes data binding and the {@link SettingsViewModel}.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Entry point for UI configuration. 
     * Hides the Main FAB to prevent UI clutter and calls the comprehensive 
     * {@link #setupSettings()} method.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFabVisible(false);
        }
        // Apply dynamic padding to clear navigation and ad banners
        setPaddingForRecyclerView(binding.fragmentSettingsMainView, false);

        setupSettings();
    }

    /**
     * The main orchestrator for settings initialization.
     * <p>
     * Implementation details:
     * - Observes theme changes and applies {@link androidx.appcompat.app.AppCompatDelegate} logic.
     * - Configures notification permission flow including high-level explanation snacks.
     * - Manages custom sound selection via {@link SoundPickerBottomSheet}.
     * - Handles quiet hours range selection via sequential {@link MaterialTimePicker} dialogs.
     * - Provides a destructive "Clear Data" flow with an explicit confirmation dialog.
     * </p>
     */
    private void setupSettings() {
        // 1. Version Info
        binding.txtVersion.setText(getString(R.string.settings_version_summary, BuildConfig.VERSION_NAME));

        // 2. Notifications Logic
        updateNotificationStatus();
        binding.btnNotifications.setOnClickListener(v -> {
            boolean isGranted = NotificationManager.getInstance(requireActivity()).hasPermission();
            if (!isGranted) {
                NotificationManager.getInstance(requireActivity()).requestPermissionIfNeeded();
            } else {
                Snackbar.make(binding.getRoot(), "Notifications are already active! To disable, please use system settings.", Snackbar.LENGTH_LONG).show();
                binding.switchNotifications.setChecked(true);
            }
        });

        // 2b. Sound Setting
        viewModel.getSoundName().observe(getViewLifecycleOwner(), name -> 
            binding.txtSoundDesc.setText(getString(R.string.settings_sound_summary, name)));
        
        binding.btnNotifSound.setOnClickListener(v -> {
            SoundPickerBottomSheet picker = new SoundPickerBottomSheet();
            picker.setCurrentSoundUri(viewModel.getSoundUri().getValue());
            picker.setOnSoundSelectedListener((name, uri) -> viewModel.setSound(name, uri));
            picker.show(getChildFragmentManager(), "SoundPicker");
        });

        // 2c. Bypass Volume Logic
        viewModel.getBypassVolume().observe(getViewLifecycleOwner(), bypass -> {
            binding.switchBypass.setChecked(bypass);
            binding.layoutVolume.setVisibility(bypass ? View.VISIBLE : View.GONE);
        });
        
        binding.btnBypassVolume.setOnClickListener(v -> 
            viewModel.setBypassVolume(!Boolean.TRUE.equals(viewModel.getBypassVolume().getValue())));

        // 2d. Volume Slider Configuration
        viewModel.getNotifVolume().observe(getViewLifecycleOwner(), volume -> 
            binding.sliderVolume.setValue(volume));
        
        binding.sliderVolume.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) viewModel.setNotifVolume((int) value);
        });

        // 3. Theme Setting (Segmented Toggle Group)
        viewModel.getTheme().observe(getViewLifecycleOwner(), theme -> {
            int buttonId;
            if (theme == SettingsViewModel.THEME_LIGHT) buttonId = R.id.btn_theme_light;
            else if (theme == SettingsViewModel.THEME_DARK) buttonId = R.id.btn_theme_dark;
            else buttonId = R.id.btn_theme_system;
            
            binding.toggleGroupTheme.check(buttonId);
        });

        binding.toggleGroupTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            
            int theme;
            if (checkedId == R.id.btn_theme_light) theme = SettingsViewModel.THEME_LIGHT;
            else if (checkedId == R.id.btn_theme_dark) theme = SettingsViewModel.THEME_DARK;
            else theme = SettingsViewModel.THEME_SYSTEM;
            
            viewModel.setTheme(theme);
        });

        // 4. Placeholder actions for future features
        binding.btnBackup.setOnClickListener(v -> {
            Snackbar.make(binding.getRoot(), "Backup feature is being prepared for you!", Snackbar.LENGTH_SHORT).show();
        });

        // 5. Destructive Action: Clear Data
        binding.btnClearData.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Wipe everything?")
                    .setMessage("This will delete all your medications and history. We can't get them back once they're gone!")
                    .setPositiveButton("Yes, start fresh", (dialog, which) -> {
                        Medication.clearAllMedications(requireContext());
                        Snackbar.make(binding.getRoot(), "All data cleared. A fresh start!", Snackbar.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No, keep them", null)
                    .show();
        });

        // 6. Placeholder for support
        binding.btnSupport.setOnClickListener(v -> {
            Snackbar.make(binding.getRoot(), "Support portal is coming soon!", Snackbar.LENGTH_SHORT).show();
        });
        
        // 7. Quiet Hours Scheduling
        viewModel.getQuietHoursRange().observe(getViewLifecycleOwner(), range -> {
            binding.txtQuietHoursDesc.setText(getString(R.string.settings_quiet_hours_format, range));
        });

        binding.btnQuietHours.setOnClickListener(v -> {
            // Sequence of two time pickers to define a range
            MaterialTimePicker startPicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(23)
                    .setMinute(0)
                    .setTitleText("When should quiet hours start?")
                    .build();

            startPicker.addOnPositiveButtonClickListener(v1 -> {
                MaterialTimePicker endPicker = new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(7)
                        .setMinute(0)
                        .setTitleText("When should quiet hours end?")
                        .build();

                endPicker.addOnPositiveButtonClickListener(v2 -> {
                    viewModel.setQuietHours(startPicker.getHour(), startPicker.getMinute(), endPicker.getHour(), endPicker.getMinute());
                    Snackbar.make(binding.getRoot(), "Quiet hours updated!", Snackbar.LENGTH_SHORT).show();
                });
                endPicker.show(getChildFragmentManager(), "end_picker");
            });
            startPicker.show(getChildFragmentManager(), "start_picker");
        });
    }

    /**
     * Checks current system permission status and adjusts the settings UI accordingly.
     */
    private void updateNotificationStatus() {
        boolean isGranted = NotificationManager.getInstance(requireActivity()).hasPermission();
        binding.switchNotifications.setChecked(isGranted);
        binding.containerAlertDetails.setVisibility(isGranted ? View.VISIBLE : View.GONE);
    }

    /**
     * Ensures permission status is accurate if the user returns from system settings.
     */
    @Override
    public void onResume() {
        super.onResume();
        updateNotificationStatus();
    }

    /**
     * Standard binding cleanup.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}