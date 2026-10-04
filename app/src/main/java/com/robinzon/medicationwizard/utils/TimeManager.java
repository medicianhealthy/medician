package com.robinzon.medicationwizard.utils;

/**
 * Singleton utility class designed for time manipulation, translation, and spoofing.
 * <p>
 * This manager provides standardized converters for various time units and handles
 * optional "cheat" behaviors used in testing and debugging by overlaying a fake timeline.
 * </p>
 */
public class TimeManager {

    /**
     * SharedPreferences key for storing the timestamp of the start of the fake timeline.
     */
    public static final String KEY_CHEAT_FAKE_TIME_START = "cheat_fake_time_start";
    /**
     * SharedPreferences key for storing the actual system time when the cheat was configured.
     */
    public static final String KEY_CHEAT_REAL_TIME_AT_SET = "cheat_real_time_at_set";

    private static TimeManager sInstance;

    private TimeManager() {
    }

    /**
     * Retrieves the singleton instance of TimeManager.
     *
     * @return The singleton TimeManager instance.
     */
    public static synchronized TimeManager getInstance() {
        if (null == sInstance) {
            sInstance = new TimeManager();
        }
        return sInstance;
    }

    /**
     * Provides the current contextual system time in milliseconds.
     * <p>
     * If a fake time cheat is active within SharedPreferences, it calculates the simulated current time
     * by incrementing the fake start time by the real-time elapsed since the cheat was initiated.
     * </p>
     *
     * @return The active contextual time in milliseconds.
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
     * Provides the true, unmanipulated system time in milliseconds.
     *
     * @return The actual system epoch time in milliseconds, bypassing any configured cheats.
     */
    public long getRealTimeInMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Converts a duration from seconds to milliseconds.
     *
     * @param seconds The duration in seconds.
     * @return The equivalent duration in milliseconds.
     */
    public long toMillisFromSeconds(final long seconds) {
        return seconds * 1000L;
    }

    /**
     * Converts a duration from minutes to milliseconds.
     *
     * @param minutes The duration in minutes.
     * @return The equivalent duration in milliseconds.
     */
    public long toMillisFromMinutes(final int minutes) {
        return minutes * 60 * 1000L;
    }

    /**
     * Converts a duration from hours to milliseconds.
     *
     * @param hours The duration in hours.
     * @return The equivalent duration in milliseconds.
     */
    public long toMillisFromHours(final int hours) {
        return hours * 60 * 60 * 1000L;
    }

    /**
     * Converts a duration from days to milliseconds.
     *
     * @param days The duration in days.
     * @return The equivalent duration in milliseconds.
     */
    public long toMillisFromDays(final int days) {
        return days * 24 * 60 * 60 * 1000L;
    }

    /**
     * Converts a duration from milliseconds to seconds.
     *
     * @param millis The duration in milliseconds.
     * @return The equivalent duration in seconds.
     */
    public long toSecondsFromMillis(final long millis) {
        return millis / 1000L;
    }

    /**
     * Converts a duration from minutes to seconds.
     *
     * @param minutes The duration in minutes.
     * @return The equivalent duration in seconds.
     */
    public long toSecondsFromMinutes(final float minutes) {
        return (long) (minutes * 60);
    }

    /**
     * Converts a duration from hours to seconds.
     *
     * @param hours The duration in hours.
     * @return The equivalent duration in seconds.
     */
    public long toSecondsFromHours(final float hours) {
        return (long) (hours * 60 * 60);
    }

    /**
     * Converts a duration from days to seconds.
     *
     * @param days The duration in days.
     * @return The equivalent duration in seconds.
     */
    public long toSecondsFromDays(final int days) {
        return (long) days * 24 * 60 * 60;
    }

    /**
     * Converts a duration from milliseconds to minutes.
     *
     * @param millis The duration in milliseconds.
     * @return The equivalent duration in minutes as a floating-point value.
     */
    public float toMinutesFromMillis(final long millis) {
        return (float) millis / 1000L / 60L;
    }

    /**
     * Converts a duration from hours to minutes.
     *
     * @param hours The duration in hours.
     * @return The equivalent duration in minutes.
     */
    public long toMinutesFromHours(final float hours) {
        return (long) (hours * 60);
    }

}
