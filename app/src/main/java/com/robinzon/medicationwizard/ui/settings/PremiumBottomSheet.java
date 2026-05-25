package com.robinzon.medicationwizard.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.R;

public class PremiumBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_premium, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_upgrade).setOnClickListener(v -> {
            // Mock upgrade
            AppConfig.IS_PREMIUM = true;
            dismiss();
        });

        view.findViewById(R.id.btn_maybe_later).setOnClickListener(v -> dismiss());
    }
}
