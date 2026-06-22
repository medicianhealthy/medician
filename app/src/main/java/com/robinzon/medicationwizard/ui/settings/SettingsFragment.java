package com.robinzon.medicationwizard.ui.settings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.android.material.button.MaterialButton;
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
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.Collections;

/**
 * Fragment that provides the user interface for all application settings.
 */
public class SettingsFragment extends MedicationWizardFragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean isThemeReverting = false;

    // Default styles for Health Alerts card to ensure a perfect match when enabled
    private android.content.res.ColorStateList mDefaultCardBgColor;
    private int mDefaultCardStrokeWidth;
    private android.content.res.ColorStateList mDefaultCardStrokeColor;

    private final android.os.Handler mProgressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable mProgressRunnable = this::refreshMagicPassProgress;
    private android.animation.AnimatorSet mMagicAnimator;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Logger.log("SettingsFragment", "Sign-in intent returned. Result code: " + result.getResultCode());
                
                // CRITICAL: We must use getSignedInAccountFromIntent even if result is not OK
                // as it extracts the specific ApiException and status codes.
                com.google.android.gms.tasks.Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                
                if (task.isSuccessful()) {
                    GoogleSignInAccount account = task.getResult();
                    Logger.log("SettingsFragment", "Sign-in success: " + account.getEmail());
                    handleSignInSuccess(account);
                    Snackbar.make(binding.getRoot(), getString(R.string.signed_in_as_format, account.getEmail()), Snackbar.LENGTH_SHORT).show();
                } else {
                    Exception e = task.getException();
                    Logger.log("SettingsFragment", "Sign-in task failed: " + (e != null ? e.getMessage() : "Unknown error"));
                    
                    // Filter out explicit user cancellation (12501)
                    if (e instanceof com.google.android.gms.common.api.ApiException) {
                        int code = ((com.google.android.gms.common.api.ApiException) e).getStatusCode();
                        if (code == 12501) {
                            Logger.log("SettingsFragment", "User cancelled sign-in picker.");
                            return;
                        }
                    }
                    
                    // For all other errors, show the high-end feedback dialog
                    handleSignInError(e);
                }
            }
    );

    private void handleSignInError(@Nullable Exception e) {
        int statusCode = -1;
        if (e instanceof com.google.android.gms.common.api.ApiException) {
            statusCode = ((com.google.android.gms.common.api.ApiException) e).getStatusCode();
            Logger.log("SettingsFragment", "Mapping error for status code: " + statusCode);
        }

        int messageResId = switch (statusCode) {
            case 7 -> R.string.sign_in_error_network; // NETWORK_ERROR
            case 12501 -> R.string.sign_in_error_cancelled; // SIGN_IN_CANCELLED
            case 10, 12500 -> R.string.sign_in_error_dev; // DEVELOPER_ERROR or SIGN_IN_FAILED (config)
            default -> R.string.sign_in_error_generic;
        };

        if (getContext() != null) {
            com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(getContext());
            dialog.setTitle(getString(R.string.sign_in_error_title));
            dialog.setMessage(getString(messageResId));
            dialog.setPositiveButton(getString(R.string.sign_in_error_btn_retry), (d, which) -> {
                googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
            });
            dialog.setNegativeButton(getString(android.R.string.cancel), null);
            dialog.show();
        }
    }

    private void handleSignInSuccess(GoogleSignInAccount account) {
        GoogleAccountManager.getInstance(requireContext()).saveAccountInfo(
                account.getEmail(), account.getDisplayName(), account.getPhotoUrl());
        updateCloudUi(GoogleAccountManager.getInstance(requireContext()), 
                      CloudBackupSettings.getInstance(requireContext()));
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshNavHeader();
        }
    }

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    BackupManager.createBackup(requireContext(), uri, (success, message) -> {
                        requireActivity().runOnUiThread(() -> 
                            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show());
                    });
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
            dialog.setPositiveButton(getString(R.string.button_confirm), (d, which) -> {
                BackupManager.restoreBackup(requireContext(), uri, (success, message) -> {
                    requireActivity().runOnUiThread(() -> 
                        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show());
                });
            });
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
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFabVisible(false);
        }
        setPaddingForRecyclerView(binding.fragmentSettingsMainView, false);

        // Capture default card styling only once to avoid capturing modified state
        if (binding != null && mDefaultCardBgColor == null) {
            mDefaultCardBgColor = binding.cardNotifications.getCardBackgroundColor();
            mDefaultCardStrokeWidth = binding.cardNotifications.getStrokeWidth();
            mDefaultCardStrokeColor = binding.cardNotifications.getStrokeColorStateList();
            Logger.log("SettingsFragment", "Captured defaults - Stroke: " + mDefaultCardStrokeWidth);
        }

        setupSettings();
        setupCloudBackup();
    }

    private final Runnable adAvailabilityListener = this::updateRewardedUi;

    private void setupSettings() {
        binding.txtVersion.setText(getString(R.string.settings_version_summary, BuildConfig.VERSION_NAME));

        MainActivity mainActivity = (MainActivity) getActivity();

        binding.cardMagicPass.setOnClickListener(v -> {
            if (mainActivity == null) return;

            // Allow clicking even if active to "extend" or re-apply magic
            if (mainActivity.getAdsManager().isRewardedLoaded()) {
                mainActivity.getAdsManager().showRewarded(success -> {
                    if (success && getContext() != null) {
                        long currentExpiry = SharedPreferencesManager.getInstance(getContext()).getLong(AppConfig.KEY_TEMP_PREMIUM_EXPIRY, 0);
                        long baseTime = Math.max(System.currentTimeMillis(), currentExpiry);
                        long newExpiry = baseTime + ((long) AppConfig.getMagicPassDurationHours() * 60 * 60 * 1000);
                        
                        SharedPreferencesManager.getInstance(getContext()).setLong(AppConfig.KEY_TEMP_PREMIUM_EXPIRY, newExpiry);
                        updateRewardedUi();
                        Snackbar.make(binding.getRoot(), getString(R.string.premium_pass_active), Snackbar.LENGTH_LONG).show();
                    }
                });
            } else {
                Snackbar.make(binding.getRoot(), R.string.reward_ad_not_ready, Snackbar.LENGTH_SHORT).show();
            }
        });

        updateNotificationStatus();
        binding.btnNotifications.setOnClickListener(v -> {
            NotificationManager nm = NotificationManager.getInstance(requireActivity());
            if (!nm.hasPermission()) {
                nm.showInvitationDialog();
            } else {
                com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
                dialog.setTitle(getString(R.string.settings_notifications_title));
                dialog.setMessage(getString(R.string.settings_notifications_manage_prompt));
                dialog.setPositiveButton(getString(R.string.action_settings), (d, which) -> nm.openNotificationAppSettings(requireContext()));
                dialog.setNegativeButton(getString(android.R.string.cancel), null);
                dialog.show();
            }
        });

        viewModel.getSoundName().observe(getViewLifecycleOwner(), name -> 
            binding.txtSoundDesc.setText(getString(R.string.settings_sound_summary, name)));
        
        binding.btnNotifSound.setOnClickListener(v -> {
            SoundPickerBottomSheet picker = new SoundPickerBottomSheet();
            picker.setCurrentSoundUri(viewModel.getSoundUri().getValue());
            picker.setOnSoundSelectedListener((name, uri) -> viewModel.setSound(name, uri));
            picker.show(getChildFragmentManager(), "SoundPicker");
        });

        viewModel.getBypassVolume().observe(getViewLifecycleOwner(), bypass -> {
            binding.switchBypass.setChecked(bypass);
            binding.layoutVolume.setVisibility(bypass ? View.VISIBLE : View.GONE);
        });
        
        binding.btnBypassVolume.setOnClickListener(v -> {
            if (AppConfig.isPremium(requireContext())) {
                viewModel.setBypassVolume(!Boolean.TRUE.equals(viewModel.getBypassVolume().getValue()));
            } else {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
            }
        });

        viewModel.getNotifVolume().observe(getViewLifecycleOwner(), volume -> 
            binding.sliderVolume.setValue(volume));
        
        binding.sliderVolume.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) viewModel.setNotifVolume((int) value);
        });

        viewModel.getTheme().observe(getViewLifecycleOwner(), theme -> {
            int buttonId;
            if (theme == SettingsViewModel.THEME_LIGHT) buttonId = R.id.btn_theme_light;
            else if (theme == SettingsViewModel.THEME_DARK) buttonId = R.id.btn_theme_dark;
            else buttonId = R.id.btn_theme_system;
            binding.toggleGroupTheme.check(buttonId);
        });

        binding.toggleGroupTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || isThemeReverting) return;
            
            // "System" is free. 
            // Light/Dark require Ad/Premium UNLESS it matches the current system theme.
            boolean isSystemDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            
            boolean matchesSystem = (checkedId == R.id.btn_theme_light && !isSystemDark) || 
                                     (checkedId == R.id.btn_theme_dark && isSystemDark);

            if (checkedId != R.id.btn_theme_system && !matchesSystem && !AppConfig.isPremium(requireContext())) {
                isThemeReverting = true;
                
                // Revert UI selection to current theme
                Integer currentTheme = viewModel.getTheme().getValue();
                int currentId = (currentTheme != null && currentTheme == SettingsViewModel.THEME_LIGHT) ? R.id.btn_theme_light : 
                                (currentTheme != null && currentTheme == SettingsViewModel.THEME_DARK) ? R.id.btn_theme_dark : R.id.btn_theme_system;
                
                // Show prompt to watch ad to unlock theme
                showThemeUnlockAdDialog(checkedId);
                
                // Re-check the previous one to avoid visual mismatch
                group.post(() -> {
                    group.check(currentId);
                    isThemeReverting = false;
                });
                return;
            }

            int theme = (checkedId == R.id.btn_theme_light) ? SettingsViewModel.THEME_LIGHT : 
                        (checkedId == R.id.btn_theme_dark) ? SettingsViewModel.THEME_DARK : SettingsViewModel.THEME_SYSTEM;
            viewModel.setTheme(theme);
        });

        viewModel.getLanguageCode().observe(getViewLifecycleOwner(), langCode -> {
            String langName = switch (langCode) {
                case "iw" -> getString(R.string.lang_hebrew);
                case "ar" -> getString(R.string.lang_arabic);
                case "es" -> getString(R.string.lang_spanish);
                case "fr" -> getString(R.string.lang_french);
                case "de" -> getString(R.string.lang_german);
                case "ja" -> getString(R.string.lang_japanese);
                case "pt-BR" -> getString(R.string.lang_portuguese);
                case "ko" -> getString(R.string.lang_korean);
                default -> getString(R.string.lang_english);
            };
            binding.txtLanguageDesc.setText(langName);
        });

        binding.btnLanguage.setOnClickListener(v -> {
            String[] langs = {
                    getString(R.string.lang_english),
                    getString(R.string.lang_hebrew),
                    getString(R.string.lang_arabic),
                    getString(R.string.lang_spanish),
                    getString(R.string.lang_french),
                    getString(R.string.lang_german),
                    getString(R.string.lang_japanese),
                    getString(R.string.lang_portuguese),
                    getString(R.string.lang_korean)
            };
            String[] codes = {"en", "iw", "ar", "es", "fr", "de", "ja", "pt-BR", "ko"};

            com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
            dialog.setTitle(getString(R.string.settings_language_title));
            dialog.setItems(langs, (d, which) -> {
                String selectedCode = codes[which];
                if (!selectedCode.equals(viewModel.getLanguageCode().getValue())) {
                    com.robinzon.medicationwizard.ui.CustomMaterialDialog confirmDialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
                    confirmDialog.setTitle(getString(R.string.settings_language_title));
                    confirmDialog.setMessage(getString(R.string.settings_language_restart_warning));
                    confirmDialog.setPositiveButton(getString(R.string.button_ok), (d2, w) -> {
                        viewModel.setLanguage(selectedCode);
                        if (getActivity() != null) {
                            android.content.Intent intent = getActivity().getIntent();
                            getActivity().finish();
                            getActivity().startActivity(intent);
                        }
                    });
                    confirmDialog.setNegativeButton(getString(R.string.button_not_now), null);
                    confirmDialog.show();
                }
            });
            dialog.show();
        });

        // Use a more reliable trigger: 10 taps on the version text
        binding.txtVersion.setOnClickListener(new View.OnClickListener() {
            int count = 0;
            @Override
            public void onClick(View v) {
                count++;
                if (count >= 10) {
                    count = 0;
                    showCheatPasswordDialog();
                }
            }
        });

        binding.btnBackup.setOnClickListener(v -> {
            String[] options = {getString(R.string.backup_export), getString(R.string.backup_import)};
            com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
            dialog.setTitle(getString(R.string.settings_backup_title));
            dialog.setItems(options, (d, which) -> {
                if (which == 0) exportLauncher.launch("medication_wizard_backup_" + System.currentTimeMillis() + ".json");
                else importLauncher.launch(new String[]{"application/json"});
            });
            dialog.show();
        });

        binding.btnClearData.setOnClickListener(v -> {
            com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
            dialog.setTitle(getString(R.string.settings_clear_data_title));
            dialog.setMessage(getString(R.string.settings_clear_data_message));
            dialog.setPositiveButton(getString(android.R.string.yes), (d, which) -> {
                // 1. Clear local database
                Medication.clearAllMedications(requireContext());
                
                // 2. Clear cloud backup if signed in
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
                if (account != null) {
                    Drive googleDriveService = new Drive.Builder(
                            new com.google.api.client.http.javanet.NetHttpTransport(),
                            new com.google.api.client.json.gson.GsonFactory(),
                            com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
                                    requireContext(), Collections.singleton(DriveScopes.DRIVE_APPDATA))
                                    .setSelectedAccount(account.getAccount())
                    ).setApplicationName("Medication Wizard").build();

                    DriveServiceHelper driveHelper = new DriveServiceHelper(googleDriveService);
                    CloudBackupManager cloudManager = new CloudBackupManager(requireContext(), driveHelper);
                    cloudManager.deleteBackup().addOnFailureListener(e -> 
                        com.robinzon.medicationwizard.utils.Logger.log("SettingsFragment", "Failed to delete cloud backup: " + e.getMessage()));
                }

                Snackbar.make(binding.getRoot(), R.string.data_cleared, Snackbar.LENGTH_SHORT).show();
            });
            dialog.setNegativeButton(getString(android.R.string.no), null);
            dialog.show();
        });

        binding.btnSupport.setOnClickListener(v -> showSupportOptionsDialog());
        
        viewModel.getQuietHoursRange().observe(getViewLifecycleOwner(), range -> 
            binding.txtQuietHoursDesc.setText(getString(R.string.settings_quiet_hours_format, range)));

        binding.btnQuietHours.setOnClickListener(v -> {
            if (!AppConfig.isPremium(requireContext())) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
            showQuietHoursPickers();
        });

        viewModel.getSnoozeDuration().observe(getViewLifecycleOwner(), mins -> 
            binding.txtSnoozeDurationDesc.setText(getString(R.string.settings_snooze_duration_summary, mins)));
        
        binding.btnSnoozeDuration.setOnClickListener(v -> {
            String[] options = {"5 min", "10 min", "15 min", "20 min", "30 min"};
            int[] values = {5, 10, 15, 20, 30};
            com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
            dialog.setTitle(getString(R.string.settings_snooze_duration_title));
            dialog.setItems(options, (d, which) -> viewModel.setSnoozeDuration(values[which]));
            dialog.show();
        });

        viewModel.getMaxSnoozes().observe(getViewLifecycleOwner(), max -> {
            if (max != null && max == -1) binding.txtMaxSnoozesDesc.setText(R.string.settings_max_snoozes_unlimited_summary);
            else binding.txtMaxSnoozesDesc.setText(getString(R.string.settings_max_snoozes_summary, String.valueOf(max)));
        });

        binding.btnMaxSnoozes.setOnClickListener(v -> {
            String[] options = {"1 time", "2 times", "3 times", "5 times", "Unlimited"};
            int[] values = {1, 2, 3, 5, -1};
            com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
            dialog.setTitle(getString(R.string.settings_max_snoozes_title));
            dialog.setItems(options, (d, which) -> viewModel.setMaxSnoozes(values[which]));
            dialog.show();
        });

        // Initialize UI state based on current premium status
        updateRewardedUi();
    }

    private void setupCloudBackup() {
        GoogleAccountManager accountManager = GoogleAccountManager.getInstance(requireContext());
        CloudBackupSettings cloudSettings = CloudBackupSettings.getInstance(requireContext());

        updateCloudUi(accountManager, cloudSettings);

        View.OnClickListener signInListener = v -> {
            if (!AppConfig.isPremium(requireContext())) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
            if (!accountManager.isSignedIn()) {
                googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
            }
        };
        binding.btnGoogleSignin.setOnClickListener(signInListener);
        binding.btnGoogleSigninAction.setOnClickListener(signInListener);

        binding.btnSignOut.setOnClickListener(v -> {
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                accountManager.clearAccountInfo();
                updateCloudUi(accountManager, cloudSettings);
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshNavHeader();
                }
            });
        });

        binding.btnAutoBackup.setOnClickListener(v -> {
            if (!AppConfig.isPremium(requireContext())) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
            if (!accountManager.isSignedIn()) {
                showSignInRequiredDialog();
                return;
            }
            boolean current = cloudSettings.isAutoBackupEnabled();
            cloudSettings.setAutoBackupEnabled(!current);
            binding.switchAutoBackup.setChecked(!current);
        });

        binding.btnWifiOnly.setOnClickListener(v -> {
            if (!AppConfig.isPremium(requireContext())) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
            if (!accountManager.isSignedIn()) {
                showSignInRequiredDialog();
                return;
            }
            boolean current = cloudSettings.isWifiOnlyEnabled();
            cloudSettings.setWifiOnlyEnabled(!current);
            binding.switchWifiOnly.setChecked(!current);
        });

        binding.btnBackupNow.setOnClickListener(v -> {
            if (!AppConfig.isPremium(requireContext())) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
            if (!accountManager.isSignedIn()) {
                showSignInRequiredDialog();
                return;
            }
            performCloudAction(true);
        });
        
        binding.btnRestoreCloud.setOnClickListener(v -> {
            if (!AppConfig.isPremium(requireContext())) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
            if (!accountManager.isSignedIn()) {
                showSignInRequiredDialog();
                return;
            }
            performCloudAction(false);
        });
    }

    private void showSignInRequiredDialog() {
        com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        dialog.setTitle(getString(R.string.sign_in_required_title));
        dialog.setMessage(getString(R.string.sign_in_required_message));
        dialog.setPositiveButton(getString(R.string.cloud_backup_title), (d, which) -> {
            googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
        });
        dialog.setNegativeButton(getString(R.string.button_not_now), null);
        dialog.show();
    }

    private void performCloudAction(boolean isBackup) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account == null) {
            showSignInRequiredDialog();
            return;
        }

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                requireContext(), Collections.singleton(DriveScopes.DRIVE_APPDATA));
        credential.setSelectedAccount(account.getAccount());

        Drive googleDriveService = new Drive.Builder(
                new NetHttpTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("Medication Wizard")
                .build();

        DriveServiceHelper driveHelper = new DriveServiceHelper(googleDriveService);
        CloudBackupManager manager = new CloudBackupManager(requireContext(), driveHelper);

        if (isBackup) {
            Snackbar.make(binding.getRoot(), R.string.backing_up_drive, Snackbar.LENGTH_SHORT).show();
            manager.backupToCloud().addOnSuccessListener(aVoid -> 
                Snackbar.make(binding.getRoot(), R.string.cloud_backup_success, Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> 
                Snackbar.make(binding.getRoot(), R.string.cloud_backup_failed, Snackbar.LENGTH_SHORT).show());
        } else {
            com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
            dialog.setTitle(getString(R.string.cloud_restore_title));
            dialog.setMessage(getString(R.string.cloud_restore_message));
            dialog.setPositiveButton(getString(R.string.button_restore), (d, which) -> {
                Snackbar.make(binding.getRoot(), R.string.restoring_cloud, Snackbar.LENGTH_SHORT).show();
                manager.restoreFromCloud().addOnSuccessListener(success -> {
                    if (success) {
                        Snackbar.make(binding.getRoot(), R.string.restore_success, Snackbar.LENGTH_LONG).show();
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).recreate();
                        }
                    } else {
                        Snackbar.make(binding.getRoot(), R.string.no_backup_found, Snackbar.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> 
                    Snackbar.make(binding.getRoot(), getString(R.string.restore_failed_format, e.getMessage()), Snackbar.LENGTH_SHORT).show());
            });
            dialog.setNegativeButton("Cancel", null);
            dialog.show();
        }
    }

    private void updateCloudUi(GoogleAccountManager accountManager, CloudBackupSettings cloudSettings) {
        boolean isPremium = com.robinzon.medicationwizard.AppConfig.isPremium(requireContext());
        
        // Check real Google Sign In state
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(requireContext());
        boolean signedIn = googleAccount != null;
        
        Logger.log("SettingsFragment", "Updating Cloud UI: Premium=" + isPremium + ", SignedIn=" + signedIn);

        if (!isPremium) {
            binding.containerCloudSettings.setVisibility(View.GONE);
            binding.btnSignOut.setVisibility(View.GONE);
            binding.btnGoogleSigninAction.setVisibility(View.VISIBLE);
            binding.txtAccountName.setText(R.string.cloud_backup_title);
            binding.txtAccountEmail.setText(R.string.premium_only_feature);
            binding.imgUserProfile.setImageResource(R.drawable.ic_magic_wand);
            return;
        }

        // For Premium users, sub-settings are always visible but might be "disabled"
        binding.containerCloudSettings.setVisibility(View.VISIBLE);
        binding.btnSignOut.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        binding.btnGoogleSigninAction.setVisibility(signedIn ? View.GONE : View.VISIBLE);

        float alpha = signedIn ? 1.0f : 0.4f;
        binding.btnAutoBackup.setAlpha(alpha);
        binding.btnWifiOnly.setAlpha(alpha);
        binding.btnBackupNow.setAlpha(alpha);
        binding.btnRestoreCloud.setAlpha(alpha);

        if (signedIn) {
            binding.txtAccountName.setText(googleAccount.getDisplayName());
            binding.txtAccountEmail.setText(googleAccount.getEmail());
            binding.switchAutoBackup.setChecked(cloudSettings.isAutoBackupEnabled());
            binding.switchWifiOnly.setChecked(cloudSettings.isWifiOnlyEnabled());
            
            android.net.Uri photoUri = googleAccount.getPhotoUrl();
            if (photoUri != null) {
                com.bumptech.glide.Glide.with(this)
                        .load(photoUri)
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(binding.imgUserProfile);
            } else {
                binding.imgUserProfile.setImageResource(R.mipmap.ic_launcher);
            }
            
            // Sync local cache for other components
            accountManager.saveAccountInfo(googleAccount.getEmail(), googleAccount.getDisplayName(), photoUri);
        } else {
            binding.txtAccountName.setText(R.string.cloud_backup_title);
            binding.txtAccountEmail.setText(R.string.cloud_backup_sign_in_hint);
            binding.imgUserProfile.setImageResource(R.mipmap.ic_launcher);
            accountManager.clearAccountInfo();
        }
    }

    private void showQuietHoursPickers() {
        MaterialTimePicker startPicker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setHour(23).setTitleText("Start?").build();
        startPicker.addOnPositiveButtonClickListener(v -> {
            MaterialTimePicker endPicker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setHour(7).setTitleText("End?").build();
            endPicker.addOnPositiveButtonClickListener(v2 -> viewModel.setQuietHours(startPicker.getHour(), startPicker.getMinute(), endPicker.getHour(), endPicker.getMinute()));
            endPicker.show(getChildFragmentManager(), "end_picker");
        });
        startPicker.show(getChildFragmentManager(), "start_picker");
    }

    private void showSnoozeDurationPicker() {
        String[] options = {"5 min", "10 min", "15 min", "20 min", "30 min"};
        int[] values = {5, 10, 15, 20, 30};
        com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        dialog.setTitle(getString(R.string.settings_snooze_duration_title));
        dialog.setItems(options, (d, which) -> viewModel.setSnoozeDuration(values[which]));
        dialog.show();
    }

    private void showMaxSnoozesPicker() {
        String[] options = {"1 time", "2 times", "3 times", "5 times", "Unlimited"};
        int[] values = {1, 2, 3, 5, -1};
        com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        dialog.setTitle(getString(R.string.settings_max_snoozes_title));
        dialog.setItems(options, (d, which) -> viewModel.setMaxSnoozes(values[which]));
        dialog.show();
    }

    private void showSupportOptionsDialog() {
        com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        dialog.setTitle(getString(R.string.support_dialog_title));
        dialog.setMessage(getString(R.string.support_dialog_message));
        dialog.setPositiveButton(getString(R.string.support_option_bug), (d, which) -> openEmailClient(true));
        dialog.setNegativeButton(getString(R.string.support_option_feature), (d, which) -> openEmailClient(false));
        // CustomMaterialDialog currently doesn't support neutral button, 
        // we'll skip it or add it if needed. 
        // For support, positive/negative are the main actions.
        dialog.show();
    }

    private void openEmailClient(boolean isBug) {
        String appVersion = BuildConfig.VERSION_NAME;
        String deviceName = android.os.Build.MODEL + " (" + android.os.Build.MANUFACTURER + ")";
        String osVersion = android.os.Build.VERSION.RELEASE;
        String locale = java.util.Locale.getDefault().toString();

        String subjectTemplate = isBug ? getString(R.string.support_subject_bug) : getString(R.string.support_subject_feature);
        String subject = String.format(subjectTemplate, appVersion);

        StringBuilder body = new java.lang.StringBuilder();
        body.append(getString(R.string.support_body_message_hint)).append("\n\n\n");
        body.append(getString(R.string.support_body_header)).append("\n");
        body.append(String.format(getString(R.string.support_body_version), appVersion)).append("\n");
        body.append(String.format(getString(R.string.support_body_device), deviceName)).append("\n");
        body.append(String.format(getString(R.string.support_body_os), osVersion)).append("\n");
        body.append(String.format(getString(R.string.support_body_locale), locale)).append("\n");

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(android.net.Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.support_email_address)});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body.toString());

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Snackbar.make(binding.getRoot(), R.string.support_no_email_client, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showCheatPasswordDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        
        com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        dialog.setTitle("Developer Access");
        dialog.setMessage("Enter access code");
        dialog.setView(input);
        dialog.setPositiveButton(getString(R.string.button_confirm), (d, which) -> {
            if ("Gway1952".equals(input.getText().toString())) {
                new com.robinzon.medicationwizard.ui.cheats.CheatsBottomSheet().show(getChildFragmentManager(), "CheatsBS");
            } else {
                Snackbar.make(binding.getRoot(), R.string.invalid_code, Snackbar.LENGTH_SHORT).show();
            }
        });
        dialog.setNegativeButton(getString(R.string.button_cancel), null);
        dialog.show();
    }

    private void showThemeUnlockAdDialog(int targetThemeId) {
        if (getContext() == null) return;
        
        com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        dialog.setTitle(getString(R.string.theme_unlock_title));
        dialog.setMessage(getString(R.string.theme_unlock_prompt, AppConfig.getMagicPassDurationHours()));
        dialog.setPositiveButton(getString(R.string.premium_pass_btn_watch), (d, which) -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null && mainActivity.getAdsManager().isRewardedLoaded()) {
                mainActivity.getAdsManager().showRewarded(success -> {
                    if (success && getContext() != null) {
                        long currentExpiry = SharedPreferencesManager.getInstance(getContext()).getLong(AppConfig.KEY_TEMP_PREMIUM_EXPIRY, 0);
                        long baseTime = Math.max(System.currentTimeMillis(), currentExpiry);
                        long newExpiry = baseTime + ((long) AppConfig.getMagicPassDurationHours() * 60 * 60 * 1000);
                        
                        SharedPreferencesManager.getInstance(getContext()).setLong(AppConfig.KEY_TEMP_PREMIUM_EXPIRY, newExpiry);
                        
                        int theme = (targetThemeId == R.id.btn_theme_light) ? SettingsViewModel.THEME_LIGHT : SettingsViewModel.THEME_DARK;
                        viewModel.setTheme(theme);
                        
                        updateRewardedUi();
                        Snackbar.make(binding.getRoot(), getString(R.string.premium_pass_active), Snackbar.LENGTH_LONG).show();
                    }
                });
            } else {
                Snackbar.make(binding.getRoot(), R.string.reward_ad_not_ready, Snackbar.LENGTH_SHORT).show();
            }
        });
        dialog.setNegativeButton(getString(android.R.string.cancel), null);
        dialog.show();
    }

    public void updateNotificationStatus() {
        if (binding == null || getContext() == null) return;
        
        // Non-cached check
        boolean isGranted = androidx.core.app.NotificationManagerCompat.from(requireContext().getApplicationContext()).areNotificationsEnabled();
        Logger.log("SettingsFragment", "updateNotificationStatus: granted=" + isGranted);
        
        binding.containerAlertDetails.setVisibility(isGranted ? View.VISIBLE : View.GONE);
        
        if (isGranted) {
            binding.txtNotificationsTitle.setText(R.string.settings_notifications_title);
            binding.txtNotificationsSummary.setText(R.string.settings_notifications_summary);
            
            // RESTORE ORIGINAL STYLE: 
            // We use the exact values captured in onViewCreated to ensure 100% parity with other cards.
            if (mDefaultCardBgColor != null) {
                binding.cardNotifications.setCardBackgroundColor(mDefaultCardBgColor);
            }
            binding.cardNotifications.setStrokeWidth(mDefaultCardStrokeWidth);
            if (mDefaultCardStrokeColor != null) {
                binding.cardNotifications.setStrokeColor(mDefaultCardStrokeColor);
            }
            
            Logger.log("SettingsFragment", "Restored card styling to defaults.");
        } else {
            binding.txtNotificationsTitle.setText(R.string.notification_missing);
            binding.txtNotificationsSummary.setText(R.string.settings_notifications_disabled_summary);
            
            float density = getResources().getDisplayMetrics().density;
            String pkg = requireContext().getPackageName();
            int primaryAttrId = getResources().getIdentifier("colorPrimary", "attr", pkg);
            int surfaceAttrId = getResources().getIdentifier("colorSurface", "attr", pkg);
            
            int primaryColor = com.google.android.material.color.MaterialColors.getColor(requireContext(), primaryAttrId, android.graphics.Color.BLUE);
            int surfaceColor = com.google.android.material.color.MaterialColors.getColor(requireContext(), surfaceAttrId, android.graphics.Color.WHITE);
            
            // STANDOUT EFFECT: primary-colored stroke and a subtle 10% primary tint
            binding.cardNotifications.setStrokeWidth((int) (1.5f * density));
            binding.cardNotifications.setStrokeColor(primaryColor);
            
            int blendedColor = androidx.core.graphics.ColorUtils.blendARGB(surfaceColor, primaryColor, 0.10f);
            binding.cardNotifications.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(blendedColor));
            
            Logger.log("SettingsFragment", "Applied standout styling to card.");
        }
    }

    private void updateRewardedUi() {
        if (getContext() == null || binding == null) return;
        
        boolean isFullPremium = AppConfig.IS_PREMIUM;
        long expiry = SharedPreferencesManager.getInstance(requireContext()).getLong(AppConfig.KEY_TEMP_PREMIUM_EXPIRY, 0);
        boolean isTempPremium = System.currentTimeMillis() < expiry;
        boolean isPremium = isFullPremium || isTempPremium;

        boolean isSystemDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        // Auto-reversion: If the theme is restricted but the user is no longer premium, revert to System.
        // A theme is restricted if it's NOT System AND it's NOT the current system theme.
        Integer currentTheme = viewModel.getTheme().getValue();
        boolean isThemeRestricted = false;
        if (currentTheme != null && currentTheme != SettingsViewModel.THEME_SYSTEM) {
            boolean matchesSystem = (currentTheme == SettingsViewModel.THEME_LIGHT && !isSystemDark) || 
                                     (currentTheme == SettingsViewModel.THEME_DARK && isSystemDark);
            if (!matchesSystem) {
                isThemeRestricted = true;
            }
        }

        if (isThemeRestricted && !isPremium) {
            // Signal to the listener to ignore this programmatic change
            isThemeReverting = true;
            viewModel.setTheme(SettingsViewModel.THEME_SYSTEM);
            // We'll reset this flag after a short delay to ensure the observer/listener cycle completes
            binding.getRoot().postDelayed(() -> isThemeReverting = false, 100);
        }

        MainActivity mainActivity = (MainActivity) getActivity();
        boolean rvLoaded = mainActivity != null && mainActivity.getAdsManager().isRewardedLoaded();

        if (isFullPremium) {
            binding.cardMagicPass.setVisibility(View.GONE);
            binding.imgMagicPassStatus.setVisibility(View.GONE);
            binding.magicPassProgress.setVisibility(View.GONE);
            binding.badgeAdLight.setVisibility(View.GONE);
            binding.badgeAdDark.setVisibility(View.GONE);
        } else if (isTempPremium) {
            binding.cardMagicPass.setVisibility(View.VISIBLE);
            binding.cardMagicPass.setAlpha(rvLoaded ? 1.0f : 0.6f);
            binding.txtMagicPassTitle.setText(R.string.premium_pass_active);
            long diff = expiry - System.currentTimeMillis();
            long hours = diff / (60 * 60 * 1000);
            long mins = (diff % (60 * 60 * 1000)) / (60 * 1000);
            String timeLeft = getString(R.string.premium_pass_remaining_format, hours, mins);
            binding.txtMagicPassSummary.setText(rvLoaded ? timeLeft : getString(R.string.loading_magic));
            
            // Show Checkmark for active pass
            binding.imgMagicPassStatus.setVisibility(View.VISIBLE);
            binding.imgMagicPassStatus.setImageResource(R.drawable.ic_done_pill);
            
            // Show and update progress bar
            refreshMagicPassProgress();
            
            binding.badgeAdLight.setVisibility(View.GONE);
            binding.badgeAdDark.setVisibility(View.GONE);
        } else {
            binding.cardMagicPass.setVisibility(View.VISIBLE);
            binding.txtMagicPassTitle.setText(getString(R.string.premium_pass_title, AppConfig.getMagicPassDurationHours()));
            
            // Show Magic Wand for inactive pass
            binding.imgMagicPassStatus.setVisibility(View.VISIBLE);
            binding.imgMagicPassStatus.setImageResource(R.drawable.ic_magic_wand);
            
            // Show empty progress bar for inactive pass
            refreshMagicPassProgress();
            
            if (rvLoaded) {
                binding.cardMagicPass.setAlpha(1.0f);
                binding.txtMagicPassSummary.setText(getString(R.string.premium_pass_summary, AppConfig.getMagicPassDurationHours()));
                binding.cardMagicPass.setEnabled(true);
            } else {
                binding.cardMagicPass.setAlpha(0.6f);
                binding.txtMagicPassSummary.setText(R.string.loading_magic);
                binding.cardMagicPass.setEnabled(true);
            }
            
            // Dynamic AD badges based on system theme:
            // If system is Light, "Light" is free (no badge), "Dark" requires Ad.
            // If system is Dark, "Dark" is free (no badge), "Light" requires Ad.
            binding.badgeAdLight.setVisibility(isSystemDark ? View.VISIBLE : View.GONE);
            binding.badgeAdDark.setVisibility(isSystemDark ? View.GONE : View.VISIBLE);
        }
    }

    private void refreshMagicPassProgress() {
        if (binding == null || getContext() == null) return;

        long expiry = SharedPreferencesManager.getInstance(requireContext()).getLong(AppConfig.KEY_TEMP_PREMIUM_EXPIRY, 0);
        long currentTime = System.currentTimeMillis();
        long totalDuration = (long) AppConfig.getMagicPassDurationHours() * 60 * 60 * 1000;

        int progress;
        if (currentTime < expiry) {
            long remaining = expiry - currentTime;
            progress = (int) ((remaining * 100) / totalDuration);
            progress = Math.max(1, Math.min(100, progress)); // Keep at least 1% if active
        } else {
            progress = 0;
        }

        // 1. Animate the bar itself
        int finalProgress = progress;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.magicPassProgress.setProgress(finalProgress, true);
        } else {
            binding.magicPassProgress.setProgress(finalProgress);
        }

        // 2. Position and animate the Sparkle at the tip
        binding.magicPassProgress.post(() -> {
            if (binding == null || getContext() == null) return;
            float width = binding.magicPassProgress.getWidth();
            float tipX = (width * finalProgress) / 100f;
            
            // Adjust for sparkle center
            binding.imgMagicSparkle.animate()
                    .translationX(tipX - (binding.imgMagicSparkle.getWidth() / 2f))
                    .setDuration(currentTime < expiry ? 1000 : 0)
                    .start();
            
            // 3. Keep the "alive" magic pulse running
            if (finalProgress > 0) {
                startMagicPulse();
            } else {
                stopMagicPulse();
            }
        });

        if (currentTime < expiry) {
            mProgressHandler.removeCallbacks(mProgressRunnable);
            mProgressHandler.postDelayed(mProgressRunnable, 30000);
        }
    }

    private void startMagicPulse() {
        if (mMagicAnimator != null && mMagicAnimator.isRunning()) return;

        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(binding.imgMagicSparkle, "scaleX", 0.8f, 1.3f, 0.8f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(binding.imgMagicSparkle, "scaleY", 0.8f, 1.3f, 0.8f);
        android.animation.ObjectAnimator rotate = android.animation.ObjectAnimator.ofFloat(binding.imgMagicSparkle, "rotation", 0f, 180f);
        android.animation.ObjectAnimator alpha = android.animation.ObjectAnimator.ofFloat(binding.magicPassProgress, "alpha", 0.6f, 1.0f, 0.6f);

        mMagicAnimator = new android.animation.AnimatorSet();
        mMagicAnimator.playTogether(scaleX, scaleY, rotate, alpha);
        mMagicAnimator.setDuration(2000);
        mMagicAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        
        mMagicAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (binding != null && getContext() != null && AppConfig.isPremium(requireContext())) {
                    animation.start();
                }
            }
        });
        mMagicAnimator.start();
    }

    private void stopMagicPulse() {
        if (mMagicAnimator != null) {
            mMagicAnimator.removeAllListeners();
            mMagicAnimator.cancel();
            mMagicAnimator = null;
        }
        if (binding != null) {
            binding.imgMagicSparkle.setScaleX(1.0f);
            binding.imgMagicSparkle.setScaleY(1.0f);
            binding.imgMagicSparkle.setAlpha(1.0f);
            binding.magicPassProgress.setAlpha(1.0f);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNotificationStatus();
        updateRewardedUi();
        
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.getAdsManager().addAdAvailabilityListener(adAvailabilityListener);
        }

        // Sync account info with Google Sign In state
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        GoogleAccountManager accountManager = GoogleAccountManager.getInstance(requireContext());
        if (account != null) {
            accountManager.saveAccountInfo(account.getEmail(), account.getDisplayName(), account.getPhotoUrl());
        }
        
        updateCloudUi(accountManager, CloudBackupSettings.getInstance(requireContext()));
    }

    @Override
    public void onPause() {
        super.onPause();
        mProgressHandler.removeCallbacks(mProgressRunnable);
        stopMagicPulse();
        
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.getAdsManager().removeAdAvailabilityListener(adAvailabilityListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mProgressHandler.removeCallbacksAndMessages(null);
        if (mMagicAnimator != null) {
            mMagicAnimator.removeAllListeners();
            mMagicAnimator.cancel();
            mMagicAnimator = null;
        }
        binding = null;
    }
}
