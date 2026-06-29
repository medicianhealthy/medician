package com.robinzon.medicationwizard.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;

/**
 * Base BottomSheet class for the project that ensures the Ad Banner 
 * remains visible even when the modal is open.
 */
public class MedicationWizardBottomSheet extends BottomSheetDialogFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Find the ad container in the bottom sheet layout
        FrameLayout adContainer = view.findViewById(R.id.ad_container);
        if (adContainer != null && getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            
            // Only show and attach if ads should be visible
            if (!AppConfig.isPremium(requireContext()) || AppConfig.FORCED_ADS_VISIBLE) {
                adContainer.setVisibility(View.VISIBLE);
                main.getAdsManager().attachBannerToContainer(adContainer);
            } else {
                adContainer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            main.getAdsManager().restoreBannerToDefault();
        }
    }
}
