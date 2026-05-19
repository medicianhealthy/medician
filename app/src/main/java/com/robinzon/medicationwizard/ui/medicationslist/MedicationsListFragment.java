package com.robinzon.medicationwizard.ui.medicationslist;

import static com.robinzon.medicationwizard.MainActivity.BANNER_HEIGHT_MULTIPLIER;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.databinding.FragmentMedicationsListBinding;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.entities.MedicationWizardFragment;
import com.robinzon.medicationwizard.ui.AddMedicationBottomSheet;

import java.util.List;

public class MedicationsListFragment extends MedicationWizardFragment {

    private FragmentMedicationsListBinding binding;
    private MedicationsListViewModel viewModel;
    private MedicationsListAdapter adapter;
    private static final int SNACKBAR_DURATION_MS = 5000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MedicationsListViewModel.class);
        binding = FragmentMedicationsListBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSwipeRefresh();
        setupEmptyView();

        viewModel.getMedications().observe(getViewLifecycleOwner(), medications -> {
            boolean isEmpty = medications == null || medications.isEmpty();
            updateUiState(isEmpty);
            if (!isEmpty) {
                adapter.setMedications(medications);
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setFabVisible(true);
                }
            }
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void updateUiState(boolean isEmpty) {
        binding.emptyLayout.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFabVisible(!isEmpty);
        }
    }

    private void setupEmptyView() {
        binding.emptyLayout.btnEmptyAction.setOnClickListener(v -> {
            AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "AddMedBottomSheet");
        });
    }

    private void setupRecyclerView() {
        adapter = new MedicationsListAdapter();
        adapter.setOnMedicationActionListener(new MedicationsListAdapter.OnMedicationActionListener() {
            @Override
            public void onDelete(Medication medication, int position) {
                showDeleteSnackbar(medication, position);
            }

            @Override
            public void onEdit(Medication medication) {
                AddMedicationBottomSheet bottomSheet = AddMedicationBottomSheet.newInstance(medication);
                bottomSheet.show(getChildFragmentManager(), "EditMedBottomSheet");
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
        setPaddingForRecyclerView(binding.recyclerView);
    }

    private void showDeleteSnackbar(Medication medication, int position) {
        String message = medication.getCommercialName() + " was deleted";

        // 1. Delete from storage immediately to stay in sync with refreshes
        Medication.deleteMedication(requireContext(), medication.getId());

        // 2. Update the UI
        adapter.removeItem(position);
        updateUiState(adapter.getItemCount() == 0);

        Snackbar.make(binding.getRoot(), message, SNACKBAR_DURATION_MS)
                .setAction("Undo", v -> {
                    // 3. Restore to storage if undone
                    medication.addToMedicationList(requireContext());
                    // 4. refreshMedications will be triggered automatically by our listener in VM
                })
                .show();
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refreshMedications());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}