package com.robinzon.medicationwizard.ads.rootclasses;

import java.lang.ref.WeakReference;
import java.util.List;

public abstract class MedicationWizardSuper {
    protected WeakReference<List<String>> mLogTags;

    public String getClassName(){
        return getClass().getSimpleName();
    }

    protected List<String> getLogTags(){
        return mLogTags.get();
    }

    protected void setLogTags(final List<String> logTags){
        mLogTags = new WeakReference<>(logTags);
    }
}
