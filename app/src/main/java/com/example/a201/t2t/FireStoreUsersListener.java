package com.example.a201.t2t;

import android.util.Log;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static com.example.a201.t2t.ConversationsActivity.checkNullOrEmpty;

/**
 * A class represents a thread that listen for realtime changes in firestore db
 */
public class FireStoreUsersListener extends Thread implements Serializable {

    private static FireStoreUsersListener instance;
    static Map<String, User> users = Collections.synchronizedMap(new HashMap<>());
    private static ServiceCallbacks serviceCallbacks;
    private static boolean isRunning = false;
    private static String TAG = "T2T-FireStore";

    /**
     * A private constructor due to singleton design pattern
     */
    private FireStoreUsersListener() { isRunning = true; }

    /**
     * A method the thread will run at start,
     * listen for realtime changes in cloud db
     */
    @Override
    public void run() {
        isRunning = true;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "listen:error", e);
                return;
            }
            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    String name = dc.getDocument().getString("name");
                    String phone = dc.getDocument().getString("phone");
                    Boolean isGroup = dc.getDocument().getBoolean("isGroup");
                    Boolean isOnline = dc.getDocument().getBoolean("isOnline");
                    String imageUrl = dc.getDocument().getString("imageUrl");
                    String thumbUrl = dc.getDocument().getString("thumbUrl");
                    switch (dc.getType()) {
                        case ADDED:
                            if(checkNullOrEmpty(phone))
                            {
                                User user = new User(name, phone, thumbUrl);
                                if(!isGroup)
                                {
                                    user.setOnline(isOnline);
                                }
                                user.setGroup(isGroup);
                                if(thumbUrl != null)
                                {
                                    user.setOldThumbUrl(thumbUrl);
                                }
                                if (imageUrl != null)
                                {
                                    user.setImageUrl(imageUrl);
                                }
                                users.put(phone, user);
                            }
                            Log.d(TAG, "New user: " + dc.getDocument().getData());
                            break;
                        case MODIFIED:
                            if(users.containsKey(phone))
                            {
                                User user = users.get(phone);
                                user.setName(name);
                                if(!isGroup)
                                {
                                    user.setOnline(isOnline);
                                }
                                user.setGroup(isGroup);
                                if(thumbUrl != null)
                                {
                                    user.setThumbUrl(thumbUrl);
                                }
                                if (imageUrl != null)
                                {
                                    user.setImageUrl(imageUrl);
                                }
                            }
                            Log.d(TAG, "Modified user: " + dc.getDocument().getData());
                            break;
                        case REMOVED:
                            if(users.containsKey(phone))
                            {
                                users.remove(phone);
                            }
                            Log.d(TAG, "Removed user: " + dc.getDocument().getData());
                            break;
                    }
                }
                if (serviceCallbacks != null && ConversationsActivity.isIsContactsPermissionGranted()) {
                    serviceCallbacks.updateUi();
                }
            }
        });
    }

    /**
     * A method returns the single instance of this thread
     * @return - the single instance of this class
     */
    static FireStoreUsersListener getListenerInstance()
    {
        if(instance == null)
        {
            instance = new FireStoreUsersListener();
            return instance;
        }

        return instance;
    }

    /**
     * A method that checked if the thread was running
     * @return - true if the thread was running, false otherwise
     */
    static boolean isRunning()
    {
        return isRunning;
    }

    /**
     * A method for set callback of ui update
     * @param callbacks - the callback function
     */
    void setCallbacks(ServiceCallbacks callbacks)
    {
        serviceCallbacks = callbacks;
    }
}

