package com.robinzon.medicationwizard.ads.adsproviders.admob;

import android.app.Activity;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.EAdCallBacks;
import com.robinzon.medicationwizard.ads.interfaces.IAdsLifeCycleCallBack;
import com.robinzon.medicationwizard.ads.rootclasses.Banner;
import com.robinzon.medicationwizard.ads.rootclasses.EAdPlacement;

public final class AdMobBanner extends Banner {
    private final AdView mBanner;

    public AdMobBanner(Activity activity, EAdPlacement placement) {
        super(activity, placement);
        mBanner = getActivity().findViewById(R.id.adView);
    }

    @Override
    public boolean shouldLoad() {
        return null != mBanner && super.shouldLoad();
    }

    @Override
    public void load(final IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        if (shouldLoad()){
            mBanner.setAdListener(getAdListener(adsLifeCycleCallBack));
            mBanner.loadAd(getAdRequest());
        }
    }

    @Override
    public void load() {
        load(null);
    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    private AdListener getAdListener(final IAdsLifeCycleCallBack adsLifeCycleCallBack) {
        return new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                handleAdCallBacks(EAdCallBacks.CLICKED, adsLifeCycleCallBack);
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                handleAdCallBacks(EAdCallBacks.DISMISSED, adsLifeCycleCallBack);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                handleAdCallBacks(EAdCallBacks.FAILED_TO_LOAD, adsLifeCycleCallBack);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                handleAdCallBacks(EAdCallBacks.LOADED, adsLifeCycleCallBack);
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                handleAdCallBacks(EAdCallBacks.SHOWN, adsLifeCycleCallBack);
            }
        };
    }

    @Override
    public boolean canShow() {
        return null != mBanner && super.canShow();
    }

    @Override
    public void show() {

    }

    @Override
    public void show(IAdsLifeCycleCallBack adsLifeCycleCallBack) {

    }

    @Override
    public void onResume() {
        if (null != mBanner){
            mBanner.resume();
        }
    }

    @Override
    public void onPause() {
        if (null != mBanner){
            mBanner.pause();
        }
    }

    @Override
    public void onDestroy() {
        if (null != mBanner){
            mBanner.destroy();
        }
    }

    @Override
    public void onCreate() {

    }


    @Override
    public int getBannerHeightInPixels() {
        return getAdSize().getHeightInPixels(getActivity());
    }

    private AdSize getAdSize() {
        final float density = getActivity().getResources().getDisplayMetrics().density;

        final float screenWidth;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            final WindowMetrics windowMetrics = getActivity().getWindowManager().getCurrentWindowMetrics();
            final Rect bounds = windowMetrics.getBounds();
            screenWidth = bounds.width();
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            screenWidth = displayMetrics.widthPixels;
        }
        final int adWidth = (int) (screenWidth / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(getActivity(), adWidth);
    }

    @Override
    public Object getAdCoreObject() {
        return mBanner;
    }
}
