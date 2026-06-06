package com.robinzon.medicationwizard.ads.admob;

import android.app.Activity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.AdAction;
import com.robinzon.medicationwizard.ads.AdPlacement;
import com.robinzon.medicationwizard.ads.AdType;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;
import com.robinzon.medicationwizard.utils.NetworkUtils;
import com.robinzon.medicationwizard.utils.Screen;

public class AdMobBanner extends AdMobAd {

    private final AdView mAdView;
    private AdListener mAdListener;
    private FrameLayout mAdContainerView;
    private boolean mIsViewAdded = false;

    public AdMobBanner(final @NonNull String adUnitId,
                       final @NonNull AdsManager adsManager,
                       final @NonNull AdPlacement placement) {
        super(adUnitId, adsManager, placement);
        log("%s Creating object.\n%s", getLogTag(), thisToString());
        this.mAdView = new AdView(getActivity());
        mAdView.setId(R.id.adView);
        mAdView.setAdUnitId(adUnitId);
        createAdListener();
        mAdView.setAdListener(getAdListener());
        addBannerHeightListener();
        mAdView.setAdSize(getAdSize(getActivity()));
        
        getAdsManager().onAdAction(this, AdAction.Created);
    }

    private void attachToContainer() {
        final FrameLayout adContainerView = getAdContainerView();
        if (null != adContainerView && !mIsViewAdded) {
            if (mAdView.getParent() != null) {
                ((ViewGroup) mAdView.getParent()).removeView(mAdView);
            }
            adContainerView.addView(mAdView);
            mIsViewAdded = true;
        }
    }

    private void addBannerHeightListener() {
        getAdView().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                    }
                });
    }

    @NonNull
    private String thisToString() {
        return AdMobBanner.this.toString();
    }

    @Override
    public boolean shouldShow() {
        return true;
    }

    private void createAdListener() {
        mAdListener = new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
                getAdsManager().onAdAction(AdMobBanner.this, AdAction.Clicked);
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                getAdsManager().onAdAction(AdMobBanner.this, AdAction.Dismissed);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                setIsLoaded(false);
                setIsLoading(false);
                failedToLoad(loadAdError);
                log("%s Failed to load. Reason is %s.\n%s", getLogTag(), loadAdError.getMessage(), thisToString());
                getAdsManager().onAdAction(AdMobBanner.this, AdAction.FailedToLoad);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                getAdsManager().onAdAction(AdMobBanner.this, AdAction.Impression);
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                setIsLoaded(true);
                setIsLoading(false);
                log("%s Loaded.\n%s", getLogTag(), thisToString());
                getAdsManager().onAdAction(AdMobBanner.this, AdAction.LoadedSuccessfully);
                loaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                log("%s Opened.\n%s", getLogTag(), thisToString());
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
            }
        };
    }

    private @NonNull AdListener getAdListener() {
        return mAdListener;
    }

    public @NonNull AdView getAdView() {
        return mAdView;
    }

    @Override
    public AdType getAdType() {
        return AdType.AdaptiveBanner;
    }

    public @Nullable FrameLayout getAdContainerView() {
        if(null == mAdContainerView){
            mAdContainerView = getActivity().findViewById(R.id.ad_container);
        }
        return mAdContainerView;
    }

    @Override
    public void load() {
        log("%s Requesting load.\n%s", getLogTag(), thisToString());
        if (Boolean.TRUE.equals(shouldBeLoaded())) {
            getAdsManager().onAdAction(AdMobBanner.this, AdAction.StartingToLoad);
            log("%s Preparing for loading.\n%s", getLogTag(), thisToString());

            getActivity().runOnUiThread(() -> {
                attachToContainer();
                mAdView.loadAd(getAdRequest());
                setIsLoading(true);
            });
        } else {
            log("%s Refusing load. Has network %b. \n%s",
                    getLogTag(),
                    NetworkUtils.isNetworkAvailable(getContext().getApplicationContext()),
                    thisToString());
        }
    }

    public static int getBannerHeightDP(final Activity activity) {
        return getAdSize(activity).getHeight();
    }

    public static int getBannerWidthDP(final Activity activity) {
        return getAdSize(activity).getWidth();
    }


    private static AdSize getAdSize(final Activity activity) {

        int adWidthPixels = Screen.getUsableScreenWidthPX(activity);
        final int adWidthDp = (int) (adWidthPixels / Screen.getDensity(activity.getResources()));

        // Use standard Anchored Adaptive (not the 'Large' version). 
        // This provides the best balance of aesthetics and filling the width.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp);
    }

    @Override
    public void show() {

    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public void hide() {

    }

    @Override
    public void onPause() {
        getAdView().pause();
    }

    @Override
    public void onResume() {
        getAdView().resume();
    }

    @Override
    public @NonNull AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    @Override
    public Object getCoreAdObject() {
        return getAdView();
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            if (mAdView.getParent() != null) {
                ((ViewGroup) mAdView.getParent()).removeView(mAdView);
            }
            mAdView.destroy();
        }
    }
}
