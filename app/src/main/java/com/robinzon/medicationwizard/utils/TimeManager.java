package com.robinzon.medicationwizard.utils;

public class TimeManager {

    public static final String KEY_CHEAT_FAKE_TIME_START = "cheat_fake_time_start";
    public static final String KEY_CHEAT_REAL_TIME_AT_SET = "cheat_real_time_at_set";

    private static TimeManager sInstance;

    private TimeManager() {
    }

    public static synchronized TimeManager getInstance() {
        if (null == sInstance) {
            sInstance = new TimeManager();
        }
        return sInstance;
    }

    /**
     * @return The current time in milliseconds. If a fake time cheat is active, 
     * returns the fake time incremented by the real-time elapsed since it was set.
     */
    public long getCurrentTimeInMillisFakeOrReal() {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(com.robinzon.medicationwizard.MedicationWizardApplication.getContext());
        long fakeStart = prefs.getLong(KEY_CHEAT_FAKE_TIME_START, 0);
        long realAtSet = prefs.getLong(KEY_CHEAT_REAL_TIME_AT_SET, 0);

        if (fakeStart == 0) {
            return System.currentTimeMillis();
        }

        long elapsedRealTime = System.currentTimeMillis() - realAtSet;
        return fakeStart + elapsedRealTime;
    }

    /**
     * @return The actual system time in milliseconds, bypassing any cheats.
     */
    public long getRealTimeInMillis() {
        return System.currentTimeMillis();
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
