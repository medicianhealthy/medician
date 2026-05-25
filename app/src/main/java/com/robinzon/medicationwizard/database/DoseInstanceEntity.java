package com.robinzon.medicationwizard.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.robinzon.medicationwizard.entities.MedicationInstance;

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

    /** Unique primary key for the database record. */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /** Unique ID of the parent medication (from SharedPreferences). */
    private String medicationId;
    
    /** Name of the medication at the time of scheduling. */
    private String medicationName;
    
    /** The dose amount to be taken (e.g., 2.0). */
    private float amount;
    
    /** The strength of the medication (e.g., 500.0). */
    private float strength;
    
    /** The measurement unit (e.g., "mg", "mL"). */
    private String unit;
    
    /** The physical form of the drug (e.g., "Pill", "Drops"). */
    private String form;
    
    /** The planned execution time (epoch milliseconds). */
    private long scheduledTime;
    
    /** The actual time the user interacted with this dose (epoch milliseconds). */
    private long actionTime;
    
    /** Current status: SCHEDULED, TAKEN, MISSED, or SKIPPED. */
    private String status;
    
    /** Casual instructions (e.g., "After eating"). */
    private String instruction;

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
        return entity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMedicationId() { return medicationId; }
    public void setMedicationId(String medicationId) { this.medicationId = medicationId; }

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount = amount; }

    public float getStrength() { return strength; }
    public void setStrength(float strength) { this.strength = strength; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }

    public long getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(long scheduledTime) { this.scheduledTime = scheduledTime; }

    public long getActionTime() { return actionTime; }
    public void setActionTime(long actionTime) { this.actionTime = actionTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
}