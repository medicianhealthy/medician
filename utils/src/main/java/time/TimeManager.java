package time;

import java.lang.ref.WeakReference;

import misc.SharedPreferencesManager;

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

    public long toMillisFromSeconds(final short seconds){
        return seconds * 1000L;
    }

    public long toMillisFromMinutes(final byte minutes){
        return minutes * 60 * 1000L;
    }

    public long toMillisFromHours(final byte hours){
        return hours * 60 * 60 * 1000L;
    }

    public long toMillisFromDays(final byte days){
        return days * 24 * 60 * 60 * 1000L;
    }
}
