package com.robinzon.medicationwizard.utils;

import java.lang.ref.WeakReference;

public class TimeManager {

    public static WeakReference<TimeManager> sInstance;

    private TimeManager() {
    }

    public static TimeManager getInstance(){
        if (null == sInstance || null == sInstance.get()){
            final TimeManager timeManager = new TimeManager();
            sInstance = new WeakReference<>(timeManager);
        }
        return sInstance.get();
    }

    //Milliseconds

    public long toMillisFromSeconds(final long seconds){
        return seconds * 1000L;
    }

    public long toMillisFromMinutes(final int minutes){
        return minutes * 60 * 1000L;
    }

    public long toMillisFromHours(final int hours){
        return hours * 60 * 60 * 1000L;
    }

    public long toMillisFromDays(final int days){
        return days * 24 * 60 * 60 * 1000L;
    }

    //Seconds
    public long toSecondsFromMillis(final long millis){
        return millis / 1000L;
    }

    public long toSecondsFromMinutes(final float minutes){
        return (long) (minutes * 60);
    }

    public long toSecondsFromHours(final float hours){
        return (long) (hours * 60 * 60);
    }

    public long toSecondsFromDays(final int days){
        return (long) days * 24 * 60 * 60;
    }
}
