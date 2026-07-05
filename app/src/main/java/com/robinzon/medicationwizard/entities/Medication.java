package com.robinzon.medicationwizard.entities;

import android.content.Context;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import com.robinzon.medicationwizard.AppConfig;
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
 * Domain model representing a medication definition.
 * <p>
 * This class holds the persistent configuration for a medication (e.g., Aspirin 500mg, 
 * taken twice a day). It handles serialization to JSON for SharedPreferences storage 
 * and coordinate with Room for scheduling dose instances.
 * </p>
 */
public class Medication implements Comparable<Medication> {

    public static final String PREF_MEDICATION_LIST = "shared_pref_medications_list";

    private String id;
    private SparseArray<SimpleDayTime> timesADay;
    private float amount;
    private int frequency;
    private String commercialName;
    private EForm form;
    private float strength;
    private String medicalCondition;
    private List<Long> dailySchedule;
    private int amountLeft;
    private EInstructions instruction;
    private EMeasurementUnit measurementUnit;

    /**
     * Constructs a new medication with a unique random UUID.
     */
    public Medication() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Constructor used primarily for cloning or reconstruction from partial data.
     *
     * @param id The unique identifier for this medication.
     */
    public Medication(String id) {
        this.id = id;
    }

    /**
     * @return The number of doses scheduled per day.
     */
    public int getDailyFrequency() {
        return frequency;
    }

    /**
     * @param frequency The number of doses scheduled per day.
     */
    public void setDailyFrequency(int frequency) {
        this.frequency = frequency;
    }

    /**
     * @return True if the minimum required fields (Name, Amount, Frequency) are populated.
     */
    public boolean isValid() {
        return !java.util.Objects.equals(commercialName, "") &&
                commercialName != null &&
                amount > 0 &&
                frequency > 0;
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
        
        JSONArray medsArray = SharedPreferencesManager.getInstance(context).getJsonArray(PREF_MEDICATION_LIST, null);
        if (medsArray == null) {
            medsArray = new JSONArray();
        }

        // 1. Update SharedPreferences (Small list of definitions)
        boolean found = false;
        for (int i = 0; i < medsArray.length(); i++) {
            try {
                JSONObject obj = medsArray.getJSONObject(i);
                if (id.equals(obj.optString(JsonKeys.ID))) {
                    medsArray.put(i, json);
                    found = true;
                    break;
                }
            } catch (JSONException ignored) {}
        }

        if (!found) {
            medsArray.put(json);
        }
        SharedPreferencesManager.getInstance(context).setJsonArray(PREF_MEDICATION_LIST, medsArray);

        // 2. Room logic: Generate and save schedules
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            
            // Step A: Cancel existing alarms for pending doses
            List<DoseInstanceEntity> scheduled = db.doseInstanceDao().getScheduledByMedicationId(id);
            for (DoseInstanceEntity e : scheduled) {
                ReminderManager.cancelReminder(context, e.getId());
            }
            
            // Step B: Purge all pending (SCHEDULED) doses to ensure latest definition is applied
            db.doseInstanceDao().deleteScheduledByMedicationId(id);

            final SparseArray<SimpleDayTime> activeTimes = getTimesADay();
            if (activeTimes == null || activeTimes.size() == 0) return;

            // Step C: Generate fresh doses for the scheduling window (Today + future)
            List<DoseInstanceEntity> newEntities = new ArrayList<>();
            for (int i = 0; i < AppConfig.NUMBER_OF_DAYS_TO_SCHEDULE; i++) {
                for (int k = 0; k < activeTimes.size(); k++) {
                    SimpleDayTime time = activeTimes.valueAt(k);
                    MedicationInstance instance = getMedicationInstance(i, time);
                    long scheduledTime = instance.getScheduledTime();
                    
                    // CRITICAL FIX: Only create if a dose doesn't already exist for this slot.
                    // This preserves historical TAKEN/SKIPPED records for today while 
                    // updating all pending tasks with new name/strength/form.
                    if (db.doseInstanceDao().getInstanceByTime(id, scheduledTime) == null) {
                        newEntities.add(DoseInstanceEntity.fromInstance(instance));
                    }
                }
            }

            if (!newEntities.isEmpty()) {
                db.doseInstanceDao().insertAll(newEntities);
                
                // Step D: Re-schedule Android alarms for all future doses
                long now = System.currentTimeMillis();
                List<DoseInstanceEntity> futureDoses = db.doseInstanceDao().getInstancesInRangeInternal(now, now + (AppConfig.NUMBER_OF_DAYS_TO_SCHEDULE * 24 * 60 * 60 * 1000L));
                for (DoseInstanceEntity e : futureDoses) {
                    if (id.equals(e.getMedicationId()) && "SCHEDULED".equals(e.getStatus())) {
                        ReminderManager.scheduleReminder(context, e);
                    }
                }

                Logger.log("Room", "Refreshed " + newEntities.size() + " doses for " + commercialName);
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
     * Completely removes a medication definition and all associated schedules.
     *
     * @param context Application context.
     * @param id      The medication ID to delete.
     */
    public static void deleteMedication(final Context context, final String id) {
        // 1. SharedPreferences cleanup
        JSONArray medsArray = SharedPreferencesManager.getInstance(context).getJsonArray(PREF_MEDICATION_LIST, null);
        if (medsArray != null) {
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < medsArray.length(); i++) {
                try {
                    JSONObject obj = medsArray.getJSONObject(i);
                    if (!id.equals(obj.optString(JsonKeys.ID))) {
                        newArray.put(obj);
                    }
                } catch (JSONException ignored) {}
            }
            SharedPreferencesManager.getInstance(context).setJsonArray(PREF_MEDICATION_LIST, newArray);
        }

        // 2. Room cleanup: cancel alarms and delete records
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            List<DoseInstanceEntity> instances = db.doseInstanceDao().getAllInstancesInternal();
            for (DoseInstanceEntity e : instances) {
                if (id.equals(e.getMedicationId())) {
                    ReminderManager.cancelReminder(context, e.getId());
                }
            }
            db.doseInstanceDao().deleteByMedicationId(id);
        });
    }

    /**
     * Wipes all user data (Medications and Schedules).
     *
     * @param context Application context.
     */
    public static void clearAllMedications(final Context context) {
        // 1. SharedPreferences
        SharedPreferencesManager.getInstance(context).setJsonArray(PREF_MEDICATION_LIST, null);

        // 2. Room
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            List<DoseInstanceEntity> instances = db.doseInstanceDao().getAllInstancesInternal();
            for (DoseInstanceEntity e : instances) {
                ReminderManager.cancelReminder(context, e.getId());
            }
            db.doseInstanceDao().deleteAll();
        });
    }

    /**
     * @param context Application context.
     * @return True if there is at least one medication definition in the library.
     */
    public static boolean hasMedications(Context context) {
        JSONArray array = SharedPreferencesManager.getInstance(context).getJsonArray(PREF_MEDICATION_LIST, null);
        return array != null && array.length() > 0;
    }

    /**
     * Retrieves all saved medications as domain objects.
     *
     * @param context Application context.
     * @return List of medication definitions.
     */
    public static ArrayList<Medication> getSavedMedications(final Context context) {
        ArrayList<Medication> result = new ArrayList<>();
        JSONArray array = SharedPreferencesManager.getInstance(context).getJsonArray(PREF_MEDICATION_LIST, null);
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    result.add(fromJson(array.getJSONObject(i)));
                } catch (JSONException ignored) {}
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Reconstructs a Medication object from its JSON representation.
     *
     * @param json The serialized data.
     * @return A populated Medication object.
     */
    public static Medication fromJson(@NonNull JSONObject json) {
        Medication med = new Medication(json.optString(JsonKeys.ID));
        med.setCommercialName(json.optString(JsonKeys.COMMERCIAL_NAME));
        med.setAmount((float) json.optDouble(JsonKeys.AMOUNT, 0));
        med.setDailyFrequency(json.optInt(JsonKeys.FREQUENCY, 0));
        med.setStrength((float) json.optDouble(JsonKeys.STRENGTH, 0));
        med.setMedicalCondition(json.optString(JsonKeys.MEDICAL_CONDITION));
        med.setAmountLeft(json.optInt(JsonKeys.AMOUNT_LEFT, 0));

        if (!json.isNull(JsonKeys.FORM)) {
            try { med.setForm(EForm.valueOf(json.getString(JsonKeys.FORM))); } catch (Exception ignored) {}
        }
        if (!json.isNull(JsonKeys.INSTRUCTIONS)) {
            try { med.setInstruction(EInstructions.valueOf(json.getString(JsonKeys.INSTRUCTIONS))); } catch (Exception ignored) {}
        }
        if (!json.isNull(JsonKeys.MEASUREMENT_UNIT)) {
            try { med.setMeasurementUnit(EMeasurementUnit.valueOf(json.getString(JsonKeys.MEASUREMENT_UNIT))); } catch (Exception ignored) {}
        }

        JSONArray times = json.optJSONArray(JsonKeys.TIMES_IN_DAY);
        if (times != null) {
            SparseArray<SimpleDayTime> timeMap = new SparseArray<>();
            for (int i = 0; i < times.length(); i++) {
                SimpleDayTime t = SimpleDayTime.fromJson(times.opt(i));
                if (t != null) timeMap.put(i + 1, t);
            }
            med.addTimeStampsForDay(timeMap);
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
            timesADay = null;
            return;
        }
        timesADay = new SparseArray<>(simpleDayTimeSparseArray.size());
        for (int i = 0; i < simpleDayTimeSparseArray.size(); i++) {
            int key = simpleDayTimeSparseArray.keyAt(i);
            SimpleDayTime value = simpleDayTimeSparseArray.valueAt(i);
            timesADay.put(key, new SimpleDayTime(value));
        }
        sortTimesADay();
    }

    /**
     * Logical sorter: Sorts the daily dose times chronologically.
     */
    public void sortTimesADay() {
        if (timesADay == null || timesADay.size() <= 1) return;
        
        List<SimpleDayTime> list = new ArrayList<>();
        for (int i = 0; i < timesADay.size(); i++) {
            list.add(timesADay.valueAt(i));
        }
        Collections.sort(list);
        
        timesADay.clear();
        for (int i = 0; i < list.size(); i++) {
            timesADay.put(i + 1, list.get(i));
        }
    }

    /** @return The unique identifier of the medication. */
    public String getId() { return id; }
    /** @param id The unique identifier of the medication. */
    public void setId(String id) { this.id = id; }
    /** @return The commercial display name. */
    public String getCommercialName() { return commercialName; }
    /** @param commercialName The commercial display name. */
    public void setCommercialName(String commercialName) { this.commercialName = commercialName; }
    /** @return The amount per dose. */
    public float getAmount() { return amount; }
    /** @param amount The amount per dose. */
    public void setAmount(float amount) { this.amount = amount; }
    /** @return The delivery form (e.g., Pill, Drops). */
    public EForm getForm() { return form; }
    /** @param form The delivery form (e.g., Pill, Drops). */
    public void setForm(EForm form) { this.form = form; }
    /** @return The strength value (e.g., 500). */
    public float getStrength() { return strength; }
    /** @param strength The strength value (e.g., 500). */
    public void setStrength(float strength) { this.strength = strength; }
    /** @return The medical condition being treated. */
    public String getMedicalCondition() { return medicalCondition; }
    /** @param medicalCondition The medical condition being treated. */
    public void setMedicalCondition(String medicalCondition) { this.medicalCondition = medicalCondition; }
    /** @return The list of daily timestamps. */
    public List<Long> getDailySchedule() { return dailySchedule; }
    /** @param dailySchedule The list of daily timestamps. */
    public void setDailySchedule(List<Long> dailySchedule) { this.dailySchedule = dailySchedule; }
    /** @return The measurement unit (e.g., mg, ml). */
    public EMeasurementUnit getMeasurementUnit() { return measurementUnit; }
    /** @param measurementUnit The measurement unit (e.g., mg, ml). */
    public void setMeasurementUnit(EMeasurementUnit measurementUnit) { this.measurementUnit = measurementUnit; }
    /** @return The count of remaining doses in the pack. */
    public int getAmountLeft() { return amountLeft; }
    /** @param amountLeft The count of remaining doses in the pack. */
    public void setAmountLeft(int amountLeft) { this.amountLeft = amountLeft; }
    /** @return Map of daily dose indices to times. */
    public SparseArray<SimpleDayTime> getTimesADay() { return timesADay; }
    /** @return Instructions for taking (e.g., Before Food). */
    public EInstructions getInstruction() { return instruction; }
    /** @param instruction Instructions for taking (e.g., Before Food). */
    public void setInstruction(EInstructions instruction) { this.instruction = instruction; }

    @Override
    public int compareTo(Medication other) {
        if (this.commercialName == null) return -1;
        if (other.commercialName == null) return 1;
        return this.commercialName.compareToIgnoreCase(other.commercialName);
    }

    @NonNull
    @Override
    public String toString() {
        return "Medication{" +
                "id='" + id + '\'' +
                ", name='" + commercialName + '\'' +
                ", amount=" + amount +
                ", frequency=" + frequency +
                '}';
    }

    /**
     * Serializes the medication definition to a JSONObject.
     *
     * @return The resulting JSONObject.
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(JsonKeys.ID, id);
            json.put(JsonKeys.COMMERCIAL_NAME, commercialName);
            json.put(JsonKeys.AMOUNT, (double) amount);
            json.put(JsonKeys.FREQUENCY, frequency);
            json.put(JsonKeys.STRENGTH, (double) strength);
            json.put(JsonKeys.MEDICAL_CONDITION, medicalCondition);
            json.put(JsonKeys.AMOUNT_LEFT, amountLeft);
            if (form != null) json.put(JsonKeys.FORM, form.name());
            if (instruction != null) json.put(JsonKeys.INSTRUCTIONS, instruction.name());
            if (measurementUnit != null) json.put(JsonKeys.MEASUREMENT_UNIT, measurementUnit.name());
            json.put(JsonKeys.TIMES_IN_DAY, getTimesADayAsJsonArray());
        } catch (JSONException e) {
            return null;
        }
        return json;
    }

    private JSONArray getTimesADayAsJsonArray() {
        JSONArray array = new JSONArray();
        if (timesADay != null) {
            for (int i = 0; i < timesADay.size(); i++) {
                array.put(timesADay.valueAt(i).toString());
            }
        }
        return array;
    }

    private JSONArray getDailyScheduleAsJsonArray() {
        JSONArray array = new JSONArray();
        if (dailySchedule != null) {
            for (Long time : dailySchedule) {
                array.put(time);
            }
        }
        return array;
    }

    public static class JsonKeys {
        public static final String ID = "mId";
        public static final String TIMES_IN_DAY = "mTimesADay";
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
