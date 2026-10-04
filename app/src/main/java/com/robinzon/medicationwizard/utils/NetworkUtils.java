package com.robinzon.medicationwizard.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

/**
 * Utility class for checking device network connectivity states.
 * <p>
 * Provides static helper methods to determine if the device has an active internet connection
 * or is connected to a specific network type, such as Wi-Fi.
 * </p>
 */
public class NetworkUtils {

    /**
     * Checks if there is an active network connection with internet capability.
     *
     * @param context The application or activity context. Must not be null.
     * @return True if a network with internet capability is active, false otherwise.
     */
    public static boolean isNetworkAvailable(Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        final NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

        if (networkCapabilities != null) {
            return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }

    /**
     * Checks if the device is currently connected to a Wi-Fi network.
     *
     * @param context The application or activity context. Must not be null.
     * @return True if the active network is a Wi-Fi transport, false otherwise.
     */
    public static boolean isWifiConnected(Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        final NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

        if (networkCapabilities != null) {
            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        return false;
    }
}
