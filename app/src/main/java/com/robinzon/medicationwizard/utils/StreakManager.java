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
     * Interface to receive the result of a streak calculation.
     */
    public interface StreakCallback {
        void onStreakCalculated(int streakCount);
    }

    /**
     * Calculates the current streak by checking historical data in the database.
     * <p>
     * Performance: Runs on a background thread via the database executor to avoid 
     * blocking the main UI thread.
     * </p>
     *
     * @param context  Application context.
     * @param callback Callback to return the final count.
     */
    public static void calculateCurrentStreak(Context context, StreakCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int streak = 0;
            Calendar cal = Calendar.getInstance();
            
            // Start checking from today backwards
            while (true) {
                long startOfDay = getStartOfDay(cal);
                long endOfDay = getEndOfDay(cal);
                
                // If the day has no medications, we skip it and continue the streak
                // (e.g. weekends with no meds shouldn't break a streak)
                int totalDoses = AppDatabase.getDatabase(context).doseInstanceDao().getInstancesInRangeInternal(startOfDay, endOfDay).size();
                
                if (totalDoses > 0) {
                    int unfinished = AppDatabase.getDatabase(context).doseInstanceDao().getUnfinishedDosesCount(startOfDay, endOfDay);
                    if (unfinished == 0) {
                        streak++;
                    } else {
                        // A day with meds that weren't all taken breaks the streak
                        break;
                    }
                } else if (streak == 0) {
                    // If today has no meds and we haven't found any taken meds yet, 
                    // just keep going back to find the start.
                } else {
                    // Day with no meds doesn't break an existing streak.
                }

                // Move to previous day
                cal.add(Calendar.DAY_OF_YEAR, -1);
                
                // Safety break: don't check more than a year
                if (streak > 365) break;
            }
            
            callback.onStreakCalculated(streak);
        });
    }

    private static long getStartOfDay(Calendar cal) {
        Calendar temp = (Calendar) cal.clone();
        temp.set(Calendar.HOUR_OF_DAY, 0);
        temp.set(Calendar.MINUTE, 0);
        temp.set(Calendar.SECOND, 0);
        temp.set(Calendar.MILLISECOND, 0);
        return temp.getTimeInMillis();
    }

    private static long getEndOfDay(Calendar cal) {
        Calendar temp = (Calendar) cal.clone();
        temp.set(Calendar.HOUR_OF_DAY, 23);
        temp.set(Calendar.MINUTE, 59);
        temp.set(Calendar.SECOND, 59);
        temp.set(Calendar.MILLISECOND, 999);
        return temp.getTimeInMillis();
    }
}
