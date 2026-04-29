package com.robinzon.medicationwizard.entities;

import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.NonNull;

import com.robinzon.medicationwizard.MedicationWizardSuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.SimpleDayTime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;

public class Medication extends MedicationWizardSuper {


    private HashSet<SimpleDayTime> mTimesADay;
    private float mAmount;
    private int mFrequency;
    private String mCommercialName;
    private EForm mForm;
    private float mStrength;
    private String mMedicalCondition;
    private List<Long> mDailySchedule;
    private int mAmountLeft;
    private EInstructions mInstruction;
    private EMeasurementUnit mMeasurementUnit;
    public Medication() {

    }
    public Medication(String commercialName) {
        if (!TextUtils.isEmpty(commercialName)) {
            this.mCommercialName = commercialName;
        } else {
            mCommercialName = null;
        }
    }

    public int getDailyFrequency() {
        return this.mFrequency;
    }

    public void setDailyFrequency(final int frequency) {
        this.mFrequency = frequency;
    }

    /**
     * Validates the current state of the medication to ensure all required fields are correctly populated.
     * <p>
     * A medication is considered valid only if all of the following conditions are met:
     * <ul>
     * <li>The commercial name is not null or empty.</li>
     * <li>The amount to take per dose is greater than 0.</li>
     * <li>The physical form of the medication (e.g., Pill, Drop, Inhaler) has been specified.</li>
     * <li>The frequency (doses per day) is greater than 0.</li>
     * </ul>
     *
     * @return {@code true} if the medication contains all required, valid data; {@code false} otherwise.
     */
    public boolean isValid() {
        return !TextUtils.isEmpty(mCommercialName) && mAmount > 0 && mForm != null && mFrequency > 0;
    }

    public void addToMedicationList() {
    }

    public void addTimeStampsForDay(@NonNull final SparseArray<View> mTimeButtons) {
        if (mTimeButtons.size() == 0) {
            mTimesADay = null;
            return;
        }
        mTimesADay = new HashSet<>(mTimeButtons.size());
        for (int i = 0; i < mTimeButtons.size(); i++) {
            final View view = mTimeButtons.valueAt(i);
            try {
                mTimesADay.add((SimpleDayTime) view.getTag());
            } catch (ClassCastException | NullPointerException e) {
                mTimesADay = null;
                break;
            }
            // Update or manipulate the view...
        }


    }

    public String getCommercialName() {
        return mCommercialName;
    }

    public void setCommercialName(@NonNull final String commercialName) {
        this.mCommercialName = commercialName;
    }

    public float getAmount() {
        return mAmount;
    }

    public void setAmount(final float amount) {
        this.mAmount = amount;
    }

    public EForm getForm() {
        return mForm;
    }

    public void setForm(EForm mForm) {
        this.mForm = mForm;
    }

    public float getStrength() {
        return mStrength;
    }

    public void setStrength(float mStrength) {
        this.mStrength = mStrength;
    }

    public String getMedicalCondition() {
        return mMedicalCondition;
    }

    public void setMedicalCondition(String mMedicalCondition) {
        this.mMedicalCondition = mMedicalCondition;
    }

    public List<Long> getDailySchedule() {
        return mDailySchedule;
    }

    public void setDailySchedule(List<Long> mDailySchedule) {
        this.mDailySchedule = mDailySchedule;
    }

    public EMeasurementUnit getMeasurementUnit() {
        return mMeasurementUnit;
    }

    public void setMeasurementUnit(EMeasurementUnit mMeasurementUnit) {
        this.mMeasurementUnit = mMeasurementUnit;
    }

    public int getAmountLeft() {
        return mAmountLeft;
    }

    public void setAmountLeft(int mAmountLeft) {
        this.mAmountLeft = mAmountLeft;
    }

    public HashSet<SimpleDayTime> getTimesADay() {
        return mTimesADay;
    }

    public EInstructions getInstruction() {
        return mInstruction;
    }

    public void setInstruction(EInstructions mInstruction) {
        this.mInstruction = mInstruction;
    }

    @Override
    public String toString() {
        return "Medication{" + "mCommercialName='" + mCommercialName + '\'' + ", mForm=" + mForm + ", mStrength=" + mStrength + ", mMedicalCondition='" + mMedicalCondition + '\'' + ", mDailySchedule=" + mDailySchedule + ", mAmountLeft=" + mAmountLeft + ", mInstruction=" + mInstruction + '}';
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(JsonKeys.COMMERCIAL_NAME, mCommercialName);
            json.put(JsonKeys.FORM, mForm.name());
            json.put(JsonKeys.FREQUENCY, mFrequency);
            json.put(JsonKeys.AMOUNT, mAmount);
            json.put(JsonKeys.STRENGTH, mStrength);
            json.put(JsonKeys.MEDICAL_CONDITION, mMedicalCondition);
            json.put(JsonKeys.DAILY_SCHEDULE, getDailyScheduleAsJsonArray());
            json.put(JsonKeys.AMOUNT_LEFT, mAmountLeft);
            json.put(JsonKeys.INSTRUCTIONS, mInstruction.getDescription());
        } catch (JSONException e) {
            Logger.log(Me(), "Tried to convert myself to JSONObject but an exception happen");
            return null;
        }
        return json;
    }

    private JSONArray getDailyScheduleAsJsonArray() {
        final JSONArray jsonArray = new JSONArray();
        int counter = 1;
        for (Long timeOfDay : mDailySchedule) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(String.valueOf(counter), timeOfDay);
                counter++;
            } catch (JSONException e) {
                Logger.log(Me(), "Error while trying to getDailyScheduleAsJsonArray");
            }
        }
        return jsonArray;
    }

    private void invalidate() {

    }

    public static final class JsonKeys {
        //TODO add json keys to fields added
        public static String TIMES_IN_DAY = "mTimesADay";
        public static final String AMOUNT = "mAmount";
        public static final String FREQUENCY = "mFrequency";
        public static final String COMMERCIAL_NAME = "mCommercialName";
        public static final String FORM = "mForm";
        public static final String STRENGTH = "mStrength";
        public static final String MEDICAL_CONDITION = "mMedicalCondition";
        public static final String DAILY_SCHEDULE = "mDailySchedule";
        public static final String AMOUNT_LEFT = "mAmountLeft";
        public static final String INSTRUCTIONS = "mInstruction";
        public static final String MEASUREMENT_UNIT = "mMeasurementUnit";


    }
}
