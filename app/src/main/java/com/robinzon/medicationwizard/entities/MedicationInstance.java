package com.robinzon.medicationwizard.entities;

import androidx.annotation.NonNull;

import org.json.JSONObject;

/**
 * Represents a domain-level occurrence of a medication dose.
 * <p>
 * This class extends the base {@link Medication} to include status and timing information
 * specific to a single instance (e.g., "The dose of Aspirin at 8 AM on Monday").
 * It is primarily used to pass data between the UI and the persistence layers.
 * </p>
 */
public class MedicationInstance extends Medication {

    /**
     * Represents the current state of a specific medication dose.
     */
    public enum Status {
        /** The dose is planned for the future but hasn't occurred yet. */
        SCHEDULED,
        /** The user has successfully taken the dose. */
        TAKEN,
        /** The dose was missed (time passed without action). */
        MISSED,
        /** The user explicitly chose not to take this specific dose. */
        SKIPPED
    }

    private Status status;
    private long scheduledTime;

    /**
     * Default constructor for serialization.
     */
    public MedicationInstance() {
        super();
    }

    /**
     * "Upgrade" constructor that creates a specific instance from a medication definition.
     * Copies all base properties (name, strength, form, etc.) to ensure the instance 
     * is self-contained.
     *
     * @param medication The base medication definition.
     */
    public MedicationInstance(Medication medication) {
        super();
        if (medication != null) {
            this.setId(medication.getId());
            this.setCommercialName(medication.getCommercialName());
            this.setAmount(medication.getAmount());
            this.setForm(medication.getForm());
            this.setDailyFrequency(medication.getDailyFrequency());
            this.setStrength(medication.getStrength());
            this.setMedicalCondition(medication.getMedicalCondition());
            this.setDailySchedule(medication.getDailySchedule());
            this.setAmountLeft(medication.getAmountLeft());
            this.setInstruction(medication.getInstruction());
            this.setMeasurementUnit(medication.getMeasurementUnit());
            if (medication.getTimesADay() != null) {
                this.addTimeStampsForDay(medication.getTimesADay());
            }
        }
    }

    /**
     * Constructs an instance with explicit status and time.
     *
     * @param status        Current status of the dose.
     * @param scheduledTime Planned execution time (epoch millis).
     */
    public MedicationInstance(Status status, long scheduledTime) {
        super();
        this.status = status;
        this.scheduledTime = scheduledTime;
    }

    /**
     * @return The planned execution time in epoch milliseconds.
     */
    public long getScheduledTime() {
        return scheduledTime;
    }

    /**
     * @param scheduledTime The planned execution time in epoch milliseconds.
     */
    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    /**
     * @return The current lifecycle status of this dose.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @param status The current lifecycle status of this dose.
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Serializes this instance to a JSONObject, including both base medication
     * details and instance-specific status/timing.
     *
     * @return The resulting JSONObject.
     */
    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        try {
            json.put("status", status.name());
            json.put("scheduledTime", scheduledTime);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Returns a human-readable representation of the instance, including a 
     * localized date and time string for easier debugging and logging.
     *
     * @return Formatted string (e.g., "MedicationInstance{name='Aspirin', status=SCHEDULED, scheduledTime=Mon, May 17, 08:30}").
     */
    @Override
    @NonNull
    public String toString() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, MMM d, HH:mm", java.util.Locale.getDefault());
        String readableDate = sdf.format(new java.util.Date(scheduledTime));
        return "MedicationInstance{" +
                "name='" + getCommercialName() + '\'' +
                ", status=" + status +
                ", scheduledTime=" + readableDate +
                '}';
    }
}