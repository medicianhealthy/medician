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
import java.util.Set;

public class Medication extends MedicationWizardSuper {


    private float mAmount;
    private int mFrequency;

    HashSet<SimpleDayTime> mTimesADay;

    public Medication() {

    }

    public void setDailyFrequency(final int frequency) {
        this.mFrequency = frequency;
    }

    public int getDailyFrequency() {
        return mFrequency;
    }

    public boolean isValid() {
        return !TextUtils.isEmpty(mCommercialName)
                && mAmount >0
                && mForm != null
                && mFrequency > 0;
    }

    public void addToMedicationList() {
    }

    public void addTimeStampsForDay(@NonNull final SparseArray<View> mTimeButtons) {
        if (mTimeButtons.size() == 0){
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

    public static final class JsonKeys {
        //TODO add json keys to fields added
        public static final String JSON_KEY_COMMERCIAL_NAME = "mCommercialName";
        public static final String JSON_KEY_FORM = "mForm";
        public static final String JSON_KEY_STRENGTH = "mStrength";
        public static final String JSON_KEY_MEDICAL_CONDITION = "mMedicalCondition";
        public static final String JSON_KEY_DAILY_SCHEDULE = "mDailySchedule";
        public static final String JSON_KEY_AMOUNT_LEFT = "mAmountLeft";
        public static final String JSON_KEY_INSTRUCTIONS = "mInstruction";
    }


    private String mCommercialName;
    private EForm mForm;
    private float mStrength;
    private String mMedicalCondition;
    private List<Long> mDailySchedule;
    private int mAmountLeft;
    private EInstructions mInstruction;


    private EMeasurementUnit mMeasurementUnit;

    public Medication(String commercialName) {
        if (!TextUtils.isEmpty(commercialName)) {
            this.mCommercialName = commercialName;
        } else {
            mCommercialName = null;
        }
    }

    public String getCommercialName() {
        return mCommercialName;
    }

    public void setCommercialName(@NonNull final String commercialName) {
        this.mCommercialName = commercialName;
    }

    public void setAmount(final float amount) {
        this.mAmount = amount;
    }

    public float getAmount() {
        return mAmount;
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

    public HashSet<SimpleDayTime> getTimesADay() {
        return mTimesADay;
    }

    public void setAmountLeft(int mAmountLeft) {
        this.mAmountLeft = mAmountLeft;
    }

    public EInstructions getInstruction() {
        return mInstruction;
    }

    public void setInstruction(EInstructions mInstruction) {
        this.mInstruction = mInstruction;
    }

    @Override
    public String toString() {
        return "Medication{" +
                "mCommercialName='" + mCommercialName + '\'' +
                ", mForm=" + mForm +
                ", mStrength=" + mStrength +
                ", mMedicalCondition='" + mMedicalCondition + '\'' +
                ", mDailySchedule=" + mDailySchedule +
                ", mAmountLeft=" + mAmountLeft +
                ", mInstruction=" + mInstruction +
                '}';
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(JsonKeys.JSON_KEY_COMMERCIAL_NAME, mCommercialName);
            json.put(JsonKeys.JSON_KEY_FORM, mForm.name());
            json.put(JsonKeys.JSON_KEY_STRENGTH, mStrength);
            json.put(JsonKeys.JSON_KEY_MEDICAL_CONDITION, mMedicalCondition);
            json.put(JsonKeys.JSON_KEY_DAILY_SCHEDULE, getDailyScheduleAsJsonArray());
            json.put(JsonKeys.JSON_KEY_AMOUNT_LEFT, mAmountLeft);
            json.put(JsonKeys.JSON_KEY_INSTRUCTIONS, mInstruction.getDescription());
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
}
