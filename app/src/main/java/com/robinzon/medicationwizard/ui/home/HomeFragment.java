package com.robinzon.medicationwizard.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.robinzon.medicationwizard.databinding.FragmentHomeBinding;
import com.robinzon.medicationwizard.entities.ActiveIngredient;
import com.robinzon.medicationwizard.entities.EForm;
import com.robinzon.medicationwizard.entities.EInstructions;
import com.robinzon.medicationwizard.entities.EMeasurementUnit;
import com.robinzon.medicationwizard.entities.Medication;

import java.util.ArrayList;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding mBinding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentHomeBinding.inflate(inflater, container, false);
        final View rootView = mBinding.getRoot();
        final RecyclerView recyclerView = mBinding.recyclerView;

        final ArrayList<Medication> data = new ArrayList<>();
        final Medication medication = new Medication("Cymbalta", new ArrayList<>() {{
            add(new ActiveIngredient("Doluxine", EMeasurementUnit.MILLIGRAM));
        }});
        medication.setForm(EForm.Pill);
        medication.setInstruction(EInstructions.AFTER_EATING);
        medication.setStrength(60F);

        data.add(medication);
        data.add(medication);
        data.add(medication);
        data.add(medication);
        data.add(medication);
        data.add(medication);
        data.add(medication);
        data.add(medication);

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
}