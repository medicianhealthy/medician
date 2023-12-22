package com.robinzon.medicationwizard.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.entities.Medication;

import java.util.ArrayList;

public class MedicationsAdapter extends RecyclerView.Adapter<MedicationsAdapter.ViewHolder> {
    private ArrayList<Medication> mLocalDataSet;

    /**
     * Initialize the dataset of the Adapter
     *
     * @param dataSet ArrayList<Medication> containing the data to populate views to be used
     *                by RecyclerView
     */
    public MedicationsAdapter(ArrayList<Medication> dataSet) {
        mLocalDataSet = dataSet;
    }


    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mMedName;
        private final TextView mStrength;
        private final TextView mQuantity;
        private final TextView mTime;
        private final TextView mDirections;
        private final AppCompatButton mSkip;
        private final AppCompatButton mReschduele;
        private final AppCompatButton mTake;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            mMedName = (TextView) view.findViewById(R.id.med_name);
            mStrength = (TextView) view.findViewById(R.id.med_strength);
            mQuantity = (TextView) view.findViewById(R.id.quantity);
            mTime = (TextView) view.findViewById(R.id.time);
            mDirections = (TextView) view.findViewById(R.id.directions);
            mSkip = (AppCompatButton) view.findViewById(R.id.skip_btn);
            mReschduele = (AppCompatButton) view.findViewById(R.id.rescheduele);
            mTake = (AppCompatButton) view.findViewById(R.id.take_btn);
        }

        public TextView getMedName() {
            return mMedName;
        }

        public TextView getStrength() {
            return mStrength;
        }

        public TextView getQuantity() {
            return mQuantity;
        }

        public TextView getTime() {
            return mTime;
        }

        public TextView getDirections() {
            return mDirections;
        }

        public AppCompatButton getSkip() {
            return mSkip;
        }

        public AppCompatButton getReschduele() {
            return mReschduele;
        }

        public AppCompatButton getTake() {
            return mTake;
        }
    }


    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.medications_list_row, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        final Medication medication = mLocalDataSet.get(position);
        viewHolder.getMedName().setText(medication.getCommercialName());
        viewHolder.getStrength().setText(String.valueOf(medication.getStrength()));
        viewHolder.getDirections().setText(medication.getInstruction().getDescription());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mLocalDataSet.size();
    }
}
