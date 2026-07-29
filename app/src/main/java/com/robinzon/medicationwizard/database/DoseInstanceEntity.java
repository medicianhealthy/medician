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
     * Empty constructor required by Room.
     */
    public DoseInstanceEntity() {
    }

    /**
     * Factory method to convert a domain-level {@link MedicationInstance} into a database-ready entity.
     *
     * @param instance The instance object containing domain logic and data.
     * @return A populated entity object for storage.
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
        return entity;
    }

    /**
     * Creates a DoseInstanceEntity from a JSONObject.
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
        return entity;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(String medicationId) {
        this.medicationId = medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public float getStrength() {
        return strength;
    }

    public void setStrength(float strength) {
        this.strength = strength;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public long getActionTime() {
        return actionTime;
    }

    public void setActionTime(long actionTime) {
        this.actionTime = actionTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public int getSnoozeCount() {
        return snoozeCount;
    }

    public void setSnoozeCount(int snoozeCount) {
        this.snoozeCount = snoozeCount;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    /**
     * Serializes this entity into a JSONObject for backup purposes.
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
        } catch (JSONException e) {
            return null;
        }
        return json;
    }
}