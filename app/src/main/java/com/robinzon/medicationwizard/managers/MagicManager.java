package com.robinzon.medicationwizard.managers;

import android.content.Context;
import androidx.annotation.NonNull;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.database.DoseInstanceEntity;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.List;

/**
 * Manages the "Magics" virtual currency balance and transactions.
 */
public class MagicManager {

    private static MagicManager sInstance;
    private final Context mContext;

    private MagicManager(Context context) {
        this.mContext = context.getApplicationContext();
    }

    /**
     * Retrieves the singleton instance of the MagicManager.
     *
     * @param context The application or activity context used to initialize the manager.
     * @return The singleton MagicManager instance.
     */
    public static synchronized MagicManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MagicManager(context);
        }
        return sInstance;
    }

    /**
     * @return The current Magic balance.
     */
    /**
     * Retrieves the current balance of Magics for the user.
     *
     * @return The total available Magic balance.
     */
    public int getMagicBalance() {
        return SharedPreferencesManager.getInstance(mContext).getInt(AppConfig.KEY_MAGIC_BALANCE, 0);
    }

    /**
     * Adds Magics to the user's balance.
     * @param amount The number of Magics to add.
     */
    /**
     * Adds a specified amount of Magics to the user's balance.
     *
     * @param amount The number of Magics to add. Must be greater than zero.
     */
    public void addMagics(int amount) {
        if (amount <= 0) return;
        int current = getMagicBalance();
        SharedPreferencesManager.getInstance(mContext).setInt(AppConfig.KEY_MAGIC_BALANCE, current + amount);
    }

    /**
     * Subtracts Magics from the user's balance.
     * @param amount The number of Magics to spend.
     * @return True if the transaction was successful (sufficient funds).
     */
    /**
     * Deducts a specified amount of Magics from the user's balance if sufficient funds exist.
     *
     * @param amount The number of Magics to spend.
     * @return True if the transaction was successful, false if the balance was insufficient.
     */
    public boolean spendMagics(int amount) {
        int current = getMagicBalance();
        if (current >= amount) {
            SharedPreferencesManager.getInstance(mContext).setInt(AppConfig.KEY_MAGIC_BALANCE, current - amount);
            return true;
        }
        return false;
    }

    /**
     * Grants a bonus magic if the day is "Perfect" (all doses taken).
     * Ensures it's only granted once per day.
     */
    /**
     * Evaluates today's doses and grants a bonus Magic if all doses have been taken.
     * <p>
     * This logic ensures the bonus is only awarded once per day by tracking the last perfect day timestamp.
     * </p>
     *
     * @param todaysDoses A list containing all scheduled doses for the current day.
     * @return True if the perfect day bonus was newly granted, false otherwise.
     */
    public boolean checkAndGrantPerfectDayBonus(List<DoseInstanceEntity> todaysDoses) {
        if (todaysDoses == null || todaysDoses.isEmpty()) return false;

        for (DoseInstanceEntity dose : todaysDoses) {
            if (!"TAKEN".equals(dose.getStatus())) {
                return false;
            }
        }

        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(mContext);
        long lastPerfectDay = sp.getLong("magic_last_perfect_day", 0);
        long now = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();

        if (isNewDay(lastPerfectDay, now)) {
            addMagics(1);
            sp.setLong("magic_last_perfect_day", now);
            return true;
        }
        return false;
    }

    /**
     * Determines whether the current time represents a new calendar day compared to a previous time.
     *
     * @param lastTime The previous timestamp in milliseconds.
     * @param now      The current timestamp in milliseconds.
     * @return True if 'now' is a different calendar day than 'lastTime', false otherwise.
     */
    private boolean isNewDay(long lastTime, long now) {
        java.util.Calendar lastCal = java.util.Calendar.getInstance();
        lastCal.setTimeInMillis(lastTime);
        java.util.Calendar nowCal = java.util.Calendar.getInstance();
        nowCal.setTimeInMillis(now);

        return lastTime == 0 ||
                lastCal.get(java.util.Calendar.DAY_OF_YEAR) != nowCal.get(java.util.Calendar.DAY_OF_YEAR) ||
                lastCal.get(java.util.Calendar.YEAR) != nowCal.get(java.util.Calendar.YEAR);
    }
}
