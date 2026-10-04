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

    private Status status;
    private long scheduledTime;
    private int snoozeCount;

    /**
     * Constructs a default MedicationInstance for serialization.
     */
    public MedicationInstance() {
        super();
    }

    /**
     * Constructs a specific medication instance from a base medication definition.
     * <p>
     * Copies all base properties (name, strength, form, etc.) to ensure the instance
     * is self-contained.
     * </p>
     *
     * @param medication The base medication definition to copy properties from.
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
            this.setCritical(medication.isCritical());
        }
    }

    /**
     * Constructs a medication instance with explicit status and scheduled time.
     *
     * @param status        The current lifecycle status of the dose.
     * @param scheduledTime The planned execution time in epoch milliseconds.
     */
    public MedicationInstance(Status status, long scheduledTime) {
        super();
        this.status = status;
        this.scheduledTime = scheduledTime;
    }

    /**
     * Retrieves the planned execution time of the dose.
     *
     * @return Returns the planned execution time in epoch milliseconds.
     */
    public long getScheduledTime() {
        return scheduledTime;
    }

    /**
     * Updates the planned execution time of the dose.
     *
     * @param scheduledTime The planned execution time in epoch milliseconds.
     */
    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    /**
     * Retrieves the current lifecycle status of this dose.
     *
     * @return Returns the current status of the dose.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Updates the lifecycle status of this dose.
     *
     * @param status The new status of the dose.
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Retrieves the number of times this dose has been snoozed.
     *
     * @return Returns the snooze count.
     */
    public int getSnoozeCount() {
        return snoozeCount;
    }

    /**
     * Updates the snooze count for this dose.
     *
     * @param snoozeCount The new snooze count.
     */
    public void setSnoozeCount(int snoozeCount) {
        this.snoozeCount = snoozeCount;
    }

    /**
     * Serializes this medication instance to a JSONObject, including both base medication
     * details and instance-specific status and timing.
     *
     * @return Returns the resulting JSONObject containing the instance's data.
     */
    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        try {
            json.put("status", status.name());
            json.put("scheduledTime", scheduledTime);
            json.put("snoozeCount", snoozeCount);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Generates a human-readable string representation of the instance, including a
     * localized date and time string for easier debugging and logging.
     *
     * @return Returns a formatted string (e.g., "MedicationInstance{name='Aspirin', status=SCHEDULED, scheduledTime=Mon, May 17, 08:30}").
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

    /**
     * Represents the current state of a specific medication dose.
     */
    public enum Status {
        /**
         * The dose is planned for the future but hasn't occurred yet.
         */
        SCHEDULED,
        /**
         * The user has successfully taken the dose.
         */
        TAKEN,
        /**
         * The dose was missed (time passed without action).
         */
        MISSED,
        /**
         * The user explicitly chose not to take this specific dose.
         */
        SKIPPED
    }
}
