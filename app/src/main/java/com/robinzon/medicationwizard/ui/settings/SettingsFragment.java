package com.robinzon.medicationwizard.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.backup.CloudBackupManager;
import com.robinzon.medicationwizard.backup.CloudBackupSettings;
import com.robinzon.medicationwizard.backup.DriveServiceHelper;
import com.robinzon.medicationwizard.backup.GoogleAccountManager;
import com.robinzon.medicationwizard.databinding.FragmentSettingsBinding;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.entities.MedicationWizardFragment;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.utils.BackupManager;
import com.robinzon.medicationwizard.utils.Logger;

import java.util.Collections;

/**
 * Fragment that provides the user interface for all application settings.
 */
public class SettingsFragment extends MedicationWizardFragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private GoogleSignInClient googleSignInClient;
    private boolean isThemeReverting = false;
    private int cheatTapCount = 0;

    private android.content.res.ColorStateList defaultCardBgColor;
    private int defaultCardStrokeWidth;
    private android.content.res.ColorStateList defaultCardStrokeColor;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                com.google.android.gms.tasks.Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                if (task.isSuccessful()) {
                    handleSignInSuccess(task.getResult());
                } else {
                    handleSignInError(task.getException());
                }
            }
    );

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    BackupManager.createBackup(requireContext(), uri, (success, msg) -> 
                        requireActivity().runOnUiThread(() -> Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show()));
                }
            }
    );

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
                    dialog.setTitle("Restore Backup");
                    dialog.setMessage(getString(R.string.backup_restore_warning));
                    dialog.setPositiveButton(getString(R.string.button_confirm), (confirmDialog, index) -> 
                        BackupManager.restoreBackup(requireContext(), uri, (success, msg) -> 
                            requireActivity().runOnUiThread(() -> Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show())));
                    dialog.setNegativeButton(getString(R.string.button_cancel), null);
                    dialog.show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(new Scope(DriveScopes.DRIVE_APPDATA)).build());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof MainActivity) { ((MainActivity) getActivity()).setFabVisible(false); }
        setPaddingForRecyclerView(binding.fragmentSettingsMainView, false);
        if (binding != null && defaultCardBgColor == null) {
            defaultCardBgColor = binding.cardNotifications.getCardBackgroundColor();
            defaultCardStrokeWidth = binding.cardNotifications.getStrokeWidth();
            defaultCardStrokeColor = binding.cardNotifications.getStrokeColorStateList();
        }
        setupSettings();
        setupCloudBackup();
        updateFeatureEntitlements();
    }

    /**
     * Initializes the settings UI components, observers, and listeners.
     */
    private void setupSettings() {
        if (binding == null) return;
        binding.txtVersion.setText(getString(R.string.settings_version_summary, BuildConfig.VERSION_NAME));
        updateNotificationStatus();
        
        binding.btnNotifications.setOnClickListener(v -> {
            NotificationManager nm = NotificationManager.getInstance(requireActivity());
            if (!nm.hasPermission()) { 
                nm.showInvitationDialog(); 
            } 
            else {
                com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
                dialog.setTitle(getString(R.string.settings_notifications_title));
                dialog.setMessage(getString(R.string.settings_notifications_manage_prompt));
                dialog.setPositiveButton(getString(R.string.action_settings), (d, w) -> {
                    nm.openNotificationAppSettings(requireContext());
                });
                dialog.setNegativeButton(getString(android.R.string.cancel), null);
                dialog.show();
            }
        });

        viewModel.getSoundName().observe(getViewLifecycleOwner(), name -> {
            if (binding != null && binding.txtSoundDesc != null) {
                binding.txtSoundDesc.setText(getString(R.string.settings_sound_summary, name));
            }
        });
        
        binding.btnNotifSound.setOnClickListener(v -> {
            SoundPickerBottomSheet picker = new SoundPickerBottomSheet();
            picker.setCurrentSoundUri(viewModel.getSoundUri().getValue());
            picker.setOnSoundSelectedListener((name, uri) -> {
                viewModel.setSound(name, uri);
                if (getActivity() instanceof MainActivity main) main.addInteractionScore(1.5f);
            });
            picker.show(getChildFragmentManager(), "SoundPicker");
        });

        viewModel.getBypassVolume().observe(getViewLifecycleOwner(), bypass -> {
            if (binding == null) return;
            binding.switchBypass.setChecked(bypass);
            binding.layoutVolume.setVisibility(bypass ? View.VISIBLE : View.GONE);
        });
        
        binding.btnBypassVolume.setOnClickListener(v -> {
            if (AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.BYPASS_VOLUME)) {
                viewModel.setBypassVolume(!java.util.Objects.equals(Boolean.TRUE, viewModel.getBypassVolume().getValue()));
                if (getActivity() instanceof MainActivity main) main.addInteractionScore(1.5f);
            } else { showRational(AppConfig.FeaturePassType.BYPASS_VOLUME); }
        });

        viewModel.getNotifVolume().observe(getViewLifecycleOwner(), volume -> {
            if (binding != null) binding.sliderVolume.setValue(volume);
        });
        
        binding.sliderVolume.addOnChangeListener((slider, value, fromUser) -> { 
            if (fromUser) viewModel.setNotifVolume((int) value); 
        });

        viewModel.getTheme().observe(getViewLifecycleOwner(), theme -> {
            if (binding == null) return;
            int id = (theme == SettingsViewModel.THEME_LIGHT) ? R.id.btn_theme_light : (theme == SettingsViewModel.THEME_DARK) ? R.id.btn_theme_dark : R.id.btn_theme_system;
            binding.toggleGroupTheme.check(id);
        });

        binding.toggleGroupTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || isThemeReverting) return;
            boolean isSystemDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            boolean matchesSystem = (checkedId == R.id.btn_theme_light && !isSystemDark) || (checkedId == R.id.btn_theme_dark && isSystemDark);
            if (checkedId != R.id.btn_theme_system && !matchesSystem && !AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.THEME)) {
                isThemeReverting = true;
                Integer current = viewModel.getTheme().getValue();
                int currentId = (current != null && current == SettingsViewModel.THEME_LIGHT) ? R.id.btn_theme_light : (current != null && current == SettingsViewModel.THEME_DARK) ? R.id.btn_theme_dark : R.id.btn_theme_system;
                showRational(AppConfig.FeaturePassType.THEME);
                group.post(() -> { group.check(currentId); isThemeReverting = false; });
                return;
            }
            int theme = (checkedId == R.id.btn_theme_light) ? SettingsViewModel.THEME_LIGHT : (checkedId == R.id.btn_theme_dark) ? SettingsViewModel.THEME_DARK : SettingsViewModel.THEME_SYSTEM;
            if (!java.util.Objects.equals(theme, viewModel.getTheme().getValue())) {
                viewModel.setTheme(theme);
                // Move ad scoring to here, after selection is applied
                if (getActivity() instanceof MainActivity main) main.addInteractionScore(1.5f);
            }
        });

        binding.btnLanguage.setOnClickListener(v -> showLanguageDialog());
        binding.txtVersion.setOnClickListener(v -> {
            cheatTapCount++;
            if (cheatTapCount >= 10) { 
                cheatTapCount = 0; 
                showCheatPasswordDialog(); 
            }
        });

        binding.btnBackup.setOnClickListener(v -> {
            if (AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.BACKUP)) { showBackupOptions(); } 
            else { showRational(AppConfig.FeaturePassType.BACKUP); }
        });

        binding.btnClearData.setOnClickListener(v -> showClearDataConfirmation());
        binding.btnSupport.setOnClickListener(v -> {
            if (AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.SUPPORT)) { showSupportOptionsDialog(); } 
            else { showRational(AppConfig.FeaturePassType.SUPPORT); }
        });
        
        viewModel.getQuietHoursRange().observe(getViewLifecycleOwner(), range -> {
            if (binding != null && binding.txtQuietHoursDesc != null) {
                binding.txtQuietHoursDesc.setText(getString(R.string.settings_quiet_hours_format, range));
            }
        });
        
        binding.btnQuietHours.setOnClickListener(v -> {
            if (AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.QUIET_HOURS)) { showQuietHoursPickers(); } 
            else { showRational(AppConfig.FeaturePassType.QUIET_HOURS); }
        });

        binding.btnSnoozeDuration.setOnClickListener(v -> showSnoozeDurationDialog());
        viewModel.getSnoozeDuration().observe(getViewLifecycleOwner(), duration -> {
            if (binding != null && binding.txtSnoozeDurationDesc != null) {
                binding.txtSnoozeDurationDesc.setText(getString(R.string.settings_snooze_duration_summary, duration));
            }
        });

        binding.btnMaxSnoozes.setOnClickListener(v -> showMaxSnoozesDialog());
        viewModel.getMaxSnoozes().observe(getViewLifecycleOwner(), max -> {
            if (binding != null && binding.txtMaxSnoozesDesc != null) {
                if (max != null && max == -1) binding.txtMaxSnoozesDesc.setText(R.string.settings_max_snoozes_unlimited_summary);
                else binding.txtMaxSnoozesDesc.setText(getString(R.string.settings_max_snoozes_summary, String.valueOf(max)));
            }
        });

        viewModel.getVibration().observe(getViewLifecycleOwner(), enabled -> {
            if (binding != null && binding.switchVibration != null && binding.containerVibrationDetails != null) {
                binding.switchVibration.setChecked(enabled);
                binding.containerVibrationDetails.setVisibility(enabled ? View.VISIBLE : View.GONE);
            }
        });
        
        binding.btnVibration.setOnClickListener(v -> {
            if (AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.VIBRATION)) {
                viewModel.setVibration(!java.util.Objects.equals(Boolean.TRUE, viewModel.getVibration().getValue()));
                if (getActivity() instanceof MainActivity main) main.addInteractionScore(1.5f);
            } else { showRational(AppConfig.FeaturePassType.VIBRATION); }
        });

        viewModel.getVibrationPattern().observe(getViewLifecycleOwner(), pattern -> {
            if (binding != null && binding.txtVibrationPatternDesc != null) {
                binding.txtVibrationPatternDesc.setText(pattern);
            }
        });

        viewModel.getFlashPattern().observe(getViewLifecycleOwner(), pattern -> {
            if (binding != null && binding.txtFlashPatternDesc != null) {
                binding.txtFlashPatternDesc.setText(pattern);
            }
        });

        binding.btnVibrationPattern.setOnClickListener(v -> showVibrationPatternPicker());
        binding.btnFlashPattern.setOnClickListener(v -> showFlashPatternPicker());

        viewModel.getStickyNotif().observe(getViewLifecycleOwner(), enabled -> {
            if (binding != null && binding.switchSticky != null) {
                binding.switchSticky.setChecked(enabled);
            }
        });
        
        binding.btnSticky.setOnClickListener(v -> {
            if (AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.STICKY_NOTIF)) {
                viewModel.setStickyNotif(!java.util.Objects.equals(Boolean.TRUE, viewModel.getStickyNotif().getValue()));
                if (getActivity() instanceof MainActivity main) main.addInteractionScore(1.5f);
            } else { showRational(AppConfig.FeaturePassType.STICKY_NOTIF); }
        });

        binding.btnDoseWindow.setOnClickListener(v -> {
            if (binding == null || binding.containerDoseWindowDetails == null) return;
            if (AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.DOSE_WINDOW)) {
                int visibility = (binding.containerDoseWindowDetails.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE;
                binding.containerDoseWindowDetails.setVisibility(visibility);
            } else { 
                showRational(AppConfig.FeaturePassType.DOSE_WINDOW); 
            }
        });
        
        com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager rcm = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance();
        binding.txtEarlyThresholdDesc.setText(getString(R.string.settings_threshold_format, rcm.getEarlyTakeThresholdMins()));
        binding.txtLateThresholdDesc.setText(getString(R.string.settings_threshold_format, rcm.getLateTakeThresholdMins()));
        
        binding.btnEarlyThreshold.setOnClickListener(v -> showThresholdPicker(true));
        binding.btnLateThreshold.setOnClickListener(v -> showThresholdPicker(false));
    }

    private void updateFeatureEntitlements() {
        if (binding == null || getContext() == null) return;
        boolean purchased = AppConfig.isPremiumPurchased(requireContext());
        updateRowEntitlement(purchased, AppConfig.FeaturePassType.THEME, binding.crownTheme, binding.badgeActiveTheme);
        updateRowEntitlement(purchased, AppConfig.FeaturePassType.BACKUP, binding.crownBackup, binding.badgeActiveBackup);
        updateRowEntitlement(purchased, AppConfig.FeaturePassType.BYPASS_VOLUME, binding.crownBypass, binding.badgeActiveBypass);
        updateRowEntitlement(purchased, AppConfig.FeaturePassType.QUIET_HOURS, binding.crownQuietHours, binding.badgeActiveQuietHours);
        updateRowEntitlement(purchased, AppConfig.FeaturePassType.SUPPORT, binding.crownSupport, binding.badgeActiveSupport);
        updateRowEntitlement(purchased, AppConfig.FeaturePassType.VIBRATION, binding.crownVibration, binding.badgeActiveVibration);
        updateRowEntitlement(purchased, AppConfig.FeaturePassType.STICKY_NOTIF, binding.crownSticky, binding.badgeActiveSticky);
        updateRowEntitlement(purchased, AppConfig.FeaturePassType.DOSE_WINDOW, binding.crownDoseWindow, binding.badgeActiveDoseWindow);
        
        // Listeners for crowns to show premium dialog
        View.OnClickListener showPremium = v -> new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumSettings");
        if (binding.crownTheme != null) binding.crownTheme.setOnClickListener(showPremium);
        if (binding.crownBackup != null) binding.crownBackup.setOnClickListener(showPremium);
        if (binding.crownBypass != null) binding.crownBypass.setOnClickListener(showPremium);
        if (binding.crownQuietHours != null) binding.crownQuietHours.setOnClickListener(showPremium);
        if (binding.crownSupport != null) binding.crownSupport.setOnClickListener(showPremium);
        if (binding.crownVibration != null) binding.crownVibration.setOnClickListener(showPremium);
        if (binding.crownSticky != null) binding.crownSticky.setOnClickListener(showPremium);
        if (binding.crownDoseWindow != null) binding.crownDoseWindow.setOnClickListener(showPremium);
    }

    private void updateRowEntitlement(boolean purchased, AppConfig.FeaturePassType type, @Nullable View crown, @Nullable View badge) {
        boolean unlocked = AppConfig.isFeatureUnlocked(requireContext(), type);
        if (crown != null) crown.setVisibility(purchased ? View.GONE : View.VISIBLE);
        if (badge != null) badge.setVisibility(!purchased && unlocked ? View.VISIBLE : View.GONE);
    }

    private void showRational(AppConfig.FeaturePassType type) {
        FeatureRationalBottomSheet.newInstance(type).show(getChildFragmentManager(), "FeatureRational");
        getChildFragmentManager().setFragmentResultListener("feature_unlocked", getViewLifecycleOwner(), (key, bundle) -> {
            updateFeatureEntitlements();
            String unlockedType = bundle.getString("feature_type");
            if (unlockedType == null) return;
            
            AppConfig.FeaturePassType feature = AppConfig.FeaturePassType.valueOf(unlockedType);
            switch (feature) {
                case BYPASS_VOLUME -> viewModel.setBypassVolume(true);
                case QUIET_HOURS -> showQuietHoursPickers();
                case SUPPORT -> showSupportOptionsDialog();
                case BACKUP -> updateCloudUi(GoogleAccountManager.getInstance(requireContext()), CloudBackupSettings.getInstance(requireContext()));
                case VIBRATION -> viewModel.setVibration(true);
                case STICKY_NOTIF -> viewModel.setStickyNotif(true);
                case DOSE_WINDOW -> binding.containerDoseWindowDetails.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showThresholdPicker(boolean isEarly) {
        String[] opts = {"15 min", "30 min", "45 min", "60 min", "90 min", "120 min", "180 min"};
        int[] vals = {15, 30, 45, 60, 90, 120, 180};
        
        com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager rcm = com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager.getInstance();
        int currentVal = isEarly ? rcm.getEarlyTakeThresholdMins() : rcm.getLateTakeThresholdMins();
        int checkedItemIdx = 3; // Default 60
        for (int i = 0; i < vals.length; i++) { if (vals[i] == currentVal) { checkedItemIdx = i; break; } }

        com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        d.setTitle(getString(isEarly ? R.string.settings_early_threshold_title : R.string.settings_late_threshold_title));
        d.setSingleChoiceItems(opts, checkedItemIdx, (dialog, i) -> {
            // Future logic to save custom thresholds. Currently placeholders.
            if (isEarly) binding.txtEarlyThresholdDesc.setText(opts[i]);
            else binding.txtLateThresholdDesc.setText(opts[i]);
            
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).addInteractionScore(1.0f);
            dialog.dismiss();
        });
        d.show();
    }

    private void showFlashPatternPicker() {
        String[] opts = {"None", "Single Blink", "Double Pulse", "Strobe"};
        String current = viewModel.getFlashPattern().getValue();
        int checkedItemIdxFlash = 0;
        for (int i = 0; i < opts.length; i++) { if (opts[i].equalsIgnoreCase(current)) { checkedItemIdxFlash = i; break; } }

        com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        d.setTitle(getString(R.string.settings_flash_pattern_title));
        d.setSingleChoiceItems(opts, checkedItemIdxFlash, (dialog, i) -> {
            viewModel.setFlashPattern(opts[i]);
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).addInteractionScore(1.0f);
            dialog.dismiss();
        });
        d.show();
    }

    private void showVibrationPatternPicker() {
        String[] opts = {"Standard", "Heartbeat", "SOS", "Long Pulse"};
        String current = viewModel.getVibrationPattern().getValue();
        int checkedItemIdxVib = 0;
        for (int i = 0; i < opts.length; i++) { if (opts[i].equalsIgnoreCase(current)) { checkedItemIdxVib = i; break; } }

        com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        d.setTitle(getString(R.string.settings_vibration_pattern_title));
        d.setSingleChoiceItems(opts, checkedItemIdxVib, (dialog, i) -> {
            viewModel.setVibrationPattern(opts[i]);
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).addInteractionScore(1.0f);
            dialog.dismiss();
        });
        d.show();
    }

    private void showQuietHoursPickers() {
        MaterialTimePicker start = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setHour(23).setTitleText("Start?").build();
        start.addOnPositiveButtonClickListener(v -> {
            MaterialTimePicker end = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setHour(7).setTitleText("End?").build();
            end.addOnPositiveButtonClickListener(v2 -> {
                viewModel.setQuietHours(start.getHour(), start.getMinute(), end.getHour(), end.getMinute());
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).addInteractionScore(1.5f);
            });
            end.show(getChildFragmentManager(), "end_picker");
        });
        start.show(getChildFragmentManager(), "start_picker");
    }

    private void performCloudAction(boolean isBackup) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account == null) { showSignInRequiredDialog(); return; }
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(requireContext(), Collections.singleton(DriveScopes.DRIVE_APPDATA));
        credential.setSelectedAccount(account.getAccount());
        Drive service = new Drive.Builder(new NetHttpTransport(), new GsonFactory(), credential).setApplicationName("Medication Wizard").build();
        CloudBackupManager manager = new CloudBackupManager(requireContext(), new DriveServiceHelper(service));
        if (isBackup) {
            Snackbar.make(binding.getRoot(), R.string.backing_up_drive, Snackbar.LENGTH_SHORT).show();
            manager.backupToCloud().addOnSuccessListener(aVoid -> {
                Snackbar.make(binding.getRoot(), R.string.cloud_backup_success, Snackbar.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> Snackbar.make(binding.getRoot(), R.string.cloud_backup_failed, Snackbar.LENGTH_SHORT).show());
        } else {
            com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
            dialog.setTitle(getString(R.string.cloud_restore_title));
            dialog.setMessage(getString(R.string.cloud_restore_message));
            dialog.setPositiveButton(getString(R.string.button_restore), (restoreDialog, index) -> {
                Snackbar.make(binding.getRoot(), R.string.restoring_cloud, Snackbar.LENGTH_SHORT).show();
                manager.restoreFromCloud().addOnSuccessListener(success -> {
                    if (success) { 
                        Snackbar.make(binding.getRoot(), R.string.restore_success, Snackbar.LENGTH_LONG).show(); 
                        if (getActivity() != null) getActivity().recreate(); 
                    }
                    else { Snackbar.make(binding.getRoot(), R.string.no_backup_found, Snackbar.LENGTH_SHORT).show(); }
                }).addOnFailureListener(e -> Snackbar.make(binding.getRoot(), getString(R.string.restore_failed_format, e.getMessage()), Snackbar.LENGTH_SHORT).show());
            });
            dialog.setNegativeButton("Cancel", null);
            dialog.show();
        }
    }

    private void showCheatPasswordDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        d.setTitle("Developer Access"); d.setMessage("Enter access code"); d.setView(input);
        d.setPositiveButton(getString(R.string.button_confirm), (dialog, index) -> {
            if ("Gway1952".equals(input.getText().toString())) { new com.robinzon.medicationwizard.ui.cheats.CheatsBottomSheet().show(getChildFragmentManager(), "CheatsBS"); }
            else { Snackbar.make(binding.getRoot(), R.string.invalid_code, Snackbar.LENGTH_SHORT).show(); }
        });
        d.setNegativeButton(getString(R.string.button_cancel), null); d.show();
    }

    private void showSupportOptionsDialog() {
        com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        d.setTitle(getString(R.string.support_dialog_title)); d.setMessage(getString(R.string.support_dialog_message));
        d.setPositiveButton(getString(R.string.support_option_bug), (dialog, i) -> openEmailClient(true));
        d.setNegativeButton(getString(R.string.support_option_feature), (dialog, i) -> openEmailClient(false));
        d.show();
    }

    private void openEmailClient(boolean isBug) {
        String appVersion = BuildConfig.VERSION_NAME;
        String deviceName = android.os.Build.MODEL + " (" + android.os.Build.MANUFACTURER + ")";
        String subject = String.format(isBug ? getString(R.string.support_subject_bug) : getString(R.string.support_subject_feature), appVersion);
        StringBuilder body = new StringBuilder();
        body.append(getString(R.string.support_body_message_hint)).append("\n\n\n").append(getString(R.string.support_body_header)).append("\n").append(String.format(getString(R.string.support_body_version), appVersion)).append("\n").append(String.format(getString(R.string.support_body_device), deviceName)).append("\n").append(String.format(getString(R.string.support_body_os), android.os.Build.VERSION.RELEASE)).append("\n").append(String.format(getString(R.string.support_body_locale), java.util.Locale.getDefault().toString())).append("\n");
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(android.net.Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.support_email_address)});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body.toString());
        try { startActivity(intent); } catch (android.content.ActivityNotFoundException e) { Snackbar.make(binding.getRoot(), R.string.support_no_email_client, Snackbar.LENGTH_LONG).show(); }
    }

    private void handleSignInError(@Nullable Exception e) {
        int code = (e instanceof com.google.android.gms.common.api.ApiException) ? ((com.google.android.gms.common.api.ApiException) e).getStatusCode() : -1;
        int msg = switch (code) { case 7 -> R.string.sign_in_error_network; case 12501 -> R.string.sign_in_error_cancelled; case 10, 12500 -> R.string.sign_in_error_dev; default -> R.string.sign_in_error_generic; };
        if (getContext() != null) {
            com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(getContext());
            d.setTitle(getString(R.string.sign_in_error_title)); d.setMessage(getString(msg));
            d.setPositiveButton(getString(R.string.sign_in_error_btn_retry), (dialog, i) -> googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
            d.setNegativeButton(getString(android.R.string.cancel), null); d.show();
        }
    }

    private void handleSignInSuccess(GoogleSignInAccount account) {
        GoogleAccountManager.getInstance(requireContext()).saveAccountInfo(account.getEmail(), account.getDisplayName(), account.getPhotoUrl());
        updateCloudUi(GoogleAccountManager.getInstance(requireContext()), CloudBackupSettings.getInstance(requireContext()));
        if (getActivity() instanceof MainActivity) { ((MainActivity) getActivity()).refreshNavHeader(); }
    }

    private void setupCloudBackup() {
        GoogleAccountManager am = GoogleAccountManager.getInstance(requireContext());
        CloudBackupSettings cs = CloudBackupSettings.getInstance(requireContext());
        updateCloudUi(am, cs);
        View.OnClickListener sl = v -> { 
            if (!AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.BACKUP)) { showRational(AppConfig.FeaturePassType.BACKUP); return; } 
            if (!am.isSignedIn()) googleSignInLauncher.launch(googleSignInClient.getSignInIntent()); 
        };
        binding.btnGoogleSignin.setOnClickListener(sl); binding.btnGoogleSigninAction.setOnClickListener(sl);
        binding.btnSignOut.setOnClickListener(v -> googleSignInClient.signOut().addOnCompleteListener(t -> { am.clearAccountInfo(); updateCloudUi(am, cs); if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).refreshNavHeader(); }));
        binding.btnAutoBackup.setOnClickListener(v -> { if (!AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.BACKUP)) { showRational(AppConfig.FeaturePassType.BACKUP); return; } if (!am.isSignedIn()) { showSignInRequiredDialog(); return; } boolean cur = cs.isAutoBackupEnabled(); cs.setAutoBackupEnabled(!cur); binding.switchAutoBackup.setChecked(!cur); });
        binding.btnWifiOnly.setOnClickListener(v -> { if (!AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.BACKUP)) { showRational(AppConfig.FeaturePassType.BACKUP); return; } if (!am.isSignedIn()) { showSignInRequiredDialog(); return; } boolean cur = cs.isWifiOnlyEnabled(); cs.setWifiOnlyEnabled(!cur); binding.switchWifiOnly.setChecked(!cur); });
        binding.btnBackupNow.setOnClickListener(v -> { if (!AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.BACKUP)) { showRational(AppConfig.FeaturePassType.BACKUP); return; } if (!am.isSignedIn()) { showSignInRequiredDialog(); return; } performCloudAction(true); });
        binding.btnRestoreCloud.setOnClickListener(v -> { if (!AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.BACKUP)) { showRational(AppConfig.FeaturePassType.BACKUP); return; } if (!am.isSignedIn()) { showSignInRequiredDialog(); return; } performCloudAction(false); });
    }

    private void updateCloudUi(GoogleAccountManager am, CloudBackupSettings cs) {
        boolean unlocked = AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.BACKUP);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        boolean signedIn = account != null;
        if (!unlocked) {
            binding.containerCloudSettings.setVisibility(View.GONE); binding.btnSignOut.setVisibility(View.GONE); binding.btnGoogleSigninAction.setVisibility(View.VISIBLE);
            binding.txtAccountName.setText(R.string.cloud_backup_title); binding.txtAccountEmail.setText(R.string.cloud_backup_sign_in_hint);
            return;
        }
        binding.containerCloudSettings.setVisibility(View.VISIBLE); binding.btnSignOut.setVisibility(signedIn ? View.VISIBLE : View.GONE); binding.btnGoogleSigninAction.setVisibility(signedIn ? View.GONE : View.VISIBLE);
        float alpha = signedIn ? 1.0f : 0.4f;
        binding.btnAutoBackup.setAlpha(alpha); binding.btnWifiOnly.setAlpha(alpha);
        binding.btnBackupNow.setAlpha(alpha); binding.btnRestoreCloud.setAlpha(alpha);
        if (signedIn) {
            binding.txtAccountName.setText(account.getDisplayName()); binding.txtAccountEmail.setText(account.getEmail()); binding.switchAutoBackup.setChecked(cs.isAutoBackupEnabled()); binding.switchWifiOnly.setChecked(cs.isWifiOnlyEnabled());
        } else {
            binding.txtAccountName.setText(R.string.cloud_backup_title); binding.txtAccountEmail.setText(R.string.cloud_backup_sign_in_hint);
        }
    }

    public void updateNotificationStatus() {
        if (binding == null || getContext() == null) return;
        boolean granted = androidx.core.app.NotificationManagerCompat.from(requireContext().getApplicationContext()).areNotificationsEnabled();
        binding.containerAlertDetails.setVisibility(granted ? View.VISIBLE : View.GONE);
        if (granted) {
            binding.txtNotificationsTitle.setText(R.string.settings_notifications_title); binding.txtNotificationsSummary.setText(R.string.settings_notifications_summary);
            if (defaultCardBgColor != null) binding.cardNotifications.setCardBackgroundColor(defaultCardBgColor);
            binding.cardNotifications.setStrokeWidth(defaultCardStrokeWidth); if (defaultCardStrokeColor != null) binding.cardNotifications.setStrokeColor(defaultCardStrokeColor);
        } else {
            binding.txtNotificationsTitle.setText(R.string.notification_missing); binding.txtNotificationsSummary.setText(R.string.settings_notifications_disabled_summary);
            int primaryAttr = getContext().getResources().getIdentifier("colorPrimary", "attr", getContext().getPackageName());
            int surfaceAttr = getContext().getResources().getIdentifier("colorSurface", "attr", getContext().getPackageName());
            int primary = com.google.android.material.color.MaterialColors.getColor(requireContext(), primaryAttr, android.graphics.Color.BLUE);
            int surface = com.google.android.material.color.MaterialColors.getColor(requireContext(), surfaceAttr, android.graphics.Color.WHITE);
            binding.cardNotifications.setStrokeWidth((int) (1.5f * getResources().getDisplayMetrics().density)); binding.cardNotifications.setStrokeColor(primary);
            binding.cardNotifications.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(androidx.core.graphics.ColorUtils.blendARGB(surface, primary, 0.10f)));
        }
    }

    private void showBackupOptions() {
        String[] opts = {getString(R.string.backup_export), getString(R.string.backup_import)};
        com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        d.setTitle(getString(R.string.settings_backup_title)); d.setItems(opts, (dialog, i) -> { if (i == 0) exportLauncher.launch("medication_wizard_backup_" + System.currentTimeMillis() + ".json"); else importLauncher.launch(new String[]{"application/json"}); }); d.show();
    }

    private void showSnoozeDurationDialog() {
        String[] opts = {
            getString(R.string.snooze_5_min), 
            getString(R.string.snooze_10_min), 
            getString(R.string.snooze_15_min), 
            getString(R.string.snooze_20_min), 
            getString(R.string.snooze_30_min)
        }; 
        int[] vals = {5, 10, 15, 20, 30};
        
        int currentVal = viewModel.getSnoozeDuration().getValue() != null ? viewModel.getSnoozeDuration().getValue() : 10;
        int checkedItem = 1; // Default 10 min
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == currentVal) {
                checkedItem = i;
                break;
            }
        }

        com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        d.setTitle(getString(R.string.settings_snooze_duration_title)); 
        d.setSingleChoiceItems(opts, checkedItem, (dialog, i) -> {
            viewModel.setSnoozeDuration(vals[i]);
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).addInteractionScore(1.5f);
            dialog.dismiss();
        }); 
        d.show();
    }

    private void showMaxSnoozesDialog() {
        String[] opts = {
            getString(R.string.snooze_1_time), 
            getString(R.string.snooze_2_times), 
            getString(R.string.snooze_3_times), 
            getString(R.string.snooze_5_times), 
            getString(R.string.snooze_unlimited)
        };
        int[] vals = {1, 2, 3, 5, -1};
        
        int currentVal = viewModel.getMaxSnoozes().getValue() != null ? viewModel.getMaxSnoozes().getValue() : 3;
        int checkedItem = 2; // Default 3 times
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == currentVal) {
                checkedItem = i;
                break;
            }
        }

        com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        d.setTitle(getString(R.string.settings_max_snoozes_title)); 
        d.setSingleChoiceItems(opts, checkedItem, (dialog, i) -> {
            viewModel.setMaxSnoozes(vals[i]);
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).addInteractionScore(1.5f);
            dialog.dismiss();
        }); 
        d.show();
    }

    private void showLanguageDialog() {
        String[] langs = {getString(R.string.lang_english), getString(R.string.lang_hebrew), getString(R.string.lang_arabic), getString(R.string.lang_spanish), getString(R.string.lang_french), getString(R.string.lang_german), getString(R.string.lang_japanese), getString(R.string.lang_portuguese), getString(R.string.lang_korean)};
        String[] codes = {"en", "iw", "ar", "es", "fr", "de", "ja", "pt-BR", "ko"};
        com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        d.setTitle(getString(R.string.settings_language_title)); d.setItems(langs, (dialog, i) -> {
            String sel = codes[i]; if (!sel.equals(viewModel.getLanguageCode().getValue())) {
                com.robinzon.medicationwizard.ui.CustomMaterialDialog confirm = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
                confirm.setTitle(getString(R.string.settings_language_title)); confirm.setMessage(getString(R.string.settings_language_restart_warning));
                confirm.setPositiveButton(getString(R.string.button_ok), (d2, w) -> { 
                    viewModel.setLanguage(sel); 
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).addInteractionScore(1.5f);
                    if (getActivity() != null) getActivity().recreate(); 
                });
                confirm.setNegativeButton(getString(R.string.button_not_now), null); confirm.show();
            }
        }); d.show();
    }

    private void showClearDataConfirmation() {
        com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        d.setTitle(getString(R.string.settings_clear_data_title)); d.setMessage(getString(R.string.settings_clear_data_message));
        d.setPositiveButton(getString(android.R.string.yes), (dialog, i) -> { Medication.clearAllMedications(requireContext()); Snackbar.make(binding.getRoot(), R.string.data_cleared, Snackbar.LENGTH_SHORT).show(); });
        d.setNegativeButton(getString(android.R.string.no), null); d.show();
    }

    private void showSignInRequiredDialog() {
        com.robinzon.medicationwizard.ui.CustomMaterialDialog d = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        d.setTitle(getString(R.string.sign_in_required_title)); d.setMessage(getString(R.string.sign_in_required_message));
        d.setPositiveButton(getString(R.string.cloud_backup_title), (dialog, i) -> googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
        d.setNegativeButton(getString(R.string.button_not_now), null); d.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshSettings(); // Sync with potential pass consumption
        updateNotificationStatus();
        updateFeatureEntitlements();
        MainActivity main = (MainActivity) getActivity();
        if (main != null) { main.getAdsManager().addAdAvailabilityListener(this::updateFeatureEntitlements); }
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity main = (MainActivity) getActivity();
        if (main != null) { main.getAdsManager().removeAdAvailabilityListener(this::updateFeatureEntitlements); }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
