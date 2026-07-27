package com.robinzon.medicationwizard.billing;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.utils.Logger;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all Google Play Billing interactions for the application.
 * <p>
 * This class handles connecting to the Play Store, querying available products,
 * processing purchases, and verifying existing entitlements.
 * </p>
 */
public class BillingManager implements PurchasesUpdatedListener {

    private static final String PRODUCT_ID_PREMIUM = "premium_upgrade";
    private static final String KEY_CACHED_PREMIUM = "cached_premium_status";
    private static final String KEY_OFFLINE_COUNT = "offline_premium_count";
    private static final int MAX_OFFLINE_SESSIONS = 5;

    private static BillingManager sInstance;

    private final BillingClient mBillingClient;
    private final Context mContext;
    private ProductDetails mPremiumProductDetails;

    private BillingManager(Context context) {
        this.mContext = context.getApplicationContext();

        PendingPurchasesParams pendingPurchasesParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build();

        this.mBillingClient = BillingClient.newBuilder(mContext)
                .setListener(this)
                .enablePendingPurchases(pendingPurchasesParams)
                .build();

        loadPremiumFallback();
        startConnection();
    }

    public static synchronized BillingManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BillingManager(context);
        }
        return sInstance;
    }

    /**
     * Loads the premium status from local storage as a fallback for offline use.
     * <p>
     * Logic: Allows the user to retain premium benefits for a limited number of
     * offline sessions (MAX_OFFLINE_SESSIONS) before requiring a successful
     * Play Store re-verification.
     * </p>
     */
    private void loadPremiumFallback() {
        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(mContext);
        boolean wasPremium = sp.getBoolean(KEY_CACHED_PREMIUM, false);
        int offlineCount = sp.getInt(KEY_OFFLINE_COUNT, 0);

        if (wasPremium) {
            if (offlineCount < MAX_OFFLINE_SESSIONS) {
                AppConfig.IS_PREMIUM = true;
                sp.setInt(KEY_OFFLINE_COUNT, offlineCount + 1);
                Logger.log("Billing", "Offline fallback active. Session " + (offlineCount + 1));
            } else {
                AppConfig.IS_PREMIUM = false;
                Logger.log("Billing", "Offline limit reached. Re-verification required.");
            }
        }
    }

    /**
     * Establishes a connection to the Google Play Store.
     */
    private void startConnection() {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Logger.log("Billing", "Connected to Play Store");
                    refreshPremiumStatusFromPrevPurchases();
                    queryPremiumProductDetails();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Logger.log("Billing", "Disconnected from Play Store. Retrying...");
                // In production, implement an exponential backoff retry here.
            }
        });
    }

    /**
     * Queries Google Play for the details of our premium product (price, etc.).
     */
    private void queryPremiumProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PREMIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        mBillingClient.queryProductDetailsAsync(params, (billingResult, productDetailsResult) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsResult.getProductDetailsList() != null) {
                for (ProductDetails details : productDetailsResult.getProductDetailsList()) {
                    if (PRODUCT_ID_PREMIUM.equals(details.getProductId())) {
                        mPremiumProductDetails = details;
                    }
                }
            }
        });
    }

    /**
     * Checks Google Play for active purchases and updates the user's premium state.
     * <p>
     * Performance: Iterates through current purchases and specifically verifies
     * the product ID to ensure robust entitlement management.
     * </p>
     */
    public void refreshPremiumStatusFromPrevPurchases() {
        mBillingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        boolean owned = false;
                        for (Purchase purchase : purchases) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                                    && purchase.getProducts().contains(PRODUCT_ID_PREMIUM)) {
                                owned = true;
                                break;
                            }
                        }

                        // Only update global flag if a developer cheat is not active
                        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(mContext);
                        if (!sp.getBoolean(AppConfig.KEY_CHEAT_PREMIUM, false)) {
                            sp.setBoolean(KEY_CACHED_PREMIUM, owned);
                            sp.setInt(KEY_OFFLINE_COUNT, 0);
                            AppConfig.IS_PREMIUM = owned;
                        }

                        Logger.log("Billing", "Premium status refreshed: " + owned + (sp.getBoolean(AppConfig.KEY_CHEAT_PREMIUM, false) ? " (Ignored due to cheat)" : ""));
                    }
                }
        );
    }

    /**
     * Launches the official Google Play purchase flow.
     *
     * @param activity The activity context.
     */
    public void launchPurchaseFlow(Activity activity) {
        if (mPremiumProductDetails == null) {
            Logger.log("Billing", "Product details not loaded yet.");
            return;
        }

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(mPremiumProductDetails)
                .build());

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        mBillingClient.launchBillingFlow(activity, billingFlowParams);
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        }
    }

    /**
     * Processes a single purchase object.
     * <p>
     * Performance: Checks for the specific premium ID and updates the global
     * app configuration immediately.
     * </p>
     *
     * @param purchase The purchase object to verify.
     */
    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                && purchase.getProducts().contains(PRODUCT_ID_PREMIUM)) {
            // In a real app, you must acknowledge the purchase here.
            AppConfig.IS_PREMIUM = true;

            // Immediately cache the success
            SharedPreferencesManager sp = SharedPreferencesManager.getInstance(mContext);
            sp.setBoolean(KEY_CACHED_PREMIUM, true);
            sp.setInt(KEY_OFFLINE_COUNT, 0);

            Logger.log("Billing", "Purchase successful!");
        }
    }
}
