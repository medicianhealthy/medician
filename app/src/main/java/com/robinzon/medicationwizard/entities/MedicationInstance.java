package com.robinzon.medicationwizard.entities;

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

    public MedicationInstance(Status status, long scheduledTime) {
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


}
