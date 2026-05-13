package com.robinzon.medicationwizard.utils;

import androidx.annotation.NonNull;

final public class SimpleDayTime implements Comparable<SimpleDayTime> {
    public final byte hour;
    public final byte minute;

    public SimpleDayTime(byte hour, byte time) {
        this.hour = hour;
        this.minute = time;
    }

    public SimpleDayTime(SimpleDayTime value) {
        this.hour = value.hour;
        this.minute = value.minute;
    }

    public byte getHour() {
        return hour;
    }

    public byte getMinute() {
        return minute;
    }

    @Override
    public int compareTo(@NonNull SimpleDayTime other) {
        if (this.hour != other.hour) {
            return Byte.compare(this.hour, other.hour);
        }
        return Byte.compare(this.minute, other.minute);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleDayTime that = (SimpleDayTime) o;
        return hour == that.hour && minute == that.minute;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(hour, minute);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute);
    }
}
