package com.robinzon.medicationwizard.ui.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.databinding.FragmentHomeBinding;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.entities.MedicationWizardFragment;
import com.robinzon.medicationwizard.ui.AddMedicationBottomSheet;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.ArrayList;

public class HomeFragment extends MedicationWizardFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private FragmentHomeBinding mBinding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentHomeBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setPaddingForRecyclerView(mBinding.recyclerView);
        setupSwipeRefresh();
        setupEmptyView();
        refreshData();
        SharedPreferencesManager.getInstance(requireContext()).registerListener(this);
        
        // Ensure FAB is visible if there is data
        final ArrayList<Medication> data = Medication.getSavedMedications(getContext());
        if (!data.isEmpty() && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFabVisible(true);
        }
    }

    private void updateUiState(boolean isEmpty) {
        mBinding.emptyLayout.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mBinding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFabVisible(!isEmpty);
        }
    }

    private void setupSwipeRefresh() {
        mBinding.swipeRefresh.setOnRefreshListener(this::refreshData);
    }

    private void setupEmptyView() {
        mBinding.emptyLayout.btnEmptyAction.setOnClickListener(v -> {
            AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "AddMedBottomSheet");
        });
    }

    private void refreshData() {
        final ArrayList<Medication> data = Medication.getSavedMedications(getContext());

        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        RecyclerView.Adapter<MedicationsAdapter.ViewHolder> customAdapter = new MedicationsAdapter(data);
        mBinding.recyclerView.setAdapter(customAdapter);

        updateUiState(data.isEmpty());
        mBinding.swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Medication.SPK_MEDICATION_LIST.equals(key)) {
            refreshData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SharedPreferencesManager.getInstance(requireContext()).unregisterListener(this);
        mBinding = null;
    }
}
