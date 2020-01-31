package com.example.a201.t2t;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Trigger;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;
import static com.example.a201.t2t.Constants.IS_USER_REGISTERED_KEY;
import static com.example.a201.t2t.Constants.REGISTRATION_TOKEN_KEY;
import static com.example.a201.t2t.Constants.USER_PHONE_KEY;
import static com.example.a201.t2t.RecordsDownloadJobService.EXTRA_RECORD_FROM_NAME;
import static com.example.a201.t2t.RecordsDownloadJobService.EXTRA_RECORD_FROM_PHONE;
import static com.example.a201.t2t.RecordsDownloadJobService.EXTRA_RECORD_URL;

/**
 * A class representing T2T messaging service
 */
public class T2tMessagingService extends FirebaseMessagingService {

    /**
     * A callback method called when got new registration token
     * @param token - registration token
     */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("Refreshed token: ", token);
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString(REGISTRATION_TOKEN_KEY, token).apply();
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(IS_USER_REGISTERED_KEY, false))
        {
            sendRegistrationToServer(token);
        }
    }

    /**
     * A callback method, called when receive new message
     * @param remoteMessage - the received message
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();

        if(data.size() > 0)
        {
            FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
            Bundle extras = new Bundle();
            extras.putString(EXTRA_RECORD_URL, data.get("recordUrl"));
            extras.putString(EXTRA_RECORD_FROM_NAME, data.get("fromName"));
            extras.putString(EXTRA_RECORD_FROM_PHONE, data.get("fromPhone"));

            Job myJob = dispatcher.newJobBuilder()
                    .setService(RecordsDownloadJobService.class)
                    .setTag("T2T-Download-Record-Job")
                    .setConstraints(Constraint.ON_ANY_NETWORK)
                    .setTrigger(Trigger.executionWindow(0,0))
                    .setExtras(extras)
                    .build();
            dispatcher.mustSchedule(myJob);
        }
    }

    /**
     * A method to retrieve registration token
     * @param context - context asks for registration token
     * @return - registration token
     */
    public static String getToken(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(REGISTRATION_TOKEN_KEY, "");
    }

    /**
     * A method sends registration token to server
     * @param token - registration token
     */
    private void sendRegistrationToServer(String token) {
        String userPhone = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(USER_PHONE_KEY, "");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userPhone);

        userRef
                .update(REGISTRATION_TOKEN_KEY, token)
                .addOnSuccessListener(aVoid -> Log.d("T2T", "DocumentSnapshot successfully updated!"))
                .addOnFailureListener(e -> Log.w("T2T", "Error updating document", e));
    }

    /**
     * A method that sends notification to user on new message
     * @param fromName - sender name
     * @param fromPhone - sender phone
     * @param context - context
     */
    void sendNotification(String fromName, String fromPhone, Context context) {

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        Intent intentConversationsView = new Intent (context, ConversationsActivity.class);
        stackBuilder.addParentStack(ConversationsActivity.class);
        stackBuilder.addNextIntent(intentConversationsView);

        Intent intentRecordView = new Intent(context, ConversationActivity.class);
        intentRecordView.putExtra("name", fromName);
        intentRecordView.putExtra("phone", fromPhone);
        stackBuilder.addNextIntent(intentRecordView);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(555, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,
                 context.getString(R.string.default_notification_channel_id))
                .setSmallIcon(R.drawable.microphone_splash)
                .setContentTitle("Received New Voice Message")
                .setContentText("New voice message from " + fromName)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(555 /* ID of notification */, notificationBuilder.build());
    }
}
