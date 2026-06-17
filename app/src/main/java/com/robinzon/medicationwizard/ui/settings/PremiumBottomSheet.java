package com.robinzon.medicationwizard.ui.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.billing.BillingManager;

/**
 * A specialized BottomSheet for promoting and handling Premium upgrades.
 * <p>
 * Performance: Uses a custom background drawable and standard Material 3 components 
 * to ensure smooth rendering and a distinct visual hierarchy.
 * </p>
 */
public class PremiumBottomSheet extends BottomSheetDialogFragment {

    /**
     * Standard lifecycle method to define the dialog's visual style.
     * <p>
     * Performance: Disables the default BottomSheet background to allow 
     * our custom rounded corners and stroke to be visible.
     * </p>
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog);
    }

    /**
     * Standard lifecycle method to configure the dialog window.
     * <p>
     * Performance: Ensures the sheet opens fully and adheres to standard 
     * Material 3 layout principles.
     * </p>
     */
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

    /**
     * Inflates the layout for the premium promotion.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_premium, container, false);
    }

    /**
     * Initializes interactive elements and logic for the premium upgrade flow.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_upgrade).setOnClickListener(v -> {
            // Trigger the official Google Play purchase flow.
            if (getActivity() != null) {
                BillingManager.getInstance(requireContext()).launchPurchaseFlow(getActivity());
                dismiss();
            }
        });

        view.findViewById(R.id.btn_maybe_later).setOnClickListener(v -> dismiss());
    }
}
