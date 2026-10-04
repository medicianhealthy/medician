package com.robinzon.medicationwizard.utils;

import android.content.Context;

import com.robinzon.medicationwizard.database.AppDatabase;

import java.util.Calendar;

/**
 * Utility class to calculate and manage user health streaks.
 * <p>
 * A "Streak" is defined as the number of consecutive days (ending yesterday or today)
 * where 100% of scheduled medication doses were marked as 'TAKEN'.
 * </p>
 */
public class StreakManager {

    /**
     * Calculates the current health streak by chronologically examining historical doses in the database.
     * <p>
     * Iterates backwards from today up to a maximum of one year to ensure performant execution.
     * It checks whether all scheduled doses for a day were marked as completed. A day completely devoid
     * of scheduled medications does not break the streak. The process evaluates the streak status
     * asynchronously.
     * </p>
     *
     * @param context  The application context to access the database. Must not be null.
     * @param callback The callback invoked with the final streak count. Must not be null.
     */
    public static void calculateCurrentStreak(Context context, StreakCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int streak = 0;
            Calendar cal = Calendar.getInstance();

            // Start checking from today backwards
            while (true) {
                long startOfDay = getStartOfDay(cal);
                long endOfDay = getEndOfDay(cal);

                // Fetch doses that were scheduled to occur up to 'now'
                // (don't count future doses for today's 'perfection' check yet)
                int totalDosesCount = AppDatabase.getDatabase(context).doseInstanceDao().getInstancesInRangeInternal(startOfDay, endOfDay).size();

                if (totalDosesCount > 0) {
                    // How many of these doses were actually taken?
                    int unfinished = AppDatabase.getDatabase(context).doseInstanceDao().getUnfinishedDosesCount(startOfDay, endOfDay);

                    if (unfinished == 0) {
                        // All doses for this day were taken!
                        streak++;
                    } else {
                        // This day is not "perfect".
                        // If it's TODAY, we don't break the streak yet (they might still take them).
                        // If it's YESTERDAY or earlier, the streak is officially broken.
                        boolean isToday = isSameDay(cal, Calendar.getInstance());
                        if (!isToday) {
                            break;
                        }
                        // If it's today and unfinished, we just continue to check yesterday
                        // to see the existing streak.
                    }
                } else {
                    // Day with no meds doesn't break a streak, but doesn't increment it.
                    // (e.g. if they finished a 3-day streak and today has no meds, it stays 3).
                }

                // Move to previous day
                cal.add(Calendar.DAY_OF_YEAR, -1);

                // Safety break: don't check more than a year
                if (streak > 365 || Math.abs(com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal() - cal.getTimeInMillis()) > 365L * 24 * 60 * 60 * 1000) {
                    break;
                }

                // If we've gone back more than 1 day without finding any meds,
                // or we hit a broken day, the loop would have broken above.
                // We limit backtracking to avoid infinite loops if data is sparse.
            }

            callback.onStreakCalculated(streak);
        });
    }

    /**
     * Determines whether two Calendar instances represent the exact same calendar day.
     *
     * @param cal1 The first calendar to compare.
     * @param cal2 The second calendar to compare.
     * @return True if both calendars fall on the same day of the same year, false otherwise.
     */
    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Computes the timestamp for the very beginning of a specified calendar day.
     *
     * @param cal The source calendar day.
     * @return The epoch time in milliseconds corresponding to 00:00:00.000 of the given day.
     */
    private static long getStartOfDay(Calendar cal) {
        Calendar temp = (Calendar) cal.clone();
        temp.set(Calendar.HOUR_OF_DAY, 0);
        temp.set(Calendar.MINUTE, 0);
        temp.set(Calendar.SECOND, 0);
        temp.set(Calendar.MILLISECOND, 0);
        return temp.getTimeInMillis();
    }

    /**
     * Computes the timestamp for the very end of a specified calendar day.
     *
     * @param cal The source calendar day.
     * @return The epoch time in milliseconds corresponding to 23:59:59.999 of the given day.
     */
    private static long getEndOfDay(Calendar cal) {
        Calendar temp = (Calendar) cal.clone();
        temp.set(Calendar.HOUR_OF_DAY, 23);
        temp.set(Calendar.MINUTE, 59);
        temp.set(Calendar.SECOND, 59);
        temp.set(Calendar.MILLISECOND, 999);
        return temp.getTimeInMillis();
    }

    /**
     * Interface definition for a callback invoked upon completion of streak calculations.
     */
    public interface StreakCallback {
        /**
         * Called when the current streak count has been fully calculated.
         *
         * @param streakCount The number of consecutive days of full adherence.
         */
        void onStreakCalculated(int streakCount);
    }
}
