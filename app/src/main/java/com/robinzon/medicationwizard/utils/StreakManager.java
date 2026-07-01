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

    public interface StreakCallback {
        void onStreakCalculated(int streakCount);
    }

    /**
     * Calculates the current streak by checking historical data in the database.
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
                if (streak > 365 || Math.abs(System.currentTimeMillis() - cal.getTimeInMillis()) > 365L * 24 * 60 * 60 * 1000) {
                    break;
                }
                
                // If we've gone back more than 1 day without finding any meds, 
                // or we hit a broken day, the loop would have broken above.
                // We limit backtracking to avoid infinite loops if data is sparse.
            }
            
            callback.onStreakCalculated(streak);
        });
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
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
