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

/**
 * Encapsulates the implementation logic for loading and displaying an adaptive AdMob Banner.
 * Automatically handles screen width calculations to request the correct adaptive AdSize.
 */
public class AdMobBanner extends AdMobAd {

    private final AdView mAdView;
    private AdListener mAdListener;
    private FrameLayout mAdContainerView;

    /**
     * Constructs a new Banner ad wrapper, configuring the underlying AdView instance.
     *
     * @param adUnitId The unique identifier for this banner unit. Must not be null.
     * @param adsManager The central manager coordinating this ad's lifecycle. Must not be null.
     * @param placement The UI context where this ad is intended to appear. Must not be null.
     */
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

        // IMPORTANT: AdSize is NOT set here anymore. 
        // It must be set exactly once in attachToContainer or load.

        mAdView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        getAdsManager().onAdAction(this, AdAction.Created);
    }

    /**
     * Calculates the height in density-independent pixels of the optimal adaptive banner for the current screen.
     *
     * @param activity The context used for screen measurements.
     * @return The banner height in DP.
     */
    public static int getBannerHeightDP(final Activity activity) {
        return getAdSize(activity).getHeight();
    }

    /**
     * Calculates the width in density-independent pixels of the optimal adaptive banner for the current screen.
     *
     * @param activity The context used for screen measurements.
     * @return The banner width in DP.
     */
    public static int getBannerWidthDP(final Activity activity) {
        return getAdSize(activity).getWidth();
    }

    /**
     * Generates the AdSize required to load an Anchored Adaptive Banner based on available screen space.
     * Caps the width on very wide tablet devices to avoid excessive letterboxing by AdMob.
     *
     * @param activity The context used for screen measurements.
     * @return The calculated AdSize payload.
     */
    private static AdSize getAdSize(final Activity activity) {

        int adWidthPixels = Screen.getUsableScreenWidthPX(activity);
        int adWidthDp = (int) (adWidthPixels / Screen.getDensity(activity.getResources()));

        if (adWidthDp > 760) {
            adWidthDp = 728;
        }

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp);
    }

    /**
     * Attaches the banner ad to its default target container if available.
     */
    public void attachToContainer() {
        if (mAdContainerView == null) {
            mAdContainerView = getActivity().findViewById(R.id.ad_container);
        }
        attachToContainer(mAdContainerView);
    }

    /**
     * Injects the banner AdView into the specified container, adjusting background colors to mask letterboxing artifacts.
     *
     * @param container The FrameLayout where the banner should reside. May be null.
     */
    public void attachToContainer(@Nullable FrameLayout container) {
        if (null != container) {
            if (mAdView.getParent() == container) return;

            if (mAdView.getParent() != null) {
                ((ViewGroup) mAdView.getParent()).removeView(mAdView);
            }

            // On wide tablet landscape, AdMob pads with black bars internally.
            // We use the app's background color for the container to match the app theme.
            int bgColor = com.google.android.material.color.MaterialColors.getColor(container, android.R.attr.colorBackground);
            container.setBackgroundColor(bgColor);

            // Synchronous size check/set to avoid "Ad size can only be set once" crash.
            ensureAdSizeSet(container);

            AdSize adSize = mAdView.getAdSize();
            if (adSize == null) return;

            float density = Screen.getDensity(getActivity().getResources());
            int widthPx = (int) (adSize.getWidth() * density);
            int heightPx = (int) (adSize.getHeight() * density);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    widthPx > 0 ? widthPx : ViewGroup.LayoutParams.WRAP_CONTENT,
                    heightPx > 0 ? heightPx : ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.Gravity.CENTER);

            container.addView(mAdView, lp);
            mAdContainerView = container;

            // Aggressive transparency check
            mAdView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
    }

    /**
     * Ensures the AdSize is configured exactly once for this AdView instance to prevent crashes.
     * Utilizes the container's width if passed, otherwise falls back to the full usable screen width.
     *
     * @param container The target container layout. May be null.
     */
    private void ensureAdSizeSet(@Nullable FrameLayout container) {
        if (mAdView.getAdSize() != null) return;

        int containerWidthPx = (container != null) ? container.getWidth() : 0;
        if (containerWidthPx <= 0) {
            containerWidthPx = Screen.getUsableScreenWidthPX(getActivity());
        }

        float density = Screen.getDensity(getActivity().getResources());
        int containerWidthDp = (int) (containerWidthPx / density);

        // Creative fix: On wide tablet landscape screens, AdMob often pads the ad with black bars
        // if we request the full width. We cap the requested width to a standard tablet
        // leaderboard (728dp) to prevent this and let our background show.
        if (containerWidthDp > 760) {
            containerWidthDp = 728;
        }

        AdSize targetSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(getActivity(), containerWidthDp);
        mAdView.setAdSize(targetSize);
        log("%s AdSize set to %s", getLogTag(), targetSize.toString());
    }

    /**
     * Re-attaches the banner to its default UI position.
     */
    public void resetContainer() {
        mAdContainerView = null;
        attachToContainer();
    }

    /**
     * Registers a listener to observe dynamic layout changes, ensuring the layout scales accurately.
     */
    private void addBannerHeightListener() {
        getAdView().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                    }
                });
    }

    /**
     * Returns a string representation of the banner ad's state for debugging purposes.
     *
     * @return Formatted debugging details.
     */
    @NonNull
    private String thisToString() {
        return AdMobBanner.this.toString();
    }

    /**
     * Checks if business logic allows this banner ad to be displayed.
     *
     * @return True if the user is not premium or if forced ads are enabled.
     */
    @Override
    public boolean shouldShow() {
        return !com.robinzon.medicationwizard.AppConfig.isPremium(getContext()) || com.robinzon.medicationwizard.AppConfig.FORCED_ADS_VISIBLE;
    }

    /**
     * Instantiates the AdListener that propagates ad loading and interactive states up to the AdsManager.
     */
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

    /**
     * Retrieves the instantiated ad event listener for this banner.
     *
     * @return The constructed AdListener object.
     */
    private @NonNull AdListener getAdListener() {
        return mAdListener;
    }

    /**
     * Exposes the underlying AdView managed by this class.
     *
     * @return The active AdView instance.
     */
    public @NonNull AdView getAdView() {
        return mAdView;
    }

    /**
     * Indicates the type format this class manages.
     *
     * @return Always AdType.AdaptiveBanner.
     */
    @Override
    public AdType getAdType() {
        return AdType.AdaptiveBanner;
    }

    /**
     * Safely initiates an ad payload request on the UI thread, bypassing if conditions are unmet.
     */
    @Override
    public void load() {
        log("%s Requesting load.\n%s", getLogTag(), thisToString());
        if (Boolean.TRUE.equals(shouldBeLoaded()) && shouldShow()) {
            getAdsManager().onAdAction(AdMobBanner.this, AdAction.StartingToLoad);
            log("%s Preparing for loading.\n%s", getLogTag(), thisToString());

            getActivity().runOnUiThread(() -> {
                attachToContainer();
                // Ensure size is set as fallback if container attach didn't do it
                ensureAdSizeSet(null);
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

    /**
     * Required implementation. Not used explicitly for banners as they are auto-rendered when attached.
     */
    @Override
    public void show() {

    }

    /**
     * Determines if the loaded banner content is past its useful life.
     *
     * @return Always false for Banners, as AdMob internal mechanisms handle their refresh.
     */
    @Override
    public boolean isExpired() {
        return false;
    }

    /**
     * Placeholder method to hide the ad view.
     */
    @Override
    public void hide() {

    }

    /**
     * Halts internal banner activities when the host UI moves to the background.
     */
    @Override
    public void onPause() {
        getAdView().pause();
    }

    /**
     * Resumes internal banner activities when the host UI returns to the foreground.
     */
    @Override
    public void onResume() {
        getAdView().resume();
    }

    /**
     * Assembles a new generic AdRequest object for network calls.
     *
     * @return A constructed AdRequest.
     */
    @Override
    public @NonNull AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    /**
     * Grants access to the raw View object displaying the ad.
     *
     * @return The underlying AdView.
     */
    @Override
    public Object getCoreAdObject() {
        return getAdView();
    }

    /**
     * Erases the ad from its layout parent and reclaims resources when the ad is no longer needed.
     */
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