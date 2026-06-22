package com.robinzon.medicationwizard.entities;

import android.content.Context;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.MedicationWizardSuper;
import com.robinzon.medicationwizard.database.AppDatabase;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.reminders.ReminderManager;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;
import com.robinzon.medicationwizard.utils.SimpleDayTime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * The core domain model representing a medication definition.
 * <p>
 * This class stores the persistent configuration for a medication, including its name, 
 * dosage details (amount, strength, form), and scheduling rules (frequency, times of day).
 * It handles the synchronization between the high-level list (stored in {@link SharedPreferencesManager})
 * and the specific scheduled instances (stored in the Room database).
 * </p>
 * <p>
 * Implements {@link Comparable} for alphabetical sorting by commercial name.
 * </p>
 */
public class Medication extends MedicationWizardSuper implements Comparable<Medication> {

    /** SharedPreferences key used to store the list of medication definitions as a JSON array. */
    public static final String SPK_MEDICATION_LIST = "shared_pref_medications_list";
    
    private String mId;
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

    /**
     * Constructs a new medication with a unique random UUID.
     */
    public Medication() {
        this.mId = UUID.randomUUID().toString();
    }

    /**
     * Constructs a medication with a specific commercial name.
     *
     * @param commercialName The brand or generic name of the drug.
     */
    public Medication(String commercialName) {
        this();
        if (!TextUtils.isEmpty(commercialName)) {
            this.mCommercialName = commercialName;
        } else {
            mCommercialName = null;
        }
    }

    /**
     * @return Number of doses to be taken per day.
     */
    public int getDailyFrequency() {
        return this.mFrequency;
    }

    /**
     * @param frequency Number of doses per day.
     */
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

    /**
     * Saves this medication to persistent storage and schedules future doses.
     * <p>
     * Operation:
     * 1. Updates the global medication list in SharedPreferences.
     * 2. Clears any existing future schedules for this ID in the Room database.
     * 3. Generates a fresh set of {@link DoseInstanceEntity} records for the coming week.
     * 4. Triggers {@link ReminderManager} to set Android system alarms for the new doses.
     * </p>
     *
     * @param context The application context.
     */
    public void addToMedicationList(final Context context) {
        final JSONObject json = toJson();
        if (json == null) return;
        
        JSONArray medsArray = SharedPreferencesManager.getInstance(context).getJsonArray(SPK_MEDICATION_LIST, null);
        if (medsArray == null) {
            medsArray = new JSONArray();
        }

        // 1. Update SharedPreferences (Small list of definitions)
        boolean found = false;
        for (int i = 0; i < medsArray.length(); i++) {
            try {
                JSONObject obj = medsArray.getJSONObject(i);
                if (mId.equals(obj.optString(JsonKeys.ID))) {
                    medsArray.put(i, json);
                    found = true;
                    break;
                }
            } catch (JSONException ignored) {}
        }

        if (!found) {
            medsArray.put(json);
        }
        SharedPreferencesManager.getInstance(context).setJsonArray(SPK_MEDICATION_LIST, medsArray);

        // 2. Room logic: Generate and save schedules
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            long now = System.currentTimeMillis();
            
            // FIX: Cancel and delete only FUTURE doses to preserve historical records in History view.
            // Range: from 1 minute ago (buffer) to the end of the scheduling window.
            long startRange = now - 60000;
            long endRange = now + (AppConfig.NUMBER_OF_DAYS_TO_SCHEDULE * 24 * 60 * 60 * 1000L);
            
            List<DoseInstanceEntity> instancesInRange = db.doseInstanceDao().getInstancesInRangeInternal(startRange, endRange);
            for (DoseInstanceEntity e : instancesInRange) {
                if (mId.equals(e.getMedicationId())) {
                    ReminderManager.cancelReminder(context, e.getId());
                    // Delete future doses only (SCHEDULED) or all in current range to refresh definition?
                    // We delete all in range to ensure the 7-day window always matches the latest definition.
                    db.doseInstanceDao().deleteInstanceInternal(e);
                }
            }

            final SparseArray<SimpleDayTime> timesADay = getTimesADay();
            if (timesADay == null || timesADay.size() == 0) return;

            List<DoseInstanceEntity> entities = new ArrayList<>();
            for (int i = 0; i < AppConfig.NUMBER_OF_DAYS_TO_SCHEDULE; i++) {
                for (int k = 0; k < timesADay.size(); k++) {
                    SimpleDayTime time = timesADay.valueAt(k);
                    MedicationInstance instance = getMedicationInstance(i, time);
                    entities.add(DoseInstanceEntity.fromInstance(instance));
                }
            }

            if (!entities.isEmpty()) {
                db.doseInstanceDao().insertAll(entities);
                
                // Re-fetch only the newly inserted future instances to set alarms
                List<DoseInstanceEntity> scheduled = db.doseInstanceDao().getInstancesInRangeInternal(now, endRange);
                for (DoseInstanceEntity e : scheduled) {
                    if (mId.equals(e.getMedicationId())) {
                        ReminderManager.scheduleReminder(context, e);
                    }
                }

                Logger.log("Room", "Saved " + entities.size() + " doses for " + mCommercialName);
            }
        });
    }

    /**
     * Internal helper to create a specific time-stamped instance of this medication.
     *
     * @param dayOffset Number of days from today.
     * @param time      The specific time of day.
     * @return A self-contained MedicationInstance.
     */
    @NonNull
    private MedicationInstance getMedicationInstance(int dayOffset, SimpleDayTime time) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_YEAR, dayOffset);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, time.getHour());
        calendar.set(java.util.Calendar.MINUTE, time.getMinute());
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        long scheduledTime = calendar.getTimeInMillis();

        final MedicationInstance medicationInstance = new MedicationInstance(this);
        medicationInstance.setScheduledTime(scheduledTime);
        medicationInstance.setStatus(MedicationInstance.Status.SCHEDULED);
        return medicationInstance;
    }

    /**
     * Permanently deletes a medication and all its associated scheduled doses.
     *
     * @param context The application context.
     * @param id      The UUID of the medication to remove.
     */
    public static void deleteMedication(Context context, String id) {
        JSONArray existingMeds = SharedPreferencesManager.getInstance(context).getJsonArray(SPK_MEDICATION_LIST, null);
        if (existingMeds == null) return;

        JSONArray newList = new JSONArray();
        for (int i = 0; i < existingMeds.length(); i++) {
            try {
                JSONObject obj = existingMeds.getJSONObject(i);
                if (!id.equals(obj.optString(JsonKeys.ID))) {
                    newList.put(obj);
                }
            } catch (JSONException ignored) {}
        }
        SharedPreferencesManager.getInstance(context).setJsonArray(SPK_MEDICATION_LIST, newList);
        
        // Purge records from database
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            
            // FIX: Cancel all future alarms for this medication before deleting
            long now = System.currentTimeMillis();
            long endRange = now + (AppConfig.NUMBER_OF_DAYS_TO_SCHEDULE * 24 * 60 * 60 * 1000L);
            List<DoseInstanceEntity> oldInstances = db.doseInstanceDao().getInstancesInRangeInternal(now - 60000, endRange);
            for (DoseInstanceEntity e : oldInstances) {
                if (id.equals(e.getMedicationId())) {
                    ReminderManager.cancelReminder(context, e.getId());
                }
            }
            
            db.doseInstanceDao().deleteByMedicationId(id);
        });
    }

    /**
     * Wipes all application data, including definitions and history.
     *
     * @param context The application context.
     */
    public static void clearAllMedications(Context context) {
        SharedPreferencesManager.getInstance(context).removeKey(SPK_MEDICATION_LIST);
        
        // Wipe database
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            
            // FIX: Cancel all future alarms before clearing database
            long now = System.currentTimeMillis();
            long endRange = now + (AppConfig.NUMBER_OF_DAYS_TO_SCHEDULE * 24 * 60 * 60 * 1000L);
            List<DoseInstanceEntity> allFuture = db.doseInstanceDao().getInstancesInRangeInternal(now - 60000, endRange);
            for (DoseInstanceEntity e : allFuture) {
                ReminderManager.cancelReminder(context, e.getId());
            }
            
            db.doseInstanceDao().deleteAll();
        });
    }

    /**
     * Retrieves the list of all defined medications from SharedPreferences.
     *
     * @param context The application context.
     * @return A sorted ArrayList of medication objects.
     */
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

    /**
     * Deserializes a Medication object from JSON.
     * Handles recursive parsing of Nested SparseArrays (TimesADay).
     *
     * @param json The input JSONObject.
     * @return A populated Medication instance, or {@code null} if parsing fails.
     */
    public static Medication fromJson(JSONObject json) {
        if (json == null) return null;
        Medication med = new Medication();
        try {
            med.mId = json.optString(JsonKeys.ID, UUID.randomUUID().toString());
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

    /**
     * Updates the daily schedule with a new set of timestamps.
     * Automatically triggers {@link #sortTimesADay()} to ensure chronological order.
     *
     * @param simpleDayTimeSparseArray A map of index-to-time for the doses.
     */
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
        sortTimesADay();
    }

    /**
     * Sorts the medication's daily doses chronologically.
     * Re-indexes the internal SparseArray sequentially starting from 1 to 
     * maintain consistency with the UI.
     */
    public void sortTimesADay() {
        if (mTimesADay == null || mTimesADay.size() <= 1) return;

        List<SimpleDayTime> times = new ArrayList<>();
        for (int i = 0; i < mTimesADay.size(); i++) {
            times.add(mTimesADay.valueAt(i));
        }

        Collections.sort(times);

        mTimesADay.clear();
        for (int i = 0; i < times.size(); i++) {
            // Re-index sequentially from 1 to maintain consistency with the UI keys
            mTimesADay.put(i + 1, times.get(i));
        }
    }

    public String getId() { return mId; }
    public void setId(String id) { this.mId = id; }
    public String getCommercialName() { return mCommercialName; }
    public void setCommercialName(@NonNull final String commercialName) { this.mCommercialName = commercialName; }
    public float getAmount() { return mAmount; }
    public void setAmount(final float amount) { this.mAmount = amount; }
    public EForm getForm() { return mForm; }
    public void setForm(EForm mForm) { this.mForm = mForm; }
    public float getStrength() { return mStrength; }
    public void setStrength(float mStrength) { this.mStrength = mStrength; }
    public String getMedicalCondition() { return mMedicalCondition; }
    public void setMedicalCondition(String mMedicalCondition) { this.mMedicalCondition = mMedicalCondition; }
    public List<Long> getDailySchedule() { return mDailySchedule; }
    public void setDailySchedule(List<Long> mDailySchedule) { this.mDailySchedule = mDailySchedule; }
    public EMeasurementUnit getMeasurementUnit() { return mMeasurementUnit; }
    public void setMeasurementUnit(EMeasurementUnit mMeasurementUnit) { this.mMeasurementUnit = mMeasurementUnit; }
    public int getAmountLeft() { return mAmountLeft; }
    public void setAmountLeft(int mAmountLeft) { this.mAmountLeft = mAmountLeft; }
    public SparseArray<SimpleDayTime> getTimesADay() { return mTimesADay; }
    public EInstructions getInstruction() { return mInstruction; }
    public void setInstruction(EInstructions mInstruction) { this.mInstruction = mInstruction; }

    /**
     * Case-insensitive alphabetical comparison by name.
     */
    @Override
    public int compareTo(@NonNull Medication other) {
        if (this.mCommercialName == null && other.mCommercialName == null) return 0;
        if (this.mCommercialName == null) return 1;
        if (other.mCommercialName == null) return -1;
        return this.mCommercialName.compareToIgnoreCase(other.mCommercialName);
    }

    @NonNull
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


    /**
     * Serializes this medication into a JSONObject for storage in SharedPreferences.
     *
     * @return The resulting JSONObject.
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(JsonKeys.ID, mId);
            json.put(JsonKeys.COMMERCIAL_NAME, mCommercialName);
            json.put(JsonKeys.FORM, mForm.name());
            json.put(JsonKeys.FREQUENCY, mFrequency);
            json.put(JsonKeys.AMOUNT, mAmount);
            json.put(JsonKeys.STRENGTH, mStrength);
            json.put(JsonKeys.MEDICAL_CONDITION, mMedicalCondition);
            json.put(JsonKeys.AMOUNT_LEFT, mAmountLeft);
            if (mInstruction != null) json.put(JsonKeys.INSTRUCTIONS, mInstruction.name());
            if (mMeasurementUnit != null)
                json.put(JsonKeys.MEASUREMENT_UNIT, mMeasurementUnit.name());
            if (mDailySchedule != null)
                json.put(JsonKeys.DAILY_SCHEDULE, getDailyScheduleAsJsonArray());
            json.put(JsonKeys.TIMES_IN_DAY, getTimesADayAsJsonArray());
        } catch (JSONException e) {
            Logger.log(Me(), "Tried to convert myself to JSONObject but an exception happen");
            return null;
        }
        return json;
    }

    /**
     * Helper to serialize the SparseArray of times into a JSON-compatible format.
     */
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

    /**
     * Helper to serialize the daily schedule list into a JSON-compatible format.
     */
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

    /** Constants for JSON keys to prevent typos during serialization. */
    public static final class JsonKeys {
        public static final String ID = "mId";
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