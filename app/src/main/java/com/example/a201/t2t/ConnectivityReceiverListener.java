package com.example.a201.t2t;

/**
 * An interface represent network connectivity receiver listener
 */
public interface ConnectivityReceiverListener {
    void onNetworkConnectionChanged(boolean isConnected);
}
