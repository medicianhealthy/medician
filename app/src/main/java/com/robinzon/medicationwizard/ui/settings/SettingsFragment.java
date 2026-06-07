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
import com.robinzon.medicationwizard.backup.CloudBackupManager;
import com.robinzon.medicationwizard.backup.DriveServiceHelper;
import com.robinzon.medicationwizard.utils.Logger;

import java.util.Collections;

/**
 * Fragment that provides the user interface for all application settings.
 */
public class SettingsFragment extends MedicationWizardFragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Logger.log("SettingsFragment", "Sign-in result code: " + result.getResultCode());
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                            .addOnSuccessListener(account -> {
                                Logger.log("SettingsFragment", "Sign-in success: " + account.getEmail());
                                handleSignInSuccess(account);
                                Snackbar.make(binding.getRoot(), "Signed in as " + account.getEmail(), Snackbar.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Logger.log("SettingsFragment", "Sign-in failed: " + e.getMessage());
                                String errorMsg = e.getMessage();
                                if (e instanceof com.google.android.gms.common.api.ApiException) {
                                    int statusCode = ((com.google.android.gms.common.api.ApiException) e).getStatusCode();
                                    errorMsg = com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.getStatusCodeString(statusCode);
                                }
                                Snackbar.make(binding.getRoot(), "Sign-in failed: " + errorMsg, Snackbar.LENGTH_LONG).show();
                            });
                } else {
                    String message = (result.getResultCode() == android.app.Activity.RESULT_CANCELED) ? 
                            "Sign-in cancelled" : "Sign-in failed (code: " + result.getResultCode() + ")";
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                    Logger.log("SettingsFragment", message);
                }
            }
    );

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

    private void setupSettings() {
        binding.txtVersion.setText(getString(R.string.settings_version_summary, BuildConfig.VERSION_NAME));

        updateNotificationStatus();
        binding.btnNotifications.setOnClickListener(v -> {
            boolean isGranted = NotificationManager.getInstance(requireActivity()).hasPermission();
            if (!isGranted) {
                NotificationManager.getInstance(requireActivity()).requestPermissionIfNeeded();
            } else {
                Snackbar.make(binding.getRoot(), "Notifications are already active!", Snackbar.LENGTH_LONG).show();
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
            if (com.robinzon.medicationwizard.AppConfig.IS_PREMIUM) {
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
            if (!isChecked) return;
            int theme = (checkedId == R.id.btn_theme_light) ? SettingsViewModel.THEME_LIGHT : 
                        (checkedId == R.id.btn_theme_dark) ? SettingsViewModel.THEME_DARK : SettingsViewModel.THEME_SYSTEM;
            viewModel.setTheme(theme);
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
                        Snackbar.make(binding.getRoot(), "Data cleared.", Snackbar.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        binding.btnSupport.setOnClickListener(v -> Snackbar.make(binding.getRoot(), "Support coming soon!", Snackbar.LENGTH_SHORT).show());
        
        viewModel.getQuietHoursRange().observe(getViewLifecycleOwner(), range -> 
            binding.txtQuietHoursDesc.setText(getString(R.string.settings_quiet_hours_format, range)));

        binding.btnQuietHours.setOnClickListener(v -> {
            if (!com.robinzon.medicationwizard.AppConfig.IS_PREMIUM) {
                new PremiumBottomSheet().show(getChildFragmentManager(), "PremiumBS");
                return;
            }
            showQuietHoursPickers();
        });

        viewModel.getSnoozeDuration().observe(getViewLifecycleOwner(), mins -> 
            binding.txtSnoozeDurationDesc.setText(getString(R.string.settings_snooze_duration_summary, mins)));
        
        binding.btnSnoozeDuration.setOnClickListener(v -> showSnoozeDurationPicker());

        viewModel.getMaxSnoozes().observe(getViewLifecycleOwner(), max -> {
            if (max != null && max == -1) binding.txtMaxSnoozesDesc.setText(R.string.settings_max_snoozes_unlimited_summary);
            else binding.txtMaxSnoozesDesc.setText(getString(R.string.settings_max_snoozes_summary, String.valueOf(max)));
        });

        binding.btnMaxSnoozes.setOnClickListener(v -> showMaxSnoozesPicker());
    }

    private void setupCloudBackup() {
        GoogleAccountManager accountManager = GoogleAccountManager.getInstance(requireContext());
        CloudBackupSettings cloudSettings = CloudBackupSettings.getInstance(requireContext());

        updateCloudUi(accountManager, cloudSettings);

        View.OnClickListener signInListener = v -> {
            if (!com.robinzon.medicationwizard.AppConfig.IS_PREMIUM) {
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
            if (!com.robinzon.medicationwizard.AppConfig.IS_PREMIUM) {
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
            if (!com.robinzon.medicationwizard.AppConfig.IS_PREMIUM) {
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
            if (!com.robinzon.medicationwizard.AppConfig.IS_PREMIUM) {
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
            if (!com.robinzon.medicationwizard.AppConfig.IS_PREMIUM) {
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
                .setTitle("Sign-in Required")
                .setMessage("Please sign in with your Google account to enable cloud backup and sync features.")
                .setPositiveButton("Sign In", (dialog, which) -> {
                    googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
                })
                .setNegativeButton("Cancel", null)
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
            Snackbar.make(binding.getRoot(), "Backing up to Google Drive...", Snackbar.LENGTH_SHORT).show();
            manager.backupToCloud().addOnSuccessListener(aVoid -> 
                Snackbar.make(binding.getRoot(), R.string.cloud_backup_success, Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> 
                Snackbar.make(binding.getRoot(), R.string.cloud_backup_failed, Snackbar.LENGTH_SHORT).show());
        } else {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Restore from Cloud")
                    .setMessage("This will replace all your local medications and history with the cloud version. Continue?")
                    .setPositiveButton("Restore", (dialog, which) -> {
                        Snackbar.make(binding.getRoot(), "Restoring from cloud...", Snackbar.LENGTH_SHORT).show();
                        manager.restoreFromCloud().addOnSuccessListener(success -> {
                            if (success) {
                                Snackbar.make(binding.getRoot(), "Restore successful!", Snackbar.LENGTH_LONG).show();
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).recreate();
                                }
                            } else {
                                Snackbar.make(binding.getRoot(), "No cloud backup found.", Snackbar.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(e -> 
                            Snackbar.make(binding.getRoot(), "Restore failed: " + e.getMessage(), Snackbar.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void updateCloudUi(GoogleAccountManager accountManager, CloudBackupSettings cloudSettings) {
        boolean isPremium = com.robinzon.medicationwizard.AppConfig.IS_PREMIUM;
        
        // Check real Google Sign In state
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(requireContext());
        boolean signedIn = googleAccount != null;
        
        Logger.log("SettingsFragment", "Updating Cloud UI: Premium=" + isPremium + ", SignedIn=" + signedIn);

        if (!isPremium) {
            binding.containerCloudSettings.setVisibility(View.GONE);
            binding.btnSignOut.setVisibility(View.GONE);
            binding.btnGoogleSigninAction.setVisibility(View.VISIBLE);
            binding.txtAccountName.setText("Cloud Backup");
            binding.txtAccountEmail.setText("Premium only feature");
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
            binding.txtAccountName.setText("Cloud Backup");
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
                        Snackbar.make(binding.getRoot(), "Invalid code", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateNotificationStatus() {
        boolean isGranted = NotificationManager.getInstance(requireActivity()).hasPermission();
        binding.switchNotifications.setChecked(isGranted);
        binding.containerAlertDetails.setVisibility(isGranted ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNotificationStatus();
        
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