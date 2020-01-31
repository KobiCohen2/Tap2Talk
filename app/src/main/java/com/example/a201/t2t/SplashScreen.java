package com.example.a201.t2t;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import java.util.Timer;
import java.util.TimerTask;
import static com.example.a201.t2t.Constants.IS_USER_REGISTERED_KEY;

/**
 * A class represent activity that shows splash screen
 */
public class SplashScreen extends Activity {

    private static final int SPLASH_DISPLAY_LENGTH = 2000;
    private boolean isRegistered = false;

    /**
     * A callback method called when that activity starts to create
     * @param savedInstanceState - a bundle that holds save state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        createNotificationChannel();
        isRegistered = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(IS_USER_REGISTERED_KEY, false);

        Timer runSplash = new Timer();

        TimerTask showSplash = new TimerTask() {
            @Override
            public void run() {
                if (isRegistered) {
                    Intent mainIntent = new Intent(SplashScreen.this, ConversationsActivity.class);
                    startActivity(mainIntent);
                    finish();
                } else {
                    Intent mainIntent = new Intent(SplashScreen.this, RegistrationActivity.class);
                    startActivity(mainIntent);
                    finish();
                }
            }
        };
        runSplash.schedule(showSplash, SPLASH_DISPLAY_LENGTH);
    }

    /**
     * A method that creates notification channel
     */
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "T2t_Channel";
            String description = "T2T_Messaging";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(getString(R.string.default_notification_channel_id), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}