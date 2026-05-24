package com.robinzon.medicationwizard.entities;

import androidx.annotation.NonNull;

import org.json.JSONObject;

public class MedicationInstance extends Medication {


    public enum Status {
        SCHEDULED,
        TAKEN,
        MISSED,
        SKIPPED
    }

    private Status status;
    private long scheduledTime;

    public MedicationInstance() {
        super();
    }

    public MedicationInstance(Medication medication) {
        super();
        if (medication != null) {
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

    public MedicationInstance(Status status, long scheduledTime) {
        super();
        this.status = status;
        this.scheduledTime = scheduledTime;
    }



    public long getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

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
