package com.robinzon.medicationwizard.utils;

final public class SimpleDayTime {
    public final byte hour;
    public final byte time;

    public SimpleDayTime(byte hour, byte time) {
        this.hour = hour;
        this.time = time;
    }

    public byte getHour() {
        return hour;
    }

    public byte getTime() {
        return time;
    }
}
