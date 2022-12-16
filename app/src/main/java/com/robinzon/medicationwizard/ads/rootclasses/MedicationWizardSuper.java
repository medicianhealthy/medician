package com.robinzon.medicationwizard.ads.rootclasses;

import com.robinzon.medicationwizard.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public abstract class MedicationWizardSuper {

    public String getClassName(){
        return getClass().getSimpleName();
    }

    protected List<String> getLogTags(){
        if (Logger.isLoggingEnabled()) {
            return new ArrayList<String>() {{
                add(getClass().getSimpleName());
            }};
        }
        return null;
    }

    protected void logMessage(final String message, final Object... params){
        if (Logger.isLoggingEnabled()){
            Logger.getInstance().log(getClassName() , getLogTags() , message, params);
        }
    }


}
