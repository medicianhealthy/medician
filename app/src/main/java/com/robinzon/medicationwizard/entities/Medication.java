package com.robinzon.medicationwizard.entities;

import android.content.Context;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import com.robinzon.medicationwizard.MedicationWizardSuper;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.utils.SimpleDayTime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Medication extends MedicationWizardSuper implements Comparable<Medication> {


    public static final String SPK_MEDICATION_LIST = "shared_pref_medications_list";
    private SparseArray<SimpleDayTime> mTimesADay;
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

    public void addToMedicationList(final Context context) {
        final JSONObject json = toJson();
        if (json == null) return;
        JSONArray existingMeds = SharedPreferencesManager.getInstance(context).getJsonArray(SPK_MEDICATION_LIST, null);
        if (existingMeds == null) {
            existingMeds = new JSONArray();
        }
        existingMeds.put(json);
        SharedPreferencesManager.getInstance(context).setJsonArray(SPK_MEDICATION_LIST, existingMeds);
    }

    public static ArrayList<Medication> getSavedMedications(Context context) {
        ArrayList<Medication> medications = new ArrayList<>();
        JSONArray jsonArray = SharedPreferencesManager.getInstance(context).getJsonArray(SPK_MEDICATION_LIST, null);
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    Medication med = fromJson(jsonArray.getJSONObject(i));
                    if (med != null) {
                        medications.add(med);
                    }
                } catch (JSONException ignored) {
                }
            }
        }
        Collections.sort(medications);
        return medications;
    }

    public static Medication fromJson(JSONObject json) {
        if (json == null) return null;
        Medication med = new Medication();
        try {
            med.mCommercialName = json.optString(JsonKeys.COMMERCIAL_NAME);
            med.mForm = EForm.valueOf(json.optString(JsonKeys.FORM, EForm.Pill.name()));
            med.mFrequency = json.optInt(JsonKeys.FREQUENCY);
            med.mAmount = (float) json.optDouble(JsonKeys.AMOUNT);
            med.mStrength = (float) json.optDouble(JsonKeys.STRENGTH);
            med.mMedicalCondition = json.optString(JsonKeys.MEDICAL_CONDITION);
            med.mAmountLeft = json.optInt(JsonKeys.AMOUNT_LEFT);
            if (json.has(JsonKeys.INSTRUCTIONS)) {
                med.mInstruction = EInstructions.valueOf(json.getString(JsonKeys.INSTRUCTIONS));
            }
            if (json.has(JsonKeys.MEASUREMENT_UNIT)) {
                med.mMeasurementUnit = EMeasurementUnit.valueOf(json.getString(JsonKeys.MEASUREMENT_UNIT));
            }
            if (json.has(JsonKeys.TIMES_IN_DAY)) {
                JSONArray timesArray = json.getJSONArray(JsonKeys.TIMES_IN_DAY);
                SparseArray<SimpleDayTime> times = new SparseArray<>();
                for (int i = 0; i < timesArray.length(); i++) {
                    JSONObject item = timesArray.getJSONObject(i);
                    int key = item.getInt("key");
                    SimpleDayTime value = SimpleDayTime.fromJson(item.get("value"));
                    if (value != null) {
                        times.put(key, value);
                    }
                }
                med.mTimesADay = times;
            }
        } catch (Exception e) {
            Logger.log("Medication", "Error deserializing medication: " + e.getMessage());
            return null;
        }
        return med;
    }

    public void addTimeStampsForDay(@NonNull final SparseArray<SimpleDayTime> simpleDayTimeSparseArray) {
        if (simpleDayTimeSparseArray.size() == 0) {
            mTimesADay = null;
            return;
        }
        mTimesADay = new SparseArray<>(simpleDayTimeSparseArray.size());
        for (int i = 0; i < simpleDayTimeSparseArray.size(); i++) {
            int key = simpleDayTimeSparseArray.keyAt(i);
            SimpleDayTime value = simpleDayTimeSparseArray.valueAt(i);
            mTimesADay.put(key, new SimpleDayTime(value));
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

    public SparseArray<SimpleDayTime> getTimesADay() {
        return mTimesADay;
    }

    public EInstructions getInstruction() {
        return mInstruction;
    }

    public void setInstruction(EInstructions mInstruction) {
        this.mInstruction = mInstruction;
    }

    @Override
    public int compareTo(@NonNull Medication other) {
        if (this.mCommercialName == null && other.mCommercialName == null) return 0;
        if (this.mCommercialName == null) return 1;
        if (other.mCommercialName == null) return -1;
        return this.mCommercialName.compareToIgnoreCase(other.mCommercialName);
    }

    @Override
    public String toString() {
        return "Medication{" +
                "mTimesADay=" + mTimesADay +
                ", mAmount=" + mAmount +
                ", mFrequency=" + mFrequency +
                ", mCommercialName='" + mCommercialName + '\'' +
                ", mForm=" + mForm +
                ", mStrength=" + mStrength +
                ", mMedicalCondition='" + mMedicalCondition + '\'' +
                ", mDailySchedule=" + mDailySchedule +
                ", mAmountLeft=" + mAmountLeft +
                ", mInstruction=" + mInstruction +
                ", mMeasurementUnit=" + mMeasurementUnit +
                '}';
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
            json.put(JsonKeys.AMOUNT_LEFT, mAmountLeft);
            if (mInstruction != null) json.put(JsonKeys.INSTRUCTIONS, mInstruction.name());
            if (mMeasurementUnit != null) json.put(JsonKeys.MEASUREMENT_UNIT, mMeasurementUnit.name());
            if (mDailySchedule != null) json.put(JsonKeys.DAILY_SCHEDULE, getDailyScheduleAsJsonArray());
            json.put(JsonKeys.TIMES_IN_DAY, getTimesADayAsJsonArray());
        } catch (JSONException e) {
            Logger.log(Me(), "Tried to convert myself to JSONObject but an exception happen");
            return null;
        }
        return json;
    }

    private JSONArray getTimesADayAsJsonArray() {
        final JSONArray jsonArray = new JSONArray();
        if (mTimesADay == null) return jsonArray;
        for (int i = 0; i < mTimesADay.size(); i++) {
            try {
                JSONObject item = new JSONObject();
                item.put("key", mTimesADay.keyAt(i));
                item.put("value", mTimesADay.valueAt(i).toJson());
                jsonArray.put(item);
            } catch (JSONException e) {
                Logger.log(Me(), "Error while trying to getTimesADayAsJsonArray");
            }
        }
        return jsonArray;
    }

    private JSONArray getDailyScheduleAsJsonArray() {
        final JSONArray jsonArray = new JSONArray();
        if (mDailySchedule == null) return jsonArray;
        int counter = 1;
        for (Long timeOfDay : mDailySchedule) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(String.valueOf(counter), timeOfDay);
                counter++;
                jsonArray.put(jsonObject);
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
