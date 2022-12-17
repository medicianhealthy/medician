package com.robinzon.medicationwizard.ads.rootclasses;

import com.robinzon.medicationwizard.utils.Logger;

import java.util.List;

public abstract class MedicationWizardSuper {

    private List<String> mLogTags;

    protected List<String> getLogTags() {
        if (Logger.isLoggingEnabled()) {
            return mLogTags;
        }
        return null;
    }

    protected void  setLogTags(final List<String> tags) {
        if (Logger.isLoggingEnabled()) {
            mLogTags = tags;
        }
    }

    protected String getClassName() {
        return getClass().getSimpleName();
    }

    protected void logMessage(final String message, final Object... params) {
        if (Logger.isLoggingEnabled()) {
            Logger.getInstance().log(getClassName(), getLogTags(), message, params);
        }
    }
}
