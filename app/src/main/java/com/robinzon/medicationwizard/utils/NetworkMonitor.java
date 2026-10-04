package com.robinzon.medicationwizard.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.robinzon.medicationwizard.BuildConfig;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Global monitor for internet connectivity changes.
 * <p>
 * Uses reactive callbacks to notify the app when internet is lost or restored.
 * It leverages the ConnectivityManager to listen for network state changes and
 * broadcasts updates to all registered listeners.
 * </p>
 */
public class NetworkMonitor {

    private static NetworkMonitor sInstance;
    private final Context mContext;
    private final ConnectivityManager mConnectivityManager;
    private final Set<NetworkStatusListener> mListeners = new CopyOnWriteArraySet<>();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private boolean mIsConnected;
    private final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            updateStatus(true);
        }

        @Override
        public void onLost(@NonNull Network network) {
            // Check if any other network is available before declaring "lost"
            boolean stillConnected = NetworkUtils.isNetworkAvailable(mContext);
            updateStatus(stillConnected);
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            updateStatus(hasInternet);
        }
    };

    /**
     * Constructs the NetworkMonitor.
     *
     * @param context The application context used to get system services.
     */
    private NetworkMonitor(Context context) {
        mContext = context.getApplicationContext();
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mIsConnected = NetworkUtils.isNetworkAvailable(mContext);
    }

    /**
     * Retrieves the singleton instance of the NetworkMonitor.
     *
     * @param context The application or activity context.
     * @return The active NetworkMonitor instance.
     */
    public static synchronized NetworkMonitor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new NetworkMonitor(context);
        }
        return sInstance;
    }

    /**
     * Starts listening for network changes.
     * Registers a network callback with the ConnectivityManager.
     */
    public void start() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        mConnectivityManager.registerNetworkCallback(request, mNetworkCallback);
    }

    /**
     * Stops listening for network changes.
     * Unregisters the network callback from the ConnectivityManager.
     */
    public void stop() {
        try {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        } catch (Exception ignored) {
        }
    }

    /**
     * Registers a listener to receive network status updates.
     *
     * @param listener The listener to add.
     */
    public void addListener(NetworkStatusListener listener) {
        mListeners.add(listener);
        // Immediate notification of current state
        listener.onNetworkChanged(mIsConnected);
    }

    /**
     * Unregisters a listener from receiving network status updates.
     *
     * @param listener The listener to remove.
     */
    public void removeListener(NetworkStatusListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Checks the current cached network connection state.
     *
     * @return True if the device is connected to the internet, false otherwise.
     */
    public boolean isConnected() {
        return mIsConnected;
    }

    /**
     * Updates the internal network status and notifies all listeners on the main thread.
     *
     * @param isConnected The new connection status.
     */
    private void updateStatus(boolean isConnected) {
        if (mIsConnected != isConnected) {
            mIsConnected = isConnected;
            mMainHandler.post(() -> {
                if (BuildConfig.DEBUG) {
                    String status = isConnected ? "Internet Connected" : "Internet Lost";
                    Toast.makeText(mContext, status, Toast.LENGTH_SHORT).show();
                }
                for (NetworkStatusListener listener : mListeners) {
                    listener.onNetworkChanged(mIsConnected);
                }
            });
        }
    }

    /**
     * Interface definition for a callback to be invoked when the network status changes.
     */
    public interface NetworkStatusListener {
        /**
         * Called when the network availability changes.
         *
         * @param isAvailable True if internet is available, false otherwise.
         */
        void onNetworkChanged(boolean isAvailable);
    }
}
