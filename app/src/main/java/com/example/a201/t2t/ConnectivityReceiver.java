package com.example.a201.t2t;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

/**
 * A class represent broadcast receiver listen to network connectivity changes
 */
public class ConnectivityReceiver extends BroadcastReceiver {

    public static ConnectivityReceiverListener connectivityReceiverListener;

    public ConnectivityReceiver() {
        super();
    }

    /**
     * A callback method to receive network changes
     * @param context - context the change occurred
     * @param intent - intent contains actions info
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = isNetworkConnected();
        if (connectivityReceiverListener != null) {
            connectivityReceiverListener.onNetworkConnectionChanged(isConnected);
        }
    }

    /**
     * A method that checks network connectivity manually
     * @return true if there is a network connectivity, false otherwise
     */
    public static boolean isNetworkConnected() {
        ConnectivityManager
                manager = (ConnectivityManager) T2T.getInstance().getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * A method that present snackbar with network connectivity info
     * @param activity - the activity to show the snackbar on
     * @param isConnected - the network connectivity status
     */
    public static void showSnack(View activity, boolean isConnected) {
        String message;
        int duration;
        int color;
        if (isConnected) {
            message = "Good! Connected to Internet";
            color = Color.WHITE;
            duration = Snackbar.LENGTH_LONG;
        } else {
            message = "Sorry! Not connected to internet";
            color = Color.RED;
            duration = Snackbar.LENGTH_INDEFINITE;
        }

        Snackbar snackbar = Snackbar
                .make(activity, message, duration);

        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(color);
        snackbar.show();
    }
}



