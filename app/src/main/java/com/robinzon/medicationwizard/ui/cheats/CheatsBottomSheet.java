package com.robinzon.medicationwizard.ui.cheats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager;
import com.robinzon.medicationwizard.ui.MedicationWizardBottomSheet;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

public class CheatsBottomSheet extends MedicationWizardBottomSheet {

    private final android.os.Handler updateHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable updateRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_cheats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialCheckBox checkPremium = view.findViewById(R.id.check_premium);
        MaterialCheckBox checkShowAds = view.findViewById(R.id.check_show_ads);
        TextView txtConfigInfo = view.findViewById(R.id.txt_config_info);
        View btnApply = view.findViewById(R.id.btn_apply);

        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(requireContext());
        
        checkPremium.setChecked(prefs.getBoolean(AppConfig.KEY_CHEAT_PREMIUM, AppConfig.IS_PREMIUM));
        checkShowAds.setChecked(prefs.getBoolean(AppConfig.KEY_CHEAT_SHOW_ADS, AppConfig.FORCED_ADS_VISIBLE));

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    setupConfigInfo(txtConfigInfo);
                    updateHandler.postDelayed(this, 2500); // Refresh every 2.5 seconds
                }
            }
        };
        updateHandler.post(updateRunnable);

        btnApply.setOnClickListener(v -> {
            prefs.setBoolean(AppConfig.KEY_CHEAT_PREMIUM, checkPremium.isChecked());
            prefs.setBoolean(AppConfig.KEY_CHEAT_SHOW_ADS, checkShowAds.isChecked());
            
            // Persist to the main billing cache as well so it survives process death
            prefs.setBoolean("cached_premium_status", checkPremium.isChecked());
            
            AppConfig.IS_PREMIUM = checkPremium.isChecked();
            AppConfig.FORCED_ADS_VISIBLE = checkShowAds.isChecked();

            // Perform a clean restart of the activity
            android.content.Intent intent = requireActivity().getIntent();
            requireActivity().finish();
            requireActivity().overridePendingTransition(0, 0);
            startActivity(intent);
            requireActivity().overridePendingTransition(0, 0);

            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        updateHandler.removeCallbacks(updateRunnable);
    }

    private void setupConfigInfo(TextView textView) {
        RemoteConfigManager remoteConfigManager = RemoteConfigManager.getInstance();
        StringBuilder builder = new StringBuilder();
        java.util.Locale locale = java.util.Locale.getDefault();
        
        builder.append("--- Remote Config Defaults/Server ---\n");
        builder.append("Int. Cooldown: ").append(remoteConfigManager.getAdInterstitialCoolDownSeconds()).append("s\n");
        builder.append("Min Sessions (Int): ").append(remoteConfigManager.getMinSessionsForInterstitial()).append("\n");
        builder.append("Min Sessions (AppOpen): ").append(remoteConfigManager.getMinSessionsAppOpen()).append("\n");
        builder.append("Min Usage (AppOpen): ").append(remoteConfigManager.getMinAppTimeAppOpenMins()).append("m\n");
        builder.append("Min Usage (Int): ").append(remoteConfigManager.getMinAppTimeForInterstitialMins()).append("m\n");
        builder.append("Min Usage (Banner): ").append(remoteConfigManager.getMinAppTimeForBannerMins()).append("m\n");
        builder.append("Magic Pass Duration: ").append(remoteConfigManager.getMagicPassDurationHours()).append("h\n");
        builder.append("History Retention: ").append(remoteConfigManager.getHistoryRetentionDays()).append("d\n");
        builder.append("Early Take: ").append(remoteConfigManager.getEarlyTakeThresholdMins()).append("m\n");
        builder.append("Late Take: ").append(remoteConfigManager.getLateTakeThresholdMins()).append("m\n");
        builder.append("Actions per Inter: ").append(remoteConfigManager.getActionsPerInterstitial()).append("\n\n");

        builder.append("--- Live Usage Statistics ---\n");
        builder.append("Session Count: ").append(com.robinzon.medicationwizard.utils.Statisticator.getSessionCount(requireContext())).append("\n");
        builder.append("Actions Counter: ").append(com.robinzon.medicationwizard.utils.Statisticator.getActionsForInterstitialCount(requireContext())).append("\n");
        builder.append("Total Usage: ").append(String.format(locale, "%.2fm", com.robinzon.medicationwizard.utils.Statisticator.getTotalUsageMinutes(requireContext()))).append("\n");
        builder.append("Usage since last FSA: ").append(String.format(locale, "%.2fm", com.robinzon.medicationwizard.utils.Statisticator.getUsageMinutesForAds(requireContext()))).append("\n\n");

        builder.append("--- Feature Passes (Active?) ---\n");
        builder.append("Theme: ").append(AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.THEME)).append("\n");
        builder.append("Support: ").append(AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.SUPPORT)).append("\n");
        builder.append("Backup: ").append(AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.BACKUP)).append("\n");
        builder.append("Quiet Hours: ").append(AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.QUIET_HOURS)).append("\n");
        builder.append("Bypass Vol: ").append(AppConfig.isFeatureUnlocked(requireContext(), AppConfig.FeaturePassType.BYPASS_VOLUME));

        textView.setText(builder.toString());
    }
}
