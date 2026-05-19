package com.robinzon.medicationwizard.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

public class SettingsFragment extends MedicationWizardFragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFabVisible(false);
        }
        setPaddingForRecyclerView(binding.fragmentSettingsMainView, false);

        setupSettings();
    }



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
                // If already granted, show feedback
                Snackbar.make(binding.getRoot(), "Notifications are already active!", Snackbar.LENGTH_SHORT).show();
            }
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

        // 4. Backup
        binding.btnBackup.setOnClickListener(v -> {
            Snackbar.make(binding.getRoot(), "Backup feature is being prepared for you!", Snackbar.LENGTH_SHORT).show();
        });

        // 5. Clear Data (Casual but careful)
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

        // 6. Support
        binding.btnSupport.setOnClickListener(v -> {
            Snackbar.make(binding.getRoot(), "Support portal is coming soon!", Snackbar.LENGTH_SHORT).show();
        });
        
        // 7. Quiet Hours
        viewModel.getQuietHoursRange().observe(getViewLifecycleOwner(), range -> {
            binding.txtQuietHoursDesc.setText(getString(R.string.settings_quiet_hours_format, range));
        });

        binding.btnQuietHours.setOnClickListener(v -> {
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

    private void updateNotificationStatus() {
        boolean isGranted = NotificationManager.getInstance(requireActivity()).hasPermission();
        binding.switchNotifications.setChecked(isGranted);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNotificationStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Restore FAB when leaving settings (if needed, but usually navigation handles this via onViewCreated of target)
        binding = null;
    }
}