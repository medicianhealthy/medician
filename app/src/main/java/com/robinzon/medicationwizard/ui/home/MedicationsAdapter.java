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
    private final ArrayList<Medication> mLocalDataSet;

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
        private final TextView mForm;
        private final AppCompatButton mSkip;
        private final AppCompatButton mReschduele;
        private final AppCompatButton mTake;
        private final android.widget.ImageView mMedIcon;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            mMedName = view.findViewById(R.id.med_name);
            mStrength = view.findViewById(R.id.med_strength);
            mQuantity = view.findViewById(R.id.quantity);
            mTime = view.findViewById(R.id.time);
            mDirections = view.findViewById(R.id.directions);
            mForm = view.findViewById(R.id.form);
            mSkip = view.findViewById(R.id.skip_btn);
            mReschduele = view.findViewById(R.id.rescheduele);
            mTake = view.findViewById(R.id.take_btn);
            mMedIcon = view.findViewById(R.id.med_icon);
        }

        public TextView getForm() {
            return mForm;
        }

        public android.widget.ImageView getMedIcon() {
            return mMedIcon;
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

        // Set icon based on form
        int formIconRes;
        if (medication.getForm() != null) {
            formIconRes = switch (medication.getForm()) {
                case Drops -> R.drawable.ic_med_drops;
                case Injection -> R.drawable.ic_med_injection;
                case Solution -> R.drawable.ic_med_solution;
                case Inhaler -> R.drawable.ic_med_inhaler;
                case Powder -> R.drawable.ic_med_powder;
                case Other -> R.drawable.ic_med_other;
                case Pill -> R.drawable.ic_med_pill;
            };
        } else {
            formIconRes = R.drawable.ic_med_pill;
        }
        viewHolder.getMedIcon().setImageResource(formIconRes);

        // 1. Smart Strength Formatting
        if (medication.getStrength() <= 0) {
            viewHolder.getStrength().setVisibility(View.GONE);
        } else {
            viewHolder.getStrength().setVisibility(View.VISIBLE);
            String strengthStr = medication.getStrength() == (long) medication.getStrength() ?
                    String.valueOf((long) medication.getStrength()) :
                    String.valueOf(medication.getStrength());
            viewHolder.getStrength().setText(strengthStr.concat(" mg"));
        }

        // 2. Populate Quantity and Form
        String amountStr = medication.getAmount() == (long) medication.getAmount() ?
                String.valueOf((long) medication.getAmount()) : String.valueOf(medication.getAmount());
        viewHolder.getQuantity().setText(amountStr);
        
        String formName = medication.getForm() != null ? medication.getForm().name() : "";
        if (medication.getAmount() > 1 && !formName.isEmpty()) {
            formName += "s";
        }
        viewHolder.getForm().setText(formName);

        // 3. Set Time (showing the first scheduled time for this item for now)
        if (medication.getTimesADay() != null && medication.getTimesADay().size() > 0) {
            viewHolder.getTime().setText(medication.getTimesADay().valueAt(0).toString());
        }

        // 4. Smart Directions/Instructions Formatting
        if (medication.getInstruction() == null ||
                medication.getInstruction() == com.robinzon.medicationwizard.entities.EInstructions.DOES_NOT_MATTER) {
            viewHolder.getDirections().setVisibility(View.GONE);
        } else {
            viewHolder.getDirections().setVisibility(View.VISIBLE);
            viewHolder.getDirections().setText(medication.getInstruction().getDescription());
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mLocalDataSet.size();
    }
}
