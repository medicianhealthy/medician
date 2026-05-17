package com.robinzon.medicationwizard.entities;


import static com.robinzon.medicationwizard.MainActivity.BANNER_HEIGHT_MULTIPLIER;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.robinzon.medicationwizard.ads.admob.AdMobBanner;
import com.robinzon.medicationwizard.utils.Screen;


public class MedicationWizardFragment extends Fragment {

    private static final int ADDITIONAL_FAB_PADDING_DP = 88; // 56dp (FAB) + 16dp (margin) + 16dp (breathing room)

    protected void setPaddingForRecyclerView(@NonNull final RecyclerView recyclerView) {

        // 1. Calculate Banner height clearance
        int bannerHeightDp = (int) (AdMobBanner.getBannerHeightDP((Activity) recyclerView.getContext()) * BANNER_HEIGHT_MULTIPLIER);
        
        // 2. Add FAB clearance
        int totalPaddingBottomDp = bannerHeightDp + ADDITIONAL_FAB_PADDING_DP;
        
        int paddingBottomPx = (int) (totalPaddingBottomDp * Screen.getDensity(getResources()));

        // We use getPadding...() for the other sides so we don't accidentally erase them!
        recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(),
                paddingBottomPx
        );
        // 4. Critically important: allow items to scroll into that padded area
        recyclerView.setClipToPadding(false);
    }

}
