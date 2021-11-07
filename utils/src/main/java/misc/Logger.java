package misc;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONStringer;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class Logger {


    public static final String SHARED_PREFERENCES = "Shared_preferences";
    private static final AtomicBoolean sIsLoggingEnabled = new AtomicBoolean(false);

    public static void log(final String tag, final String message, Object...params){
        if(isLoggingEnabled() && !TextUtils.isEmpty(tag) && !TextUtils.isEmpty(message)){
            Log.i(tag, String.format(Locale.getDefault(), message, params));
        }
    }

    public static void log(final List<String> tags, final String message, Object...params){
        if(null != tags && !tags.isEmpty()){
            for (String tag : tags) {
                log(tag, message , params);
            }
        }
    }

    private static boolean isLoggingEnabled(){
        return sIsLoggingEnabled.get();
    }

    public void setLoggingEnabled(final boolean isEnabled){
        sIsLoggingEnabled.set(isEnabled);
    }
}
