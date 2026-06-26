package com.robinzon.medicationwizard.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A lightweight utility class representing a time of day (hour and minute) without date context.
 * <p>
 * This class is used throughout the Medication Wizard to handle medication schedules.
 * It implements {@link Comparable} to allow chronological sorting and provides
 * robust JSON serialization support to handle legacy string formats and modern object formats.
 * </p>
 */
final public class SimpleDayTime implements Comparable<SimpleDayTime> {
    
    /** The hour of the day in 24-hour format (0-23). */
    public final byte hour;
    
    /** The minute of the hour (0-59). */
    public final byte minute;

    /**
     * Constructs a new SimpleDayTime with the specified hour and minute.
     *
     * @param hour   The hour of the day (0-23).
     * @param minute The minute of the hour (0-59).
     */
    public SimpleDayTime(byte hour, byte minute) {
        this.hour = hour;
        this.minute = minute;
    }

    /**
     * Copy constructor that creates a new SimpleDayTime from an existing instance.
     *
     * @param value The instance to copy from.
     */
    public SimpleDayTime(SimpleDayTime value) {
        this.hour = value.hour;
        this.minute = value.minute;
    }

    /**
     * @return The hour of the day (0-23).
     */
    public byte getHour() {
        return hour;
    }

    /**
     * @return The minute of the hour (0-59).
     */
    public byte getMinute() {
        return minute;
    }

    /**
     * Compares this time with another SimpleDayTime for chronological ordering.
     * Checks hours first, then minutes.
     *
     * @param other The other time to compare to.
     * @return A negative integer, zero, or a positive integer as this time 
     *         is earlier than, equal to, or later than the specified time.
     */
    @Override
    public int compareTo(@NonNull SimpleDayTime other) {
        if (this.hour != other.hour) {
            return Byte.compare(this.hour, other.hour);
        }
        return Byte.compare(this.minute, other.minute);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Equality is based on matching hour and minute values.
     *
     * @param o The reference object with which to compare.
     * @return {@code true} if this object is the same as the o argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleDayTime that = (SimpleDayTime) o;
        return hour == that.hour && minute == that.minute;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(hour, minute);
    }

    /**
     * Returns a string representation of the time in HH:mm format.
     * Uses the device's default locale for number formatting.
     *
     * @return A formatted string (e.g., "08:30").
     */
    @Override
    public String toString() {
        // ALWAYS use Locale.US for serialization to ensure ASCII digits are used.
        // Localized digits (e.g. Arabic-Indic) break Integer/Byte parsing.
        return String.format(java.util.Locale.US, "%02d:%02d", hour, minute);
    }

    /**
     * Robust factory method to create a SimpleDayTime from various JSON inputs.
     * Supports both modern {@link JSONObject} and legacy {@link String} formats (e.g., "12:00").
     *
     * @param obj The input object (expected to be a JSONObject or String).
     * @return A new SimpleDayTime instance, or {@code null} if parsing fails.
     */
    @Nullable
    public static SimpleDayTime fromJson(Object obj) {
        if (obj instanceof JSONObject) {
            JSONObject json = (JSONObject) obj;
            try {
                return new SimpleDayTime(
                        (byte) json.getInt("hour"),
                        (byte) json.getInt("minute")
                );
            } catch (JSONException e) {
                return null;
            }
        } else if (obj instanceof String) {
            String timeStr = (String) obj;
            try {
                String[] parts = timeStr.split(":");
                if (parts.length == 2) {
                    return new SimpleDayTime(
                            Byte.parseByte(parts[0].trim()),
                            Byte.parseByte(parts[1].trim())
                    );
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Serializes this time into a JSONObject for persistent storage.
     *
     * @return A JSONObject containing "hour" and "minute" keys.
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("hour", hour);
            json.put("minute", minute);
        } catch (JSONException ignored) {
        }
        return json;
    }
}