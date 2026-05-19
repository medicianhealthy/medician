package com.robinzon.medicationwizard.entities;


import static com.robinzon.medicationwizard.MainActivity.BANNER_HEIGHT_MULTIPLIER;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.robinzon.medicationwizard.ads.admob.AdMobBanner;
import com.robinzon.medicationwizard.utils.Screen;


public class MedicationWizardFragment extends Fragment {

    private static final int ADDITIONAL_FAB_PADDING_DP = 88; // 56dp (FAB) + 16dp (margin) + 16dp (breathing room)


    protected void setPaddingForRecyclerView(@NonNull final View rootView) {
        setPaddingForRecyclerView(rootView, true);
    }

    protected void setPaddingForRecyclerView(@NonNull final View rootView, final boolean withFab) {

        // 1. Calculate Banner height clearance
        int bannerHeightDp = (int) (AdMobBanner.getBannerHeightDP((Activity) rootView.getContext()) * BANNER_HEIGHT_MULTIPLIER);
        
        // 2. Add FAB clearance
        int totalPaddingBottomDp = bannerHeightDp + (withFab ? ADDITIONAL_FAB_PADDING_DP : 0);
        
        int paddingBottomPx = (int) (totalPaddingBottomDp * Screen.getDensity(getResources()));

        // We use getPadding...() for the other sides so we don't accidentally erase them!
        rootView.setPadding(
                rootView.getPaddingLeft(),
                rootView.getPaddingTop(),
                rootView.getPaddingRight(),
                paddingBottomPx
        );
        if (rootView instanceof RecyclerView) {
            // 4. Critically important: allow items to scroll into that padded area
            ((RecyclerView)rootView).setClipToPadding(false);
        }
    }

}
