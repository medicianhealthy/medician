package com.robinzon.medicationwizard.utils;

import android.os.Build;

public class DeviceConfig {
    public static int getApiLevel(){
        return Build.VERSION.SDK_INT;
    }
}
