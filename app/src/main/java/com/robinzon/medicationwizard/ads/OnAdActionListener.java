package com.robinzon.medicationwizard.ads;

import androidx.annotation.NonNull;

import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;

public interface OnAdActionListener {
    void onAdAction(@NonNull final AdMobAd adMobAd, final AdAction adAction);
}
