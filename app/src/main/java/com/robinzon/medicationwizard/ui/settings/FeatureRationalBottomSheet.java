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
import android.content.SharedPreferences;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.billing.BillingManager;
import com.robinzon.medicationwizard.managers.FeaturePassManager;
import com.robinzon.medicationwizard.managers.MagicManager;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.notifications.NotificationManager;
import com.robinzon.medicationwizard.ui.MedicationWizardBottomSheet;

/**
 * A contextual dialog to prompt users to unlock specific premium features
 * by watching a Rewarded Video.
 */
public class FeatureRationalBottomSheet extends MedicationWizardBottomSheet {

    private static final String ARG_FEATURE_TYPE = "arg_feature_type";
    private AppConfig.FeaturePassType featureType;
    private SharedPreferences.OnSharedPreferenceChangeListener balanceListener;

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
        updateMagicBalance(view);

        balanceListener = (sharedPreferences, key) -> {
            if (AppConfig.KEY_MAGIC_BALANCE.equals(key)) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateMagicBalance(view));
                }
            }
        };
        SharedPreferencesManager.getInstance(requireContext()).registerListener(balanceListener);

        view.findViewById(R.id.btn_watch_video).setVisibility(featureType == AppConfig.FeaturePassType.AD_FREE ? View.GONE : View.VISIBLE);

        view.findViewById(R.id.btn_watch_video).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity main = (MainActivity) getActivity();
                main.getAdsManager().showRewarded(status -> {
                    if (status == AdsManager.RewardedStatus.SUCCESS) {
                        FeaturePassManager.grantPass(requireContext(), featureType);
                        onFeatureUnlocked();
                    } else if (status == AdsManager.RewardedStatus.DISMISSED_EARLY) {
                        showErrorDialog(getString(R.string.reward_ad_dismissed_early_title),
                                getString(R.string.reward_ad_dismissed_early_message, getFeatureBenefitText()));
                    } else {
                        showErrorDialog(getString(R.string.reward_ad_not_ready_title),
                                getString(R.string.reward_ad_not_ready_dialog));
                    }
                });
            }
        });

        view.findViewById(R.id.btn_pay_magic_pass).setOnClickListener(v -> {
            int cost = featureType == AppConfig.FeaturePassType.AD_FREE ? AppConfig.MAGIC_COST_AD_FREE_1H : AppConfig.MAGIC_COST_PASS_1H;
            if (MagicManager.getInstance(requireContext()).spendMagics(cost)) {
                FeaturePassManager.grantPass(requireContext(), featureType);
                onFeatureUnlocked();
            } else {
                Toast.makeText(requireContext(), R.string.magic_insufficient_toast, Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btn_pay_magic_forever).setOnClickListener(v -> {
            if (MagicManager.getInstance(requireContext()).spendMagics(AppConfig.MAGIC_COST_PERMANENT)) {
                FeaturePassManager.grantPermanentPass(requireContext(), featureType);
                onFeatureUnlocked();
            } else {
                Toast.makeText(requireContext(), R.string.magic_insufficient_toast, Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btn_go_premium).setOnClickListener(v -> {
            BillingManager.getInstance(requireContext()).launchPurchaseFlow(requireActivity());
            dismiss();
        });

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (balanceListener != null) {
            SharedPreferencesManager.getInstance(requireContext()).unregisterListener(balanceListener);
        }
    }

    private void updateMagicBalance(View view) {
        TextView balanceView = view.findViewById(R.id.txt_magic_balance);
        int balance = MagicManager.getInstance(requireContext()).getMagicBalance();
        balanceView.setText(getString(R.string.magic_balance_format, balance));

        com.google.android.material.button.MaterialButton passBtn = view.findViewById(R.id.btn_pay_magic_pass);
        if (featureType == AppConfig.FeaturePassType.AD_FREE) {
            passBtn.setText(R.string.magic_spend_ad_free_btn);
        } else {
            passBtn.setText(R.string.magic_spend_pass_btn);
        }

        // Hide "Forever" button if already permanently unlocked
        boolean permanentlyUnlocked = SharedPreferencesManager.getInstance(requireContext())
                .getBoolean(AppConfig.KEY_PERMANENT_PASS_PREFIX + featureType.name(), false);
        
        View foreverBtn = view.findViewById(R.id.btn_pay_magic_forever);
        foreverBtn.setVisibility(permanentlyUnlocked ? View.GONE : View.VISIBLE);
    }

    private void onFeatureUnlocked() {
        // Notify listener to perform the action
        Bundle result = new Bundle();
        result.putString("feature_type", featureType.name());
        getParentFragmentManager().setFragmentResult("feature_unlocked", result);

        Toast.makeText(requireContext(), getString(R.string.feature_unlocked_toast, getFeatureName()), Toast.LENGTH_SHORT).show();

        // Check if notifications are disabled and remind the user
        if (getActivity() instanceof MainActivity main) {
            if (!NotificationManager.getInstance(main).hasPermission()) {
                com.robinzon.medicationwizard.ui.CustomMaterialDialog permDialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(main);
                permDialog.setTitle(getString(R.string.notification_permission_reminder_title));
                permDialog.setMessage(getString(R.string.notification_permission_reminder_message));
                permDialog.setPositiveButton(getString(R.string.button_ok), null);
                permDialog.show();
            }
        }

        dismiss();
    }

    private void showErrorDialog(String title, String message) {
        com.robinzon.medicationwizard.ui.CustomMaterialDialog dialog = new com.robinzon.medicationwizard.ui.CustomMaterialDialog(requireContext());
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setPositiveButton(getString(R.string.button_ok), null);
        dialog.show();
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
            case PHOTO -> {
                iconView.setImageResource(android.R.drawable.ic_menu_camera);
                descView.setText(R.string.photo_feature_rational);
            }
            case AD_FREE -> {
                iconView.setImageResource(R.drawable.ic_ad_badge);
                descView.setText(R.string.premium_rational_ad_free_msg);
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
            case PHOTO -> getString(R.string.btn_take_photo);
            case AD_FREE -> getString(R.string.premium_benefit_ad_free_title);
        };
    }

    private String getFeatureBenefitText() {
        return switch (featureType) {
            case THEME -> getString(R.string.benefit_theme);
            case SUPPORT -> getString(R.string.benefit_support);
            case BACKUP ->
                    getString(AppConfig.CLOUD_BACKUP_ENABLED ? R.string.benefit_backup : R.string.benefit_backup_local);
            case BYPASS_VOLUME -> getString(R.string.benefit_bypass);
            case QUIET_HOURS -> getString(R.string.benefit_quiet_hours);
            case VIBRATION -> getString(R.string.benefit_vibration);
            case STICKY_NOTIF -> getString(R.string.benefit_sticky);
            case DOSE_WINDOW -> getString(R.string.benefit_dose_window);
            case PHOTO -> getString(R.string.benefit_photo);
            case AD_FREE -> getString(R.string.benefit_ad_free);
        };
    }
}
