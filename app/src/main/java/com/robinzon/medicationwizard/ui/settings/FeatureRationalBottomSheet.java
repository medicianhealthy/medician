package com.robinzon.medicationwizard.ui.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.billing.BillingManager;
import com.robinzon.medicationwizard.managers.FeaturePassManager;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.ui.MedicationWizardBottomSheet;

/**
 * A contextual dialog to prompt users to unlock specific premium features 
 * by watching a Rewarded Video.
 */
public class FeatureRationalBottomSheet extends MedicationWizardBottomSheet {

    private static final String ARG_FEATURE_TYPE = "arg_feature_type";
    private AppConfig.FeaturePassType featureType;

    public static FeatureRationalBottomSheet newInstance(AppConfig.FeaturePassType type) {
        FeatureRationalBottomSheet fragment = new FeatureRationalBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_FEATURE_TYPE, type.name());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog);
        if (getArguments() != null) {
            featureType = AppConfig.FeaturePassType.valueOf(getArguments().getString(ARG_FEATURE_TYPE));
        }
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
        return inflater.inflate(R.layout.bottom_sheet_feature_rational, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupContent(view);

        view.findViewById(R.id.btn_watch_video).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity main = (MainActivity) getActivity();
                main.getAdsManager().showRewarded(status -> {
                    if (status == AdsManager.RewardedStatus.SUCCESS) {
                        FeaturePassManager.grantPass(requireContext(), featureType);
                        
                        // Notify listener to perform the action
                        Bundle result = new Bundle();
                        result.putString("feature_type", featureType.name());
                        getParentFragmentManager().setFragmentResult("feature_unlocked", result);
                        
                        Toast.makeText(requireContext(), getString(R.string.feature_unlocked_toast, getFeatureName()), Toast.LENGTH_SHORT).show();
                        
                        // Check if notifications are disabled and remind the user
                        if (!NotificationManager.getInstance(main).hasPermission()) {
                            com.robinzon.medicationwizard.ui.CustomMaterialDialog permDialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(main);
                            permDialog.setTitle(getString(R.string.notification_permission_reminder_title));
                            permDialog.setMessage(getString(R.string.notification_permission_reminder_message));
                            permDialog.setPositiveButton(getString(R.string.button_ok), null);
                            permDialog.show();
                        }

                        dismiss();
                    } else if (status == AdsManager.RewardedStatus.DISMISSED_EARLY) {
                        com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
                        dialog.setTitle(getString(R.string.reward_ad_dismissed_early_title));
                        
                        String benefit = getFeatureBenefitText();
                        dialog.setMessage(getString(R.string.reward_ad_dismissed_early_message, benefit));
                        
                        dialog.setPositiveButton(getString(R.string.button_ok), null);
                        dialog.show();
                    } else {
                        com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
                        dialog.setTitle(getString(R.string.reward_ad_not_ready_title));
                        dialog.setMessage(getString(R.string.reward_ad_not_ready_dialog));
                        dialog.setPositiveButton(getString(R.string.button_ok), null);
                        dialog.show();
                    }
                });
            }
        });

        view.findViewById(R.id.btn_go_premium).setOnClickListener(v -> {
            BillingManager.getInstance(requireContext()).launchPurchaseFlow(requireActivity());
            dismiss();
        });

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
    }

    private void setupContent(View view) {
        ImageView iconView = view.findViewById(R.id.img_feature_icon);
        TextView descView = view.findViewById(R.id.txt_rational_desc);
        View glowView = view.findViewById(R.id.mascot_glow);

        switch (featureType) {
            case THEME -> {
                iconView.setImageResource(R.drawable.ic_palette);
                descView.setText(R.string.premium_rational_theme_msg);
            }
            case SUPPORT -> {
                iconView.setImageResource(R.drawable.ic_help_outline);
                descView.setText(R.string.premium_rational_support_msg);
            }
            case BACKUP -> {
                iconView.setImageResource(R.drawable.ic_cloud_upload);
                descView.setText(AppConfig.CLOUD_BACKUP_ENABLED ? 
                        R.string.premium_rational_backup_msg : 
                        R.string.premium_rational_backup_local_msg);
            }
            case BYPASS_VOLUME -> {
                iconView.setImageResource(R.drawable.ic_volume_up);
                descView.setText(R.string.premium_rational_bypass_msg);
            }
            case QUIET_HOURS -> {
                iconView.setImageResource(R.drawable.ic_nightlight);
                descView.setText(R.string.premium_rational_quiet_hours_msg);
            }
            case VIBRATION -> {
                iconView.setImageResource(R.drawable.ic_vibration);
                descView.setText(R.string.premium_rational_vibration_msg);
            }
            case STICKY_NOTIF -> {
                iconView.setImageResource(R.drawable.ic_push_pin);
                descView.setText(R.string.premium_rational_sticky_msg);
            }
            case DOSE_WINDOW -> {
                iconView.setImageResource(R.drawable.ic_clock);
                descView.setText(R.string.premium_rational_dose_window_msg);
            }
        }
        
        String pkg = requireContext().getPackageName();
        int primaryAttr = getResources().getIdentifier("colorPrimary", "attr", pkg);
        int primary = com.google.android.material.color.MaterialColors.getColor(requireContext(), primaryAttr, android.graphics.Color.BLUE);

        // All features now use consistent primary theme coloring for icons and glows
        iconView.setImageTintList(android.content.res.ColorStateList.valueOf(primary));
        glowView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary));
    }

    private String getFeatureName() {
        return switch (featureType) {
            case THEME -> getString(R.string.settings_theme_title);
            case SUPPORT -> getString(R.string.settings_support_title);
            case BACKUP -> getString(R.string.settings_backup_title);
            case BYPASS_VOLUME -> getString(R.string.settings_bypass_title);
            case QUIET_HOURS -> getString(R.string.settings_quiet_hours_title);
            case VIBRATION -> getString(R.string.settings_vibration_title);
            case STICKY_NOTIF -> getString(R.string.settings_sticky_title);
            case DOSE_WINDOW -> getString(R.string.settings_dose_window_title);
        };
    }

    private String getFeatureBenefitText() {
        return switch (featureType) {
            case THEME -> getString(R.string.benefit_theme);
            case SUPPORT -> getString(R.string.benefit_support);
            case BACKUP -> getString(AppConfig.CLOUD_BACKUP_ENABLED ? R.string.benefit_backup : R.string.benefit_backup_local);
            case BYPASS_VOLUME -> getString(R.string.benefit_bypass);
            case QUIET_HOURS -> getString(R.string.benefit_quiet_hours);
            case VIBRATION -> getString(R.string.benefit_vibration);
            case STICKY_NOTIF -> getString(R.string.benefit_sticky);
            case DOSE_WINDOW -> getString(R.string.benefit_dose_window);
        };
    }
}
