package com.robinzon.medicationwizard.ui.todaysmedications;

import com.robinzon.medicationwizard.database.DoseInstanceEntity;

import java.util.List;

/**
 * Base class for items displayed in the Today's Medications list.
 * Supports both single doses and grouped doses (same time).
 */
public abstract class DoseItem {
    /**
     * Retrieves the scheduled time for the dose item.
     *
     * @return The scheduled time in milliseconds since the epoch.
     */
    public abstract long getScheduledTime();

    /**
     * Retrieves the current status of the dose item.
     *
     * @return The status string (e.g., "TAKEN", "SKIPPED", "PENDING", or "MIXED" for groups).
     */
    public abstract String getStatus();

    /**
     * Represents a single dose instance in the Today's Medications list.
     */
    public static class Single extends DoseItem {
        public final DoseInstanceEntity entity;

        /**
         * Constructs a Single DoseItem wrapping the provided dose instance.
         *
         * @param entity The underlying dose instance entity. Must not be null.
         */
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

    /**
     * Represents a grouped set of dose instances scheduled for the same time.
     */
    public static class Group extends DoseItem {
        public final List<DoseInstanceEntity> doses;

        /**
         * Constructs a Group DoseItem wrapping multiple dose instances.
         *
         * @param doses A list of dose instance entities. Must not be empty or null.
         */
        public Group(List<DoseInstanceEntity> doses) {
            this.doses = doses;
        }

        /**
         * Retrieves the scheduled time for the grouped doses.
         *
         * @return The scheduled time in milliseconds since the epoch from the first dose in the group.
         */
        @Override
        public long getScheduledTime() {
            return doses.get(0).getScheduledTime();
        }

        /**
         * Computes the aggregated status of the grouped doses.
         *
         * @return The status if all doses share the same status, otherwise returns "MIXED".
         */
        @Override
        public String getStatus() {
            String firstStatus = doses.get(0).getStatus();
            for (DoseInstanceEntity d : doses) {
                if (!d.getStatus().equals(firstStatus)) return "MIXED";
            }
            return firstStatus;
        }

        /**
         * Generates a comma-separated list of medication names in the group.
         *
         * @return A string containing all medication names.
         */
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
