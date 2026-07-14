package com.robinzon.medicationwizard.ui.todaysmedications;

import com.robinzon.medicationwizard.database.DoseInstanceEntity;

import java.util.List;

/**
 * Base class for items displayed in the Today's Medications list.
 * Supports both single doses and grouped doses (same time).
 */
public abstract class DoseItem {
    public abstract long getScheduledTime();

    public abstract String getStatus();

    public static class Single extends DoseItem {
        public final DoseInstanceEntity entity;

        public Single(DoseInstanceEntity entity) {
            this.entity = entity;
        }

        @Override
        public long getScheduledTime() {
            return entity.getScheduledTime();
        }

        @Override
        public String getStatus() {
            return entity.getStatus();
        }
    }

    public static class Group extends DoseItem {
        public final List<DoseInstanceEntity> doses;

        public Group(List<DoseInstanceEntity> doses) {
            this.doses = doses;
        }

        @Override
        public long getScheduledTime() {
            return doses.get(0).getScheduledTime();
        }

        @Override
        public String getStatus() {
            String firstStatus = doses.get(0).getStatus();
            for (DoseInstanceEntity d : doses) {
                if (!d.getStatus().equals(firstStatus)) return "MIXED";
            }
            return firstStatus;
        }

        public String getMedicationNames() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < doses.size(); i++) {
                sb.append(doses.get(i).getMedicationName());
                if (i < doses.size() - 1) sb.append(", ");
            }
            return sb.toString();
        }
    }
}
