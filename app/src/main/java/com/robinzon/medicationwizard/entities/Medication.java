package com.robinzon.medicationwizard.entities;

import com.robinzon.medicationwizard.utils.Validator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Medication {
    public static final class JsonKeys {
        public static final String JSON_KEY_COMMERCIAL_NAME = "mCommercialName";
        public static final String JSON_KEY_ACTIVE_INGREDIENTS = "mActiveIngredients";
        public static final String JSON_KEY_FORM = "mForm";
        public static final String JSON_KEY_STRENGTH = "mStrength";
        public static final String JSON_KEY_MEDICAL_CONDITION = "mMedicalCondition";
        public static final String JSON_KEY_DAILY_SCHEDULE = "mDailySchedule";
        public static final String JSON_KEY_DAYS_CYCLE = "mDaysCycle";
        public static final String JSON_KEY_AMOUNT_LEFT = "mAmountLeft";
        public static final String JSON_KEY_INSTRUCTIONS = "mInstruction";
    }
    private final String mCommercialName;
    private final List<ActiveIngredient> mActiveIngredients;
    private Form mForm;
    private float mStrength;
    private String mMedicalCondition;
    private List<Long> mDailySchedule;
    private int mDaysCycle;
    private int mAmountLeft;
    private Instructions mInstruction;

    public Medication(String commercialName, List<ActiveIngredient> activeIngredients) {
        if(Validator.isValidString(commercialName) && Validator.isValidCollection(activeIngredients)) {
            this.mCommercialName = commercialName;
            this.mActiveIngredients = activeIngredients;
        } else {
            this.mCommercialName = null;
            this.mActiveIngredients = null;
        }
    }

    public String getCommercialName() {
        return mCommercialName;
    }



    public List<ActiveIngredient> getActiveIngredients() {
        return mActiveIngredients;
    }

    public Form getForm() {
        return mForm;
    }

    public void setForm(Form mForm) {
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

    public int getDaysCycle() {
        return mDaysCycle;
    }

    public void setDaysCycle(int mDaysSchedule) {
        this.mDaysCycle = mDaysSchedule;
    }

    public int getAmountLeft() {
        return mAmountLeft;
    }

    public void setAmountLeft(int mAmountLeft) {
        this.mAmountLeft = mAmountLeft;
    }

    public Instructions getInstruction() {
        return mInstruction;
    }

    public void setInstruction(Instructions mInstruction) {
        this.mInstruction = mInstruction;
    }

    @Override
    public String toString() {
        return "Medication{" +
                "mCommercialName='" + mCommercialName + '\'' +
                ", mActiveIngredients=" + mActiveIngredients +
                ", mForm=" + mForm +
                ", mStrength=" + mStrength +
                ", mMedicalCondition='" + mMedicalCondition + '\'' +
                ", mDailySchedule=" + mDailySchedule +
                ", mDaysSchedule=" + mDaysCycle +
                ", mAmountLeft=" + mAmountLeft +
                ", mInstruction=" + mInstruction +
                '}';
    }

    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        try {
            json.put(JsonKeys.JSON_KEY_COMMERCIAL_NAME, mCommercialName);
            json.put(JsonKeys.JSON_KEY_ACTIVE_INGREDIENTS , getActiveIngredientsAsJsonArray());
            json.put(JsonKeys.JSON_KEY_FORM, mForm.name());
            json.put(JsonKeys.JSON_KEY_STRENGTH, mStrength);
            json.put(JsonKeys.JSON_KEY_MEDICAL_CONDITION, mMedicalCondition);
            json.put(JsonKeys.JSON_KEY_DAILY_SCHEDULE, getDailyScheduleAsJsonArray());
            json.put(JsonKeys.JSON_KEY_DAYS_CYCLE, mDaysCycle);
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

    private JSONArray getActiveIngredientsAsJsonArray(){
        final JSONArray jsonArray = new JSONArray();
        for (ActiveIngredient activeIngredient : mActiveIngredients) {
            jsonArray.put(activeIngredient.toJsonObject());
        }
        return jsonArray;
    }

    private String getClassNameForLogs(){
        return "{Medication}";
    }
}
