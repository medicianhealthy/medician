package com.robinzon.medicationwizard.ads;

import android.content.Context;
import android.text.TextUtils;

import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.rootclasses.MedicationWizardSuper;

public class AdsUnitProvider extends MedicationWizardSuper {

    public static String getAdUnit(
            final Context context,
            final EMediator mediator,
            final EAdType adType,
            final String placement) {
        if (null == context || null == mediator || null == adType || TextUtils.isEmpty(placement)) {
            return null;
        }
        if (EMediator.ADMOB == mediator) {
            final int adUnitResourceId;
            switch (adType) {
                case BANNER:
                    adUnitResourceId = BuildConfig.DEBUG ? R.string.admob_banner_id_test : R.string.admob_banner_id_live;
                    break;
                default:
                    adUnitResourceId = 0;
                    break;
            }
        } else {
            return null;
        }
    }
}
