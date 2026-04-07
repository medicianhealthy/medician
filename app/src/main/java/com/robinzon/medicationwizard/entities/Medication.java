package com.robinzon.medicationwizard.entities;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.robinzon.medicationwizard.utils.Validator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Medication {

    public static final class JsonKeys {
        public static final String JSON_KEY_COMMERCIAL_NAME = "mCommercialName";
        public static final String JSON_KEY_FORM = "mForm";
        public static final String JSON_KEY_STRENGTH = "mStrength";
        public static final String JSON_KEY_MEDICAL_CONDITION = "mMedicalCondition";
        public static final String JSON_KEY_DAILY_SCHEDULE = "mDailySchedule";
        public static final String JSON_KEY_AMOUNT_LEFT = "mAmountLeft";
        public static final String JSON_KEY_INSTRUCTIONS = "mInstruction";
    }

    private final String mCommercialName;
    private EForm mForm;
    private float mStrength;
    private String mMedicalCondition;
    private List<Long> mDailySchedule;
    private int mAmountLeft;
    private EInstructions mInstruction;

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



    public int getAmountLeft() {
        return mAmountLeft;
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
            e.printStackTrace();
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
                e.printStackTrace();
            }
        }
        return jsonArray;
    }



    private void invalidate(){

    }
}
