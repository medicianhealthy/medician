package com.robinzon.medicationwizard;

import android.app.Application;

import com.robinzon.medicationwizard.remoteconfig.RemoteConfigManager;

public class MedicationWizardApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RemoteConfigManager.getInstance().fetchConfiguration(null);
    }
}
