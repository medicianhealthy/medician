package com.robinzon.medicationwizard.entities;


import static com.robinzon.medicationwizard.MainActivity.BANNER_HEIGHT_MULTIPLIER;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.robinzon.medicationwizard.ads.admob.AdMobBanner;
import com.robinzon.medicationwizard.utils.Screen;

/**
 * Base fragment class for the Medication Wizard project.
 * <p>
 * This class provides shared layout logic for all feature screens (Home, List, History).
 * Its primary responsibility is managing UI "clearance" to ensure that scrollable 
 * content (like {@link RecyclerView}) is not obscured by permanent UI elements 
 * like the bottom Ad banner or the Floating Action Button (FAB).
 * </p>
 */
public class MedicationWizardFragment extends Fragment {

    /** Standard padding to clear the FAB and its surrounding margins. */
    private static final int ADDITIONAL_FAB_PADDING_DP = 88; // 56dp (FAB) + 16dp (margin) + 16dp (breathing room)


    /**
     * Applies dynamic bottom padding to a view to clear both the Ad banner and the FAB.
     *
     * @param rootView The view to apply padding to (usually a RecyclerView).
     */
    protected void setPaddingForRecyclerView(@NonNull final View rootView) {
        setPaddingForRecyclerView(rootView, true);
    }

    /**
     * Applies dynamic bottom padding to a view, with an option to exclude FAB clearance.
     * <p>
     * Implementation:
     * 1. Fetches the real-time banner height from {@link AdMobBanner}.
     * 2. Calculates total DP clearance needed based on the 'withFab' flag.
     * 3. Converts DP to Pixels based on device density.
     * 4. Updates the view's bottom padding while preserving existing horizontal/top padding.
     * 5. If the view is a {@link RecyclerView}, it automatically disables 'clipToPadding'.
     * </p>
     *
     * @param rootView The view to apply padding to.
     * @param withFab  True if the screen features a Floating Action Button.
     */
    protected void setPaddingForRecyclerView(@NonNull final View rootView, final boolean withFab) {

        // 1. Calculate Banner height clearance
        int bannerHeightDp = (int) (AdMobBanner.getBannerHeightDP((Activity) rootView.getContext()) * BANNER_HEIGHT_MULTIPLIER);
        
        // 2. Add FAB clearance if requested
        int totalPaddingBottomDp = bannerHeightDp + (withFab ? ADDITIONAL_FAB_PADDING_DP : 0);
        
        int paddingBottomPx = (int) (totalPaddingBottomDp * Screen.getDensity(getResources()));

        // Update padding safely without overwriting other sides
        rootView.setPadding(
                rootView.getPaddingLeft(),
                rootView.getPaddingTop(),
                rootView.getPaddingRight(),
                paddingBottomPx
        );

        if (rootView instanceof RecyclerView) {
            // 4. Critically important: allows items to scroll 'under' the banner/fab and reach the bottom
            ((RecyclerView)rootView).setClipToPadding(false);
        }
    }

}