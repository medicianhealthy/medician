package com.robinzon.medicationwizard.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.backup.CloudBackupSettings;
import com.robinzon.medicationwizard.backup.GoogleAccountManager;
import com.robinzon.medicationwizard.databinding.FragmentSettingsBinding;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.entities.MedicationWizardFragment;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.utils.BackupManager;
import android.content.Intent;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.backup.CloudBackupManager;
import com.robinzon.medicationwizard.backup.DriveServiceHelper;
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
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle(R.string.sign_in_error_title)
                    .setMessage(messageResId)
                    .setPositiveButton(R.string.sign_in_error_btn_retry, (dialog, which) -> {
                        googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
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
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Restore Backup")
                            .setMessage(R.string.backup_restore_warning)
                            .setPositiveButton("Restore", (dialog, which) -> {
                                BackupManager.restoreBackup(requireContext(), uri, (success, message) -> {
                                    requireActivity().runOnUiThread(() -> 
                                        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show());
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
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

        setupSettings();
        setupCloudBackup();
    }

    private final Runnable adAvailabilityListener = this::updateRewardedUi;

    private void setupSettings() {
        binding.txtVersion.setText(getString(R.string.settings_version_summary, BuildConfig.VERSION_NAME));

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.getAdsManager().addAdAvailabilityListener(adAvailabilityListener);
        }

        binding.cardMagicPass.setOnClickListener(v -> {
            if (mainActivity == null) return;

            // Allow clicking even if active to "extend" or re-apply magic
            if (mainActivity.getAdsManager().isRewardedLoaded()) {
                mainActivity.getAdsManager().showRewarded(success -> {
                    if (success && getContext() != null) {
                        long currentExpiry = SharedPreferencesManager.getInstance(getContext()).getLong(AppConfig.KEY_TEMP_PREMIUM_EXPIRY, 0);
                        long baseTime = Math.max(System.currentTimeMillis(), currentExpiry);
                        long newExpiry = baseTime + (12 * 60 * 60 * 1000);
                        
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
            if (!AppConfig.isPremium(requireContext())) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
            boolean isGranted = NotificationManager.getInstance(requireActivity()).hasPermission();
            if (!isGranted) {
                NotificationManager.getInstance(requireActivity()).requestPermissionIfNeeded();
            } else {
                Snackbar.make(binding.getRoot(), R.string.notif_already_active, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getSoundName().observe(getViewLifecycleOwner(), name -> 
            binding.txtSoundDesc.setText(getString(R.string.settings_sound_summary, name)));
        
        binding.btnNotifSound.setOnClickListener(v -> {
            if (AppConfig.isPremium(requireContext())) {
                SoundPickerBottomSheet picker = new SoundPickerBottomSheet();
                picker.setCurrentSoundUri(viewModel.getSoundUri().getValue());
                picker.setOnSoundSelectedListener((name, uri) -> viewModel.setSound(name, uri));
                picker.show(getChildFragmentManager(), "SoundPicker");
            } else {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
            }
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
            
            // "System" is free, Light/Dark require Premium/Ad
            if (checkedId != R.id.btn_theme_system && !AppConfig.isPremium(requireContext())) {
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
            if (!AppConfig.isPremium(requireContext())) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
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

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.settings_language_title)
                    .setItems(langs, (dialog, which) -> {
                        String selectedCode = codes[which];
                        if (!selectedCode.equals(viewModel.getLanguageCode().getValue())) {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.settings_language_title)
                                    .setMessage(R.string.settings_language_restart_warning)
                                    .setPositiveButton(R.string.button_ok, (d, w) -> {
                                        viewModel.setLanguage(selectedCode);
                                        if (getActivity() != null) {
                                            android.content.Intent intent = getActivity().getIntent();
                                            getActivity().finish();
                                            getActivity().startActivity(intent);
                                        }
                                    })
                                    .setNegativeButton(R.string.buttoh_not_now, null)
                                    .show();
                        }
                    })
                    .show();
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
            if (!AppConfig.isPremium(requireContext())) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
            String[] options = {getString(R.string.backup_export), getString(R.string.backup_import)};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.settings_backup_title)
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) exportLauncher.launch("medication_wizard_backup_" + System.currentTimeMillis() + ".json");
                        else importLauncher.launch(new String[]{"application/json"});
                    })
                    .show();
        });

        binding.btnClearData.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Wipe everything?")
                    .setMessage("This will delete all your medications and history.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Medication.clearAllMedications(requireContext());
                        Snackbar.make(binding.getRoot(), R.string.data_cleared, Snackbar.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
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
            if (!AppConfig.isPremium(requireContext())) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
            showSnoozeDurationPicker();
        });

        viewModel.getMaxSnoozes().observe(getViewLifecycleOwner(), max -> {
            if (max != null && max == -1) binding.txtMaxSnoozesDesc.setText(R.string.settings_max_snoozes_unlimited_summary);
            else binding.txtMaxSnoozesDesc.setText(getString(R.string.settings_max_snoozes_summary, String.valueOf(max)));
        });

        binding.btnMaxSnoozes.setOnClickListener(v -> {
            if (!AppConfig.isPremium(requireContext())) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
            showMaxSnoozesPicker();
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
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sign_in_required_title)
                .setMessage(R.string.sign_in_required_message)
                .setPositiveButton(R.string.cloud_backup_title, (dialog, which) -> {
                    googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
                })
                .setNegativeButton(R.string.buttoh_not_now, null)
                .show();
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
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.cloud_restore_title)
                    .setMessage(R.string.cloud_restore_message)
                    .setPositiveButton(R.string.button_restore, (dialog, which) -> {
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
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
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
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.settings_snooze_duration_title).setItems(options, (dialog, which) -> viewModel.setSnoozeDuration(values[which])).show();
    }

    private void showMaxSnoozesPicker() {
        String[] options = {"1 time", "2 times", "3 times", "5 times", "Unlimited"};
        int[] values = {1, 2, 3, 5, -1};
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.settings_max_snoozes_title).setItems(options, (dialog, which) -> viewModel.setMaxSnoozes(values[which])).show();
    }

    private void showSupportOptionsDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.support_dialog_title)
                .setMessage(R.string.support_dialog_message)
                .setPositiveButton(R.string.support_option_bug, (dialog, which) -> openEmailClient(true))
                .setNegativeButton(R.string.support_option_feature, (dialog, which) -> openEmailClient(false))
                .setNeutralButton(R.string.buttoh_not_now, null)
                .show();
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
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Developer Access")
                .setMessage("Enter access code")
                .setView(input)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    if ("Gway1952".equals(input.getText().toString())) {
                        new com.robinzon.medicationwizard.ui.cheats.CheatsBottomSheet().show(getChildFragmentManager(), "CheatsBS");
                    } else {
                        Snackbar.make(binding.getRoot(), R.string.invalid_code, Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showThemeUnlockAdDialog(int targetThemeId) {
        if (getContext() == null) return;
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme_unlock_title)
                .setMessage(R.string.theme_unlock_prompt)
                .setPositiveButton(R.string.premium_pass_btn_watch, (dialog, which) -> {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity != null && mainActivity.getAdsManager().isRewardedLoaded()) {
                        mainActivity.getAdsManager().showRewarded(success -> {
                            if (success && getContext() != null) {
                                long currentExpiry = SharedPreferencesManager.getInstance(getContext()).getLong(AppConfig.KEY_TEMP_PREMIUM_EXPIRY, 0);
                                long baseTime = Math.max(System.currentTimeMillis(), currentExpiry);
                                long newExpiry = baseTime + (12 * 60 * 60 * 1000);
                                
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
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateNotificationStatus() {
        boolean isGranted = NotificationManager.getInstance(requireActivity()).hasPermission();
        binding.switchNotifications.setChecked(isGranted);
        binding.containerAlertDetails.setVisibility(isGranted ? View.VISIBLE : View.GONE);
    }

    private void updateRewardedUi() {
        if (getContext() == null || binding == null) return;
        
        boolean isFullPremium = AppConfig.IS_PREMIUM;
        long expiry = SharedPreferencesManager.getInstance(requireContext()).getLong(AppConfig.KEY_TEMP_PREMIUM_EXPIRY, 0);
        boolean isTempPremium = System.currentTimeMillis() < expiry;
        boolean isPremium = isFullPremium || isTempPremium;

        // Auto-reversion: If the theme is restricted but the user is no longer premium, revert to System.
        Integer currentTheme = viewModel.getTheme().getValue();
        if (currentTheme != null && currentTheme != SettingsViewModel.THEME_SYSTEM && !isPremium) {
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
            ((MaterialButton) binding.btnThemeLight).setIcon(null);
            ((MaterialButton) binding.btnThemeDark).setIcon(null);
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
            
            ((MaterialButton) binding.btnThemeLight).setIcon(null);
            ((MaterialButton) binding.btnThemeDark).setIcon(null);
        } else {
            binding.cardMagicPass.setVisibility(View.VISIBLE);
            binding.txtMagicPassTitle.setText(R.string.premium_pass_title);
            
            // Show Magic Wand for inactive pass
            binding.imgMagicPassStatus.setVisibility(View.VISIBLE);
            binding.imgMagicPassStatus.setImageResource(R.drawable.ic_magic_wand);
            
            if (rvLoaded) {
                binding.cardMagicPass.setAlpha(1.0f);
                binding.txtMagicPassSummary.setText(R.string.premium_pass_summary);
                binding.cardMagicPass.setEnabled(true);
            } else {
                binding.cardMagicPass.setAlpha(0.6f);
                binding.txtMagicPassSummary.setText(R.string.loading_magic);
                binding.cardMagicPass.setEnabled(true);
            }
            ((MaterialButton) binding.btnThemeLight).setIconResource(R.drawable.ic_magic_wand);
            ((MaterialButton) binding.btnThemeDark).setIconResource(R.drawable.ic_magic_wand);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNotificationStatus();
        updateRewardedUi();
        
        // Sync account info with Google Sign In state
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        GoogleAccountManager accountManager = GoogleAccountManager.getInstance(requireContext());
        if (account != null) {
            accountManager.saveAccountInfo(account.getEmail(), account.getDisplayName(), account.getPhotoUrl());
        }
        
        updateCloudUi(accountManager, CloudBackupSettings.getInstance(requireContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}