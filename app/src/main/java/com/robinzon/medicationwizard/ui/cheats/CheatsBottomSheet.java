package com.robinzon.medicationwizard.ui.cheats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

public class CheatsBottomSheet extends BottomSheetDialogFragment {

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
        View btnApply = view.findViewById(R.id.btn_apply);

        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(requireContext());
        
        checkPremium.setChecked(sp.getBoolean(AppConfig.KEY_CHEAT_PREMIUM, AppConfig.IS_PREMIUM));
        checkShowAds.setChecked(sp.getBoolean(AppConfig.KEY_CHEAT_SHOW_ADS, AppConfig.FORCED_ADS_VISIBLE));

        btnApply.setOnClickListener(v -> {
            sp.setBoolean(AppConfig.KEY_CHEAT_PREMIUM, checkPremium.isChecked());
            sp.setBoolean(AppConfig.KEY_CHEAT_SHOW_ADS, checkShowAds.isChecked());
            
            // Persist to the main billing cache as well so it survives process death
            sp.setBoolean("cached_premium_status", checkPremium.isChecked());
            
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
}
