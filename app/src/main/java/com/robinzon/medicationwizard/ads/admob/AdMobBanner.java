package com.robinzon.medicationwizard.ads.admob;

import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.AdPlacement;
import com.robinzon.medicationwizard.ads.AdType;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.ads.rootclasses.AdMobAd;
import com.robinzon.medicationwizard.utils.NetworkUtils;

public class AdMobBanner extends AdMobAd {

    private final AdView mAdView;
    private AdListener mAdListener;
    private boolean mInitialLayoutComplete;
    private ConstraintLayout mAdContainerView;

    public AdMobBanner(final @NonNull String adUnitId,
                       final @NonNull AdsManager adsManager,
                       final @NonNull AdPlacement placement) {
        super(adUnitId, adsManager, placement);
        log("%s Creating object.\n%s",getLogTag() , thisToString());
        this.mAdView = new AdView(getActivity());
        getAdView().setId(R.id.adView);
        getAdView().setAdUnitId(adUnitId);
        createAdListener();
        getAdView().setAdListener(getAdListener());
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
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                setIsLoaded(false);
                setIsLoading(false);
                failedToLoad(loadAdError);
                log("%s Failed to load. Reason is %s.\n%s",getLogTag() ,loadAdError.getMessage(), thisToString());
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                setIsLoaded(true);
                setIsLoading(false);
                log("%s Loaded.\n%s",getLogTag(), thisToString());
                loaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                log("%s Opened.\n%s",getLogTag(), thisToString());
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

    @Override
    public void load() {
        log("%s Requesting load.\n%s",getLogTag() , thisToString());
        if (shouldBeLoaded()) {
            log("%s Preparing for loading.\n%s",getLogTag(), thisToString());
            try {
                mAdContainerView = getActivity().findViewById(R.id.adViewParent);
                mAdContainerView.addView(getAdView());

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(mAdContainerView);
                constraintSet.connect(getAdView().getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                constraintSet.connect(getAdView().getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                constraintSet.connect(getAdView().getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                constraintSet.applyTo(mAdContainerView);
                // Since we're loading the banner based on the adContainerView size, we need
                // to wait until this view is laid out before we can get the width.
                mAdContainerView.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                if (!mInitialLayoutComplete) {
                                    mInitialLayoutComplete = true;
                                    mAdView.setAdSize(getAdSize());
                                    mAdView.loadAd(getAdRequest());
                                }
                            }
                        });
                setIsLoading(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log("%s Refusing load. Has network %b. \n%s",
                    getLogTag() ,
                    NetworkUtils.isNetworkAvailable(getContext().getApplicationContext()),
                    thisToString());
        }
    }

    private AdSize getAdSize() {
        WindowMetrics windowMetrics;
        Rect bounds;
        float availableWidth = 0F;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            windowMetrics = getActivity().getWindowManager().getCurrentWindowMetrics();
            bounds = windowMetrics.getBounds();
            availableWidth = bounds.width();
        } else {
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            final WindowManager windowManager = (WindowManager) getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            if (null != windowManager) {
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                availableWidth = displayMetrics.widthPixels;
            }
        }
        float adWidthPixels = mAdContainerView.getWidth();
        // If the ad hasn't been laid out, default to the full screen width.
        if (0f == adWidthPixels) {
            adWidthPixels = availableWidth;
        }
        final float density = getActivity().getResources().getDisplayMetrics().density;
        final int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(getActivity(), adWidth);
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

    }
}
