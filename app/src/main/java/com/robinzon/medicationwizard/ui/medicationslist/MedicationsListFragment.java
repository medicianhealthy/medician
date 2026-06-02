package com.robinzon.medicationwizard.ui.medicationslist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.databinding.FragmentMedicationsListBinding;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.entities.MedicationWizardFragment;
import com.robinzon.medicationwizard.ui.AddMedicationBottomSheet;

/**
 * A fragment that displays the master list of all defined medications.
 * <p>
 * This screen provides the management interface for the user's medication "library". 
 * It allows users to view their active drugs, edit dose details, or delete 
 * medications with an "Undo" safety feature.
 * </p>
 * <p>
 * It implements a "Real-time Delete" pattern: items are removed from storage 
 * immediately to maintain list consistency, but can be restored if the 
 * snackbar action is triggered.
 * </p>
 */
public class MedicationsListFragment extends MedicationWizardFragment {

    private FragmentMedicationsListBinding binding;
    private MedicationsListViewModel viewModel;
    private MedicationsListAdapter adapter;
    
    /** Duration for which the "Delete Undo" snackbar is visible. */
    private static final int SNACKBAR_DURATION_MS = 5000;

    /**
     * Initializes data binding and the {@link MedicationsListViewModel}.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MedicationsListViewModel.class);
        binding = FragmentMedicationsListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Sets up the list view components.
     * Observes the global medication list and updates the UI state (Empty vs. List) 
     * automatically when data changes.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSwipeRefresh();
        setupEmptyView();
        
        // Ensure empty state is not hidden by ad banner
        setPaddingForRecyclerView(binding.emptyLayout.emptyStateContainer);

        viewModel.getMedications().observe(getViewLifecycleOwner(), medications -> {
            boolean isEmpty = medications == null || medications.isEmpty();
            updateUiState(isEmpty);
            if (!isEmpty) {
                adapter.setMedications(medications);
                // Ensure FAB is visible for adding new items
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setFabVisible(true);
                }
            }
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    /**
     * Toggles visibility between the empty state Wizard mascot and the medication cards.
     * <p>
     * Performance: This method only updates visibility and starts/stops animations 
     * when the state changes, avoiding redundant UI work.
     * </p>
     *
     * @param isEmpty True if there are no medications defined.
     */
    private void updateUiState(boolean isEmpty) {
        if (binding == null) return;

        binding.emptyLayout.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        if (isEmpty) {
            startEmptyStateAnimations(binding.getRoot());
        } else {
            stopEmptyStateAnimations();
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFabVisible(!isEmpty);
        }
    }

    /**
     * Connects the empty state action button to the "Add Medication" flow.
     * <p>
     * Performance: The listener is set once during view creation.
     * </p>
     */
    private void setupEmptyView() {
        binding.emptyLayout.btnEmptyAction.setOnClickListener(v -> {
            com.robinzon.medicationwizard.utils.Logger.log("MedicationsListFragment", "Empty state action clicked");
            AddMedicationBottomSheet bottomSheet = new AddMedicationBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "AddMedBottomSheet");
        });
    }

    /**
     * Configures the RecyclerView with the {@link MedicationsListAdapter} and 
     * sets up Edit/Delete listeners.
     * <p>
     * Performance: Uses a GridLayoutManager on tablets to utilize screen space effectively.
     * </p>
     */
    private void setupRecyclerView() {
        adapter = new MedicationsListAdapter();
        adapter.setOnMedicationActionListener(new MedicationsListAdapter.OnMedicationActionListener() {
            @Override
            public void onDelete(Medication medication, int position) {
                showDeleteSnackbar(medication, position);
            }

            @Override
            public void onEdit(Medication medication) {
                // Open the bottom sheet in 'Edit Mode'
                AddMedicationBottomSheet bottomSheet = AddMedicationBottomSheet.newInstance(medication);
                bottomSheet.show(getChildFragmentManager(), "EditMedBottomSheet");
            }
        });
        
        int columns = getResources().getInteger(R.integer.medication_grid_columns);
        if (columns > 1) {
            binding.recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columns));
        } else {
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        binding.recyclerView.setAdapter(adapter);
        
        // Ensure the list doesn't get hidden behind the ad banner
        setPaddingForRecyclerView(binding.recyclerView);
    }

    /**
     * Handles the deletion of a medication with an Undo option.
     * <p>
     * Implementation:
     * 1. Deletes the medication from storage immediately to prevent sync issues.
     * 2. Removes the item from the adapter with a standard animation.
     * 3. Displays a snackbar that allows the user to restore the deleted medication.
     * </p>
     *
     * @param medication The medication object to delete.
     * @param position   The adapter position of the item.
     */
    private void showDeleteSnackbar(Medication medication, int position) {
        String message = medication.getCommercialName() + " was deleted";

        Medication.deleteMedication(requireContext(), medication.getId());

        adapter.removeItem(position);
        updateUiState(adapter.getItemCount() == 0);

        Snackbar.make(binding.getRoot(), message, SNACKBAR_DURATION_MS)
                .setAction("Undo", v -> {
                    // Re-add to persistent storage; UI will auto-update via VM listener
                    medication.addToMedicationList(requireContext());
                })
                .show();
    }

    /**
     * Configures the pull-to-refresh behavior.
     * <p>
     * Performance: Triggers a filtered query in the ViewModel to refresh the active list.
     * </p>
     */
    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refreshMedications());
    }

    /**
     * Standard cleanup of view binding to prevent memory leaks.
     * <p>
     * Performance: Automatically stops any running animations via the base class.
     * </p>
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}