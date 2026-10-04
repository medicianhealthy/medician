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

    /**
     * The hour of the day in 24-hour format (0-23).
     */
    public final byte hour;

    /**
     * The minute of the hour (0-59).
     */
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
     * Creates a new SimpleDayTime instance by copying from an existing instance.
     *
     * @param value The instance to copy from.
     */
    public SimpleDayTime(SimpleDayTime value) {
        this.hour = value.hour;
        this.minute = value.minute;
    }

    /**
     * Parses an object into a SimpleDayTime instance.
     * <p>
     * Robust factory method that supports parsing both modern {@link JSONObject} formats
     * and legacy {@link String} formats (e.g., "12:00").
     * </p>
     *
     * @param obj The input object to parse, expected to be a JSONObject or String.
     * @return A new SimpleDayTime instance, or {@code null} if parsing fails or input type is unsupported.
     */
    @Nullable
    public static SimpleDayTime fromJson(Object obj) {
        if (obj instanceof JSONObject json) {
            try {
                return new SimpleDayTime(
                        (byte) json.getInt("hour"),
                        (byte) json.getInt("minute")
                );
            } catch (JSONException e) {
                return null;
            }
        } else if (obj instanceof String timeStr) {
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
     * Retrieves the hour component of this time.
     *
     * @return The hour of the day (0-23).
     */
    public byte getHour() {
        return hour;
    }

    /**
     * Retrieves the minute component of this time.
     *
     * @return The minute of the hour (0-59).
     */
    public byte getMinute() {
        return minute;
    }

    /**
     * Compares this time chronologically with another SimpleDayTime instance.
     * <p>
     * Checks hours first, then evaluates minutes if the hours are identical.
     * </p>
     *
     * @param other The other time to compare against. Must not be null.
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
     * Determines whether another object is "equal to" this time instance.
     * <p>
     * Equality is strictly based on matching both the hour and minute values.
     * </p>
     *
     * @param o The reference object with which to compare.
     * @return {@code true} if this object represents the same time as the argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleDayTime that = (SimpleDayTime) o;
        return hour == that.hour && minute == that.minute;
    }

    /**
     * Generates a hash code value for this time object.
     *
     * @return A hash code based on the hour and minute values.
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(hour, minute);
    }

    /**
     * Formats this time as a standard 24-hour string (e.g., "08:30").
     * <p>
     * Specifically utilizes {@link java.util.Locale#US} for formatting to guarantee ASCII digits,
     * which prevents parsing failures caused by localized digits (like Arabic-Indic numerals).
     * </p>
     *
     * @return A formatted time string in HH:mm format.
     */
    @Override
    public String toString() {
        // ALWAYS use Locale.US for serialization to ensure ASCII digits are used.
        // Localized digits (e.g. Arabic-Indic) break Integer/Byte parsing.
        return String.format(java.util.Locale.US, "%02d:%02d", hour, minute);
    }

    /**
     * Serializes this time instance into a standard JSON object.
     *
     * @return A {@link JSONObject} mapping "hour" and "minute" to their respective values.
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
