package com.robinzon.medicationwizard.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.robinzon.medicationwizard.entities.MedicationInstance;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single specific dose of medication as a table row in the Room database.
 * <p>
 * This entity captures a "point-in-time" snapshot of a medication reminder. It stores
 * both the planned schedule and the user's eventual action (taken, skipped, etc.).
 * Storing a snapshot ensures that historical records remain accurate even if the
 * parent medication's definition is later modified or deleted.
 * </p>
 */
@Entity(tableName = "dose_instances")
public class DoseInstanceEntity {

    /**
     * Unique primary key for the database record.
     */
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    /**
     * Unique ID of the parent medication (from SharedPreferences).
     */
    private String medicationId;

    /**
     * Name of the medication at the time of scheduling.
     */
    private String medicationName;

    /**
     * The dose amount to be taken (e.g., 2.0).
     */
    private float amount;

    /**
     * The strength of the medication (e.g., 500.0).
     */
    private float strength;

    /**
     * The measurement unit (e.g., "mg", "mL").
     */
    private String unit;

    /**
     * The physical form of the drug (e.g., "Pill", "Drops").
     */
    private String form;

    /**
     * The planned execution time (epoch milliseconds).
     */
    private long scheduledTime;

    /**
     * The actual time the user interacted with this dose (epoch milliseconds).
     */
    private long actionTime;

    /**
     * Current status: SCHEDULED, TAKEN, MISSED, or SKIPPED.
     */
    private String status;

    /**
     * Casual instructions (e.g., "After eating").
     */
    private String instruction;

    /**
     * Number of times this specific dose has been snoozed.
     */
    private int snoozeCount;

    /**
     * Path to the medication photo in internal storage.
     */
    private String imagePath;

    /**
     * Whether this dose belongs to an "As Needed" (PRN) medication.
     */
    private boolean isPrn;

    /**
     * Whether this dose is marked as "Critical".
     */
    private boolean isCritical;

    /**
     * Initializes a new, empty dose instance entity.
     * Required by Room for instantiation.
     */
    public DoseInstanceEntity() {
    }

    /**
     * Converts a domain-level {@link MedicationInstance} into a database-ready entity.
     * This factory method maps the domain object properties to database columns for persistence.
     *
     * @param instance The instance object containing domain logic and data. Must not be null.
     * @return Returns a populated {@link DoseInstanceEntity} ready for database insertion.
     */
    public static DoseInstanceEntity fromInstance(MedicationInstance instance) {
        DoseInstanceEntity entity = new DoseInstanceEntity();
        entity.medicationId = instance.getId();
        entity.medicationName = instance.getCommercialName();
        entity.amount = instance.getAmount();
        entity.strength = instance.getStrength();
        entity.unit = instance.getMeasurementUnit() != null ? instance.getMeasurementUnit().getName() : null;
        entity.form = instance.getForm() != null ? instance.getForm().name() : null;
        entity.scheduledTime = instance.getScheduledTime();
        entity.status = instance.getStatus() != null ? instance.getStatus().name() : null;
        entity.instruction = instance.getInstruction() != null ? instance.getInstruction().name() : null;
        entity.snoozeCount = instance.getSnoozeCount();
        entity.isPrn = (instance.getDailyFrequency() == 0);
        entity.isCritical = instance.isCritical();
        return entity;
    }

    /**
     * Deserializes a {@link DoseInstanceEntity} from a JSON representation.
     * Useful for restoring backups or processing network responses.
     *
     * @param json The JSONObject containing the entity's data.
     * @return Returns a new {@link DoseInstanceEntity}, or null if the input JSON is null.
     */
    public static DoseInstanceEntity fromJson(JSONObject json) {
        if (json == null) return null;
        DoseInstanceEntity entity = new DoseInstanceEntity();
        entity.medicationId = json.optString("medicationId");
        entity.medicationName = json.optString("medicationName");
        entity.amount = (float) json.optDouble("amount");
        entity.strength = (float) json.optDouble("strength");
        entity.unit = json.isNull("unit") ? null : json.optString("unit");
        entity.form = json.isNull("form") ? null : json.optString("form");
        entity.scheduledTime = json.optLong("scheduledTime");
        entity.actionTime = json.optLong("actionTime");
        entity.status = json.optString("status");
        entity.instruction = json.isNull("instruction") ? null : json.optString("instruction");
        entity.snoozeCount = json.optInt("snoozeCount");
        entity.isPrn = json.optBoolean("isPrn");
        entity.isCritical = json.optBoolean("isCritical");
        return entity;
    }

    /**
     * Retrieves the primary key ID.
     *
     * @return Returns the integer ID of the entity.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the primary key ID.
     *
     * @param id The new integer ID to assign to the entity.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Retrieves the parent medication's unique ID.
     *
     * @return Returns the medication ID string.
     */
    public String getMedicationId() {
        return medicationId;
    }

    /**
     * Sets the parent medication's unique ID.
     *
     * @param medicationId The medication ID string.
     */
    public void setMedicationId(String medicationId) {
        this.medicationId = medicationId;
    }

    /**
     * Retrieves the medication's name.
     *
     * @return Returns the medication name.
     */
    public String getMedicationName() {
        return medicationName;
    }

    /**
     * Sets the medication's name.
     *
     * @param medicationName The new medication name.
     */
    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    /**
     * Retrieves the dose amount.
     *
     * @return Returns the dose amount.
     */
    public float getAmount() {
        return amount;
    }

    /**
     * Sets the dose amount.
     *
     * @param amount The new dose amount.
     */
    public void setAmount(float amount) {
        this.amount = amount;
    }

    /**
     * Retrieves the strength of the medication.
     *
     * @return Returns the strength value.
     */
    public float getStrength() {
        return strength;
    }

    /**
     * Sets the strength of the medication.
     *
     * @param strength The new strength value.
     */
    public void setStrength(float strength) {
        this.strength = strength;
    }

    /**
     * Retrieves the measurement unit.
     *
     * @return Returns the unit (e.g., "mg").
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Sets the measurement unit.
     *
     * @param unit The measurement unit.
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Retrieves the physical form of the medication.
     *
     * @return Returns the form (e.g., "Pill").
     */
    public String getForm() {
        return form;
    }

    /**
     * Sets the physical form of the medication.
     *
     * @param form The physical form.
     */
    public void setForm(String form) {
        this.form = form;
    }

    /**
     * Retrieves the planned execution time.
     *
     * @return Returns the scheduled time in epoch milliseconds.
     */
    public long getScheduledTime() {
        return scheduledTime;
    }

    /**
     * Sets the planned execution time.
     *
     * @param scheduledTime The scheduled time in epoch milliseconds.
     */
    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    /**
     * Retrieves the time the user acted on this dose.
     *
     * @return Returns the action time in epoch milliseconds.
     */
    public long getActionTime() {
        return actionTime;
    }

    /**
     * Sets the time the user acted on this dose.
     *
     * @param actionTime The action time in epoch milliseconds.
     */
    public void setActionTime(long actionTime) {
        this.actionTime = actionTime;
    }

    /**
     * Retrieves the current status.
     *
     * @return Returns the status string (e.g., "TAKEN").
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current status.
     *
     * @param status The status string.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Retrieves the casual instructions for this dose.
     *
     * @return Returns the instruction string.
     */
    public String getInstruction() {
        return instruction;
    }

    /**
     * Sets the casual instructions for this dose.
     *
     * @param instruction The instruction string.
     */
    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    /**
     * Retrieves the number of times this dose was snoozed.
     *
     * @return Returns the snooze count.
     */
    public int getSnoozeCount() {
        return snoozeCount;
    }

    /**
     * Sets the number of times this dose was snoozed.
     *
     * @param snoozeCount The new snooze count.
     */
    public void setSnoozeCount(int snoozeCount) {
        this.snoozeCount = snoozeCount;
    }

    /**
     * Retrieves the file path to the medication's image.
     *
     * @return Returns the absolute image path.
     */
    public String getImagePath() {
        return imagePath;
    }

    /**
     * Sets the file path to the medication's image.
     *
     * @param imagePath The absolute image path.
     */
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    /**
     * Checks if this is an "As Needed" (PRN) dose.
     *
     * @return Returns true if PRN, false otherwise.
     */
    public boolean isPrn() {
        return isPrn;
    }

    /**
     * Sets whether this is an "As Needed" (PRN) dose.
     *
     * @param prn True if PRN, false otherwise.
     */
    public void setPrn(boolean prn) {
        isPrn = prn;
    }

    /**
     * Checks if this dose is marked as critical.
     *
     * @return Returns true if critical, false otherwise.
     */
    public boolean isCritical() {
        return isCritical;
    }

    /**
     * Sets whether this dose is marked as critical.
     *
     * @param critical True if critical, false otherwise.
     */
    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    /**
     * Serializes this entity into a JSONObject.
     * This is primarily used for creating backup files.
     *
     * @return Returns a populated {@link JSONObject}, or null if an error occurs during serialization.
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("medicationId", medicationId);
            json.put("medicationName", medicationName);
            json.put("amount", (double) amount);
            json.put("strength", (double) strength);
            json.put("unit", unit);
            json.put("form", form);
            json.put("scheduledTime", scheduledTime);
            json.put("actionTime", actionTime);
            json.put("status", status);
            json.put("instruction", instruction);
            json.put("snoozeCount", snoozeCount);
            json.put("isPrn", isPrn);
            json.put("isCritical", isCritical);
        } catch (JSONException e) {
            return null;
        }
        return json;
    }
}