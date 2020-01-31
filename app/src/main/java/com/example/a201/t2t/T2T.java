package com.example.a201.t2t;

import android.app.Application;
import android.content.IntentFilter;

/**
 * A class represent the whole application
 */
public class T2T extends Application {

    private static T2T mInstance;
    private ConnectivityReceiver connectivityReceiver;
    final static String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

    /**
     * A callback method called when that activity starts to create
     */
    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        IntentFilter filter = new IntentFilter();
        filter.addAction(CONNECTIVITY_ACTION);
        connectivityReceiver = new ConnectivityReceiver();
        registerReceiver(connectivityReceiver, filter);
    }

    /**
     * A method retrieves T2T instance
     * @return - T2T instance
     */
    public static synchronized T2T getInstance() {
        return mInstance;
    }

    /**
     * A method that sets connectivity listener
     * @param listener - connectivity receiver listener
     */
    public void setConnectivityListener(ConnectivityReceiverListener listener) {
        ConnectivityReceiver.connectivityReceiverListener = listener;
    }

}
