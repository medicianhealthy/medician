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
    private int frequency = -1;
    private String commercialName;
    private EForm form;
    private float strength;
    private String medicalCondition;
    private List<Long> dailySchedule;
    private int amountLeft;
    private EInstructions instruction;
    private EMeasurementUnit measurementUnit;
    private String imagePath;
    private Long lastTakenTimestamp;
    private boolean isCritical;

    // Inventory Tracker
    private float inventoryCurrent;
    private float inventoryThreshold;
    private InventoryAlertType inventoryAlertType = InventoryAlertType.AMOUNT_REACHED;

    public enum InventoryAlertType {
        DAYS_BEFORE, AMOUNT_REACHED
    }

    /**
     * Constructs a new medication with a unique random UUID.
     */
    public Medication() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Constructs a new medication used primarily for cloning or reconstruction from partial data.
     *
     * @param id The unique identifier for this medication.
     */
    public Medication(String id) {
        this.id = id;
    }

    /**
     * Removes a medication definition and all associated schedules completely.
     *
     * @param context Must not be null. Application context.
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
                } catch (JSONException ignored) {
                }
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
     * Wipes all medication data synchronously, preserving user settings and usage statistics.
     *
     * @param context Must not be null. Application context.
     */
    public static void clearAllMedicationsInternal(final Context context) {
        // 1. SharedPreferences: Clear ONLY the medication list
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(context);
        sp.removeKey(PREF_MEDICATION_LIST);

        // 2. Room: Cancel all alarms and wipe the table
        AppDatabase db = AppDatabase.getDatabase(context);
        List<DoseInstanceEntity> instances = db.doseInstanceDao().getAllInstancesInternal();
        for (DoseInstanceEntity e : instances) {
            ReminderManager.cancelReminder(context, e.getId());
        }
        db.doseInstanceDao().deleteAll();
    }

    /**
     * Wipes all medication data asynchronously, preserving user settings and usage statistics.
     *
     * @param context Must not be null. Application context.
     */
    public static void clearAllMedicationsAsync(final Context context) {
        AppDatabase.databaseWriteExecutor.execute(() -> clearAllMedicationsInternal(context));
    }

    /**
     * Checks whether there is at least one medication definition in the library.
     * 
     * @param context Must not be null. Application context.
     * @return Returns true if there is at least one medication definition.
     */
    public static boolean hasMedications(Context context) {
        JSONArray array = SharedPreferencesManager.getInstance(context).getJsonArray(PREF_MEDICATION_LIST, null);
        return array != null && array.length() > 0;
    }

    /**
     * Retrieves all saved medications as domain objects.
     *
     * @param context Must not be null. Application context.
     * @return Returns a list of all saved medication definitions.
     */
    public static ArrayList<Medication> getSavedMedications(final Context context) {
        ArrayList<Medication> result = new ArrayList<>();
        JSONArray array = SharedPreferencesManager.getInstance(context).getJsonArray(PREF_MEDICATION_LIST, null);
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    result.add(fromJson(array.getJSONObject(i)));
                } catch (JSONException ignored) {
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Reconstructs a Medication object from its JSON representation.
     *
     * @param json Must not be null. The serialized JSON data.
     * @return Returns a populated Medication object.
     */
    public static Medication fromJson(@NonNull JSONObject json) {
        Medication med = new Medication(json.optString(JsonKeys.ID));
        med.setCommercialName(json.optString(JsonKeys.COMMERCIAL_NAME));
        med.setAmount((float) json.optDouble(JsonKeys.AMOUNT, 0));
        med.setDailyFrequency(json.optInt(JsonKeys.FREQUENCY, 0));
        med.setStrength((float) json.optDouble(JsonKeys.STRENGTH, 0));
        med.setMedicalCondition(json.optString(JsonKeys.MEDICAL_CONDITION));
        med.setAmountLeft(json.optInt(JsonKeys.AMOUNT_LEFT, 0));
        med.setImagePath(json.optString(JsonKeys.IMAGE_PATH, null));
        if (json.has(JsonKeys.LAST_TAKEN_TIMESTAMP)) {
            med.setLastTakenTimestamp(json.optLong(JsonKeys.LAST_TAKEN_TIMESTAMP));
        }
        med.setCritical(json.optBoolean(JsonKeys.IS_CRITICAL, false));

        // Inventory
        med.setInventoryCurrent((float) json.optDouble(JsonKeys.INVENTORY_CURRENT, 0));
        med.setInventoryThreshold((float) json.optDouble(JsonKeys.INVENTORY_THRESHOLD, 0));
        if (!json.isNull(JsonKeys.INVENTORY_ALERT_TYPE)) {
            try {
                med.setInventoryAlertType(InventoryAlertType.valueOf(json.getString(JsonKeys.INVENTORY_ALERT_TYPE)));
            } catch (Exception ignored) {}
        }

        if (!json.isNull(JsonKeys.FORM)) {
            try {
                med.setForm(EForm.valueOf(json.getString(JsonKeys.FORM)));
            } catch (Exception ignored) {
            }
        }
        if (!json.isNull(JsonKeys.INSTRUCTIONS)) {
            try {
                med.setInstruction(EInstructions.valueOf(json.getString(JsonKeys.INSTRUCTIONS)));
            } catch (Exception ignored) {
            }
        }
        if (!json.isNull(JsonKeys.MEASUREMENT_UNIT)) {
            try {
                med.setMeasurementUnit(EMeasurementUnit.valueOf(json.getString(JsonKeys.MEASUREMENT_UNIT)));
            } catch (Exception ignored) {
            }
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
     * Retrieves the number of doses scheduled per day.
     * 
     * @return Returns the number of doses scheduled per day.
     */
    public int getDailyFrequency() {
        return frequency;
    }

    /**
     * Updates the number of doses scheduled per day.
     *
     * @param frequency The number of doses scheduled per day.
     */
    public void setDailyFrequency(int frequency) {
        this.frequency = frequency;
    }

    /**
     * Checks if the minimum required fields (Name, Amount, Frequency, Form) are populated.
     * 
     * @return Returns true if the medication definition is valid.
     */
    public boolean isValid() {
        return commercialName != null && !commercialName.trim().isEmpty() &&
                amount > 0 &&
                frequency >= 0 &&
                form != null;
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
     * @param context Must not be null. The application context.
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
            } catch (JSONException ignored) {
            }
        }

        if (!found) {
            medsArray.put(json);
        }
        SharedPreferencesManager.getInstance(context).setJsonArray(PREF_MEDICATION_LIST, medsArray);

        // 2. Room logic: Generate and save schedules
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);

            final SparseArray<SimpleDayTime> activeTimes = getTimesADay();
            if (activeTimes == null || activeTimes.size() == 0) return;

            // Step A: Synchronize METADATA for ALL existing doses (History + Future)
            db.doseInstanceDao().updateMetadataForAllDoses(
                    id,
                    commercialName,
                    amount,
                    strength,
                    measurementUnit != null ? measurementUnit.getName() : null,
                    form != null ? form.name() : null,
                    instruction != null ? instruction.name() : null,
                    imagePath
            );

            // Step B: CLEAN SLATE for future "un-acted" doses
            // This prevents "ghost" notifications if times were changed.
            // We only delete 'SCHEDULED' doses.
            List<DoseInstanceEntity> currentScheduled = db.doseInstanceDao().getScheduledByMedicationId(id);
            for (DoseInstanceEntity e : currentScheduled) {
                ReminderManager.cancelReminder(context, e.getId());
            }
            db.doseInstanceDao().deleteScheduledByMedicationId(id);

            // Step C: Ensure we have doses for the scheduling window (Today + future)
            List<DoseInstanceEntity> newEntities = new ArrayList<>();
            for (int i = 0; i < AppConfig.NUMBER_OF_DAYS_TO_SCHEDULE; i++) {
                for (int k = 0; k < activeTimes.size(); k++) {
                    SimpleDayTime time = activeTimes.valueAt(k);
                    MedicationInstance instance = getMedicationInstance(i, time);
                    long targetTime = instance.getScheduledTime();

                    // Logic for duplicate prevention:
                    // Check if ANY dose exists for this time slot (Taken, Skipped, Snoozed, or exact match)
                    // We use a 2-hour window around the target time to catch shifted/acted-upon doses.
                    long startWindow = targetTime - (30 * 60 * 1000L); // 30 mins before
                    long endWindow = targetTime + (120 * 60 * 1000L); // 2 hours after (snooze range)

                    if (db.doseInstanceDao().getAnyInstanceInWindow(id, startWindow, endWindow) == null) {
                        newEntities.add(DoseInstanceEntity.fromInstance(instance));
                    }
                }
            }

            if (!newEntities.isEmpty()) {
                db.doseInstanceDao().insertAll(newEntities);
                Logger.log("Room", "Added " + newEntities.size() + " new doses for " + commercialName);
            }

            // Step D: Re-schedule Android alarms for all future doses
            long now = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();
            List<DoseInstanceEntity> futureDoses = db.doseInstanceDao().getInstancesInRangeInternal(now, now + (AppConfig.NUMBER_OF_DAYS_TO_SCHEDULE * 24 * 60 * 60 * 1000L));
            for (DoseInstanceEntity e : futureDoses) {
                if (id.equals(e.getMedicationId()) && "SCHEDULED".equals(e.getStatus())) {
                    ReminderManager.scheduleReminder(context, e);
                }
            }
        });
    }

    /**
     * Creates a specific time-stamped instance of this medication internally.
     *
     * @param dayOffset Number of days from today.
     * @param time      The specific time of day.
     * @return Returns a self-contained MedicationInstance.
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
     * Updates the daily schedule with a new set of timestamps.
     * <p>
     * Automatically triggers {@link #sortTimesADay()} to ensure chronological order.
     * </p>
     *
     * @param simpleDayTimeSparseArray Must not be null. A map of index-to-time for the doses.
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
     * Sorts the daily dose times chronologically.
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

    /**
     * Retrieves the unique identifier of the medication.
     * 
     * @return Returns the unique identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Updates the unique identifier of the medication.
     * 
     * @param id The unique identifier of the medication.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Retrieves the commercial display name.
     * 
     * @return Returns the commercial display name.
     */
    public String getCommercialName() {
        return commercialName;
    }

    /**
     * Updates the commercial display name.
     * 
     * @param commercialName The commercial display name.
     */
    public void setCommercialName(String commercialName) {
        this.commercialName = commercialName;
    }

    /**
     * Retrieves the amount per dose.
     * 
     * @return Returns the amount per dose.
     */
    public float getAmount() {
        return amount;
    }

    /**
     * Updates the amount per dose.
     * 
     * @param amount The amount per dose.
     */
    public void setAmount(float amount) {
        this.amount = amount;
    }

    /**
     * Retrieves the delivery form.
     * 
     * @return Returns the delivery form (e.g., Pill, Drops).
     */
    public EForm getForm() {
        return form;
    }

    /**
     * Updates the delivery form.
     * 
     * @param form The delivery form (e.g., Pill, Drops).
     */
    public void setForm(EForm form) {
        this.form = form;
    }

    /**
     * Retrieves the strength value.
     * 
     * @return Returns the strength value (e.g., 500).
     */
    public float getStrength() {
        return strength;
    }

    /**
     * Updates the strength value.
     * 
     * @param strength The strength value (e.g., 500).
     */
    public void setStrength(float strength) {
        this.strength = strength;
    }

    /**
     * Retrieves the medical condition being treated.
     * 
     * @return Returns the medical condition being treated.
     */
    public String getMedicalCondition() {
        return medicalCondition;
    }

    /**
     * Updates the medical condition being treated.
     * 
     * @param medicalCondition The medical condition being treated.
     */
    public void setMedicalCondition(String medicalCondition) {
        this.medicalCondition = medicalCondition;
    }

    /**
     * Retrieves the list of daily timestamps.
     * 
     * @return Returns the list of daily timestamps.
     */
    public List<Long> getDailySchedule() {
        return dailySchedule;
    }

    /**
     * Updates the list of daily timestamps.
     * 
     * @param dailySchedule The list of daily timestamps.
     */
    public void setDailySchedule(List<Long> dailySchedule) {
        this.dailySchedule = dailySchedule;
    }

    /**
     * Retrieves the measurement unit.
     * 
     * @return Returns the measurement unit (e.g., mg, ml).
     */
    public EMeasurementUnit getMeasurementUnit() {
        return measurementUnit;
    }

    /**
     * Updates the measurement unit.
     * 
     * @param measurementUnit The measurement unit (e.g., mg, ml).
     */
    public void setMeasurementUnit(EMeasurementUnit measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    /**
     * Retrieves the count of remaining doses in the pack.
     * 
     * @return Returns the count of remaining doses.
     */
    public int getAmountLeft() {
        return amountLeft;
    }

    /**
     * Updates the count of remaining doses in the pack.
     * 
     * @param amountLeft The count of remaining doses in the pack.
     */
    public void setAmountLeft(int amountLeft) {
        this.amountLeft = amountLeft;
    }

    /**
     * Retrieves the map of daily dose indices to times.
     * 
     * @return Returns the sparse array mapping dose index to scheduled time.
     */
    public SparseArray<SimpleDayTime> getTimesADay() {
        return timesADay;
    }

    /**
     * Retrieves instructions for taking the medication.
     * 
     * @return Returns instructions for taking (e.g., Before Food).
     */
    public EInstructions getInstruction() {
        return instruction;
    }

    /**
     * Updates instructions for taking the medication.
     * 
     * @param instruction Instructions for taking (e.g., Before Food).
     */
    public void setInstruction(EInstructions instruction) {
        this.instruction = instruction;
    }

    /**
     * Retrieves the path to the medication's image.
     *
     * @return Returns the image path string.
     */
    public String getImagePath() {
        return imagePath;
    }

    /**
     * Updates the path to the medication's image.
     *
     * @param imagePath The image path string.
     */
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    /**
     * Retrieves the timestamp when the medication was last taken.
     *
     * @return Returns the timestamp as a Long.
     */
    public Long getLastTakenTimestamp() {
        return lastTakenTimestamp;
    }

    /**
     * Updates the timestamp when the medication was last taken.
     *
     * @param lastTakenTimestamp The timestamp as a Long.
     */
    public void setLastTakenTimestamp(Long lastTakenTimestamp) {
        this.lastTakenTimestamp = lastTakenTimestamp;
    }

    /**
     * Checks if this medication is marked as critical.
     *
     * @return Returns true if the medication is critical.
     */
    public boolean isCritical() {
        return isCritical;
    }

    /**
     * Updates the critical status of the medication.
     *
     * @param critical True if the medication is critical.
     */
    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    /**
     * Retrieves the current inventory level of the medication.
     *
     * @return Returns the current inventory level.
     */
    public float getInventoryCurrent() {
        return inventoryCurrent;
    }

    /**
     * Updates the current inventory level of the medication.
     *
     * @param inventoryCurrent The current inventory level.
     */
    public void setInventoryCurrent(float inventoryCurrent) {
        this.inventoryCurrent = inventoryCurrent;
    }

    /**
     * Retrieves the inventory threshold for alerts.
     *
     * @return Returns the inventory threshold.
     */
    public float getInventoryThreshold() {
        return inventoryThreshold;
    }

    /**
     * Updates the inventory threshold for alerts.
     *
     * @param inventoryThreshold The inventory threshold.
     */
    public void setInventoryThreshold(float inventoryThreshold) {
        this.inventoryThreshold = inventoryThreshold;
    }

    /**
     * Retrieves the type of alert triggered for inventory management.
     *
     * @return Returns the inventory alert type.
     */
    public InventoryAlertType getInventoryAlertType() {
        return inventoryAlertType;
    }

    /**
     * Updates the type of alert triggered for inventory management.
     *
     * @param inventoryAlertType The inventory alert type.
     */
    public void setInventoryAlertType(InventoryAlertType inventoryAlertType) {
        this.inventoryAlertType = inventoryAlertType;
    }

    /**
     * Checks if the medication is taken on an as-needed basis.
     *
     * @return Returns true if the frequency is 0 (as needed).
     */
    public boolean isAsNeeded() {
        return frequency == 0;
    }

    /**
     * Compares this medication with another based on their commercial names.
     *
     * @param other The other medication to compare to.
     * @return Returns a negative integer, zero, or a positive integer as this medication's name is less than, equal to, or greater than the specified medication's name.
     */
    @Override
    public int compareTo(Medication other) {
        if (this.commercialName == null) return -1;
        if (other.commercialName == null) return 1;
        return this.commercialName.compareToIgnoreCase(other.commercialName);
    }

    /**
     * Generates a string representation of the medication.
     *
     * @return Returns a formatted string detailing ID, name, amount, and frequency.
     */
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
     * @return Returns the resulting JSONObject, or null if an exception occurs.
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
            json.put(JsonKeys.IMAGE_PATH, imagePath);
            if (lastTakenTimestamp != null) {
                json.put(JsonKeys.LAST_TAKEN_TIMESTAMP, lastTakenTimestamp);
            }
            json.put(JsonKeys.IS_CRITICAL, isCritical);

            // Inventory
            json.put(JsonKeys.INVENTORY_CURRENT, (double) inventoryCurrent);
            json.put(JsonKeys.INVENTORY_THRESHOLD, (double) inventoryThreshold);
            if (inventoryAlertType != null) {
                json.put(JsonKeys.INVENTORY_ALERT_TYPE, inventoryAlertType.name());
            }

            if (form != null) json.put(JsonKeys.FORM, form.name());
            if (instruction != null) json.put(JsonKeys.INSTRUCTIONS, instruction.name());
            if (measurementUnit != null)
                json.put(JsonKeys.MEASUREMENT_UNIT, measurementUnit.name());
            json.put(JsonKeys.TIMES_IN_DAY, getTimesADayAsJsonArray());
        } catch (JSONException e) {
            return null;
        }
        return json;
    }

    /**
     * Converts the daily dose times array to a JSON array.
     *
     * @return Returns a JSONArray containing the formatted times.
     */
    private JSONArray getTimesADayAsJsonArray() {
        JSONArray array = new JSONArray();
        if (timesADay != null) {
            for (int i = 0; i < timesADay.size(); i++) {
                array.put(timesADay.valueAt(i).toString());
            }
        }
        return array;
    }

    /**
     * Converts the daily schedule of timestamps into a JSON array.
     *
     * @return Returns a JSONArray containing the epoch millisecond timestamps.
     */
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
        public static final String IMAGE_PATH = "mImagePath";
        public static final String LAST_TAKEN_TIMESTAMP = "mLastTakenTimestamp";
        public static final String IS_CRITICAL = "mIsCritical";
        public static final String INVENTORY_CURRENT = "mInventoryCurrent";
        public static final String INVENTORY_THRESHOLD = "mInventoryThreshold";
        public static final String INVENTORY_ALERT_TYPE = "mInventoryAlertType";
    }
}
