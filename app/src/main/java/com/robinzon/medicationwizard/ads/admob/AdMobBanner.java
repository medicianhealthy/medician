package com.robinzon.medicationwizard.ads.admob;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

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

public class AdMobBanner extends AdMobAd {

    private final AdView mAdView;
    private AdListener mAdListener;
    private boolean mInitialLayoutComplete;
    private ConstraintLayout mAdContainerView;
    private int mBannerHeight;
    private AdSize mAdSize;

    public AdMobBanner(final @NonNull String adUnitId,
                       final @NonNull AdsManager adsManager,
                       final @NonNull AdPlacement placement) {
        super(adUnitId, adsManager, placement);
        log("%s Creating object.\n%s", getLogTag(), thisToString());
        this.mAdView = new AdView(getActivity());
        getAdView().setId(R.id.adView);
        getAdView().setAdUnitId(adUnitId);
        createAdListener();
        getAdView().setAdListener(getAdListener());
        addBannerHeightListener();
    }

    private void addBannerHeightListener() {
        getAdView().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Get the current height of the AdView
                        int newBannerHeight = mAdView.getHeight();
                        // Check if the banner height has changed and is not zero
                        if (newBannerHeight != mBannerHeight && newBannerHeight != 0) {
                            // Update mBannerHeight with the new value
                            mBannerHeight = newBannerHeight;
                            // Find the RecyclerView in the current activity
                            RecyclerView recyclerView = getActivity().findViewById(R.id.recyclerView);
                            // Get the current layout parameters of the RecyclerView
                            ViewGroup.MarginLayoutParams layoutParams =
                                    (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
                            // Retrieve the top margin dimension from resources
                            int marginInPixels = getActivity().getResources().getDimensionPixelSize(R.dimen.bannerTopMargin);
                            layoutParams.bottomMargin = mBannerHeight + marginInPixels;
                            // Apply the new layout parameters to the RecyclerView
                            recyclerView.setLayoutParams(layoutParams);
                        }
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

    @Override
    public void load() {
        log("%s Requesting load.\n%s", getLogTag(), thisToString());
        if (Boolean.TRUE.equals(shouldBeLoaded())) {
            getAdsManager().onAdAction(AdMobBanner.this, AdAction.StartingToLoad);
            log("%s Preparing for loading.\n%s", getLogTag(), thisToString());
            try {
                mAdContainerView = getActivity().findViewById(R.id.content_main);
                // Locked in as final
                final View adView = getAdView();
                final int adViewId = adView.getId();

                mAdContainerView.addView(adView);

                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(mAdContainerView);

                constraintSet.connect(adViewId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                constraintSet.connect(adViewId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                constraintSet.connect(adViewId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

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
                    getLogTag(),
                    NetworkUtils.isNetworkAvailable(getContext().getApplicationContext()),
                    thisToString());
        }
    }

    private AdSize getAdSize() {
        if(null != mAdSize){
            return mAdSize;
        }
        // 1. Secure a single, non-null Activity reference
        final Activity activity = getActivity();

        int adWidthPixels = mAdContainerView.getWidth();

        // 2. If the view hasn't been laid out yet, calculate the screen width
        if (adWidthPixels == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final Rect bounds = activity.getWindowManager().getCurrentWindowMetrics().getBounds();
                adWidthPixels = bounds.width();
            } else {
                final DisplayMetrics displayMetrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                adWidthPixels = displayMetrics.widthPixels;
            }
        }

        // 3. Convert pixels to density-independent pixels (dp)
        final float density = activity.getResources().getDisplayMetrics().density;
        final int adWidthDp = (int) (adWidthPixels / density);

        mAdSize  = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp);
        return mAdSize;
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
