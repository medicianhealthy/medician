package com.robinzon.medicationwizard.utils;

public class TimeManager {

    private static TimeManager sInstance;

    private TimeManager() {
    }

    public static synchronized TimeManager getInstance() {
        if (null == sInstance) {
            sInstance = new TimeManager();
        }
        return sInstance;
    }

    //Milliseconds

    public long toMillisFromSeconds(final long seconds) {
        return seconds * 1000L;
    }

    public long toMillisFromMinutes(final int minutes) {
        return minutes * 60 * 1000L;
    }

    public long toMillisFromHours(final int hours) {
        return hours * 60 * 60 * 1000L;
    }

    public long toMillisFromDays(final int days) {
        return days * 24 * 60 * 60 * 1000L;
    }

    //Seconds
    public long toSecondsFromMillis(final long millis) {
        return millis / 1000L;
    }

    public long toSecondsFromMinutes(final float minutes) {
        return (long) (minutes * 60);
    }

    public long toSecondsFromHours(final float hours) {
        return (long) (hours * 60 * 60);
    }

    public long toSecondsFromDays(final int days) {
        return (long) days * 24 * 60 * 60;
    }


    //Minutes
    public float toMinutesFromMillis(final long millis) {
        return (float) millis / 1000L / 60L;
    }

    public long toMinutesFromHours(final float hours) {
        return (long) (hours * 60);
    }

}
