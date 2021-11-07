package com.robinzon.madician.ads.interfaces;

import android.app.Activity;

public interface AdsProviderActions {
    void onResume(final Activity activity);
    void onPause(final Activity activity);
    void onDestroy(final Activity activity);
    void onCreate(final Activity activity);



}
