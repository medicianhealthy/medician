package com.robinzon.medicationwizard.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.robinzon.medicationwizard.entities.MedicationInstance;

@Entity(tableName = "dose_instances")
public class DoseInstanceEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String medicationId;
    private String medicationName;
    private float amount;
    private float strength;
    private String unit;
    private String form;
    private long scheduledTime;
    private long actionTime;
    private String status;
    private String instruction;

    public DoseInstanceEntity() {
    }

    // Converters / Helpers
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

    // Getters and Setters
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