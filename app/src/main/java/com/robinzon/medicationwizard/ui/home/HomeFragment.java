package com.robinzon.medicationwizard.ui.home;

import static com.robinzon.medicationwizard.MainActivity.BANNER_HEIGHT_MULTIPLIER;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.robinzon.medicationwizard.ads.admob.AdMobBanner;
import com.robinzon.medicationwizard.databinding.FragmentHomeBinding;
import com.robinzon.medicationwizard.entities.EForm;
import com.robinzon.medicationwizard.entities.EInstructions;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.utils.Screen;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding mBinding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentHomeBinding.inflate(inflater, container, false);
        final View rootView = mBinding.getRoot();
        final RecyclerView recyclerView = mBinding.recyclerView;
        setPaddingForRecyclerView(recyclerView);

        final ArrayList<Medication> data = new ArrayList<>();
        final Medication medication = new Medication("Cymbalta");
        medication.setForm(EForm.Pill);
        medication.setInstruction(EInstructions.AFTER_EATING);
        medication.setStrength(60F);
        data.add(medication);

        final Medication medication2 = new Medication("Lisinopril");
        medication2.setForm(EForm.Pill);
        medication2.setInstruction(EInstructions.BEFORE_EATING);
        medication2.setStrength(10F);
        data.add(medication2);

        final Medication medication3 = new Medication("Metformin");
        medication3.setForm(EForm.Pill);
        medication3.setInstruction(EInstructions.WHILE_EATING);
        medication3.setStrength(500F);
        data.add(medication3);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireActivity().getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        RecyclerView.Adapter<MedicationsAdapter.ViewHolder> customAdapter = new MedicationsAdapter(data);
        recyclerView.setAdapter(customAdapter);

        return rootView;

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private void setPaddingForRecyclerView(@NonNull final RecyclerView recyclerView) {

        // 1. Define your desired bottom padding in DP (e.g., 84dp to clear the ad)
        int paddingBottomDp = (int) (AdMobBanner.getBannerHeightDP((Activity) recyclerView.getContext()) * BANNER_HEIGHT_MULTIPLIER);
        int paddingBottomPx = (int) (paddingBottomDp * Screen.getDensity(getResources()));
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