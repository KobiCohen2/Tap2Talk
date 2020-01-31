package com.example.a201.t2t;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import static com.example.a201.t2t.Constants.READ_CONTACTS_PERMISSION;
import static com.example.a201.t2t.Constants.USER_PHONE_KEY;
import static com.example.a201.t2t.Constants.requestPermissionResult;

/**
 * A class represent activity that shows all the conversations
 */
public class ConversationsActivity extends AppCompatActivity implements ServiceCallbacks, ConnectivityReceiverListener {

    private ListView contactsListView;
    private List<User> filteredUsers = Collections.synchronizedList(new ArrayList<>());
    private ProgressBar spinner;
    private static String myPhone;
    private static FireStoreUsersListener fireStoreUsersListener;
    private static boolean isContactsPermissionGranted;

    /**
     * A callback method called when that activity starts to create
     * @param savedInstanceState - a bundle that holds save state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        contactsListView = findViewById(R.id.list);
        spinner = findViewById(R.id.loadContactsProgressBar);
        spinner.setVisibility(View.VISIBLE);

        //create the firestore user listener
        if (fireStoreUsersListener == null && !FireStoreUsersListener.isRunning()) {
            fireStoreUsersListener = FireStoreUsersListener.getListenerInstance();
            fireStoreUsersListener.start();
            //register fot callback
            registerForCallback();
        }

        //restart firestore user listener if needed
        if (fireStoreUsersListener != null && !FireStoreUsersListener.isRunning()) {
            fireStoreUsersListener.start();
        }

        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.conversations_actionbar);
        View view = actionBar.getCustomView();
        ImageButton imageButton = view.findViewById(R.id.settingsIcon);
        imageButton.setOnClickListener(this::OpenSettings);

        contactsListView.setOnItemClickListener((parent, view1, position, id) -> {
            Intent intent = new Intent(getApplicationContext(), ConversationActivity.class);
            intent.putExtra("name", filteredUsers.get(position).getName());
            intent.putExtra("phone", filteredUsers.get(position).getPhone());
            startActivity(intent);
        });
        myPhone = PreferenceManager.getDefaultSharedPreferences(this).getString(USER_PHONE_KEY, "");
        //check network connectivity
        onNetworkConnectionChanged(ConnectivityReceiver.isNetworkConnected());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            isContactsPermissionGranted = false;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    READ_CONTACTS_PERMISSION);
        }
        registerForCallback();
    }

    /**
     * Show the contacts in the ListView.
     */
    private void showContacts() {
        updateFilteredUserList(this.filteredUsers, this, false);
        spinner.setVisibility(View.INVISIBLE);
        UsersListAdapter adapter = new UsersListAdapter(this, filteredUsers);
        contactsListView.setAdapter(adapter);
        //store thumb urls locally
        synchronized (this) {
            new Thread(() -> {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                filteredUsers.forEach(user -> {
                    boolean isGroup = user.isGroup();
                    if (isGroup) {
                        //store group indicator locally
                        editor.putBoolean(user.getPhone(), user.isGroup());
                    }
                    if (user.getThumbUrl() != null) {
                        if(!isGroup) {
                            //store users thumb url locally
                            editor.putString(user.getPhone(), user.getThumbUrl().toString());
                        }
                    }
                });
                //store my thumb url locally
                User myUser = FireStoreUsersListener.users.get(myPhone);
                if (myUser != null && myUser.getThumbUrl() != null) {
                    editor.putString(myPhone, myUser.getThumbUrl().toString());
                }
                editor.apply();
            }).start();
        }
    }

    /**
     * A method filters the whole T2T user list,
     * to users list that exist in user phone book
     * @param filteredUsersList - the filtered list
     * @param context           - activity that call that method
     * @param isSelectContacts  - if in select contacts mode for creating group channel
     */
    static void updateFilteredUserList(List<User> filteredUsersList, Context context, boolean isSelectContacts) {
        filteredUsersList.clear();
        FireStoreUsersListener.users.keySet().forEach(phone -> {
            if ((contactExists(context, phone) && !phone.equals(myPhone) && !phone.contains("_")) ||
                    (phone.startsWith(myPhone + "_") && !isSelectContacts)) {
                filteredUsersList.add(FireStoreUsersListener.users.get(phone));
            }
        });
    }

    /**
     * A callback method called on request permission result
     * @param requestCode - code of request permission
     * @param permissions - permissions strings as array
     * @param grantResults - the results of the requested permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        requestPermissionResult(this, requestCode, grantResults);
        if (requestCode == READ_CONTACTS_PERMISSION && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Timer timer = new Timer();
            int interval = 3000;
            TimerTask closeApp = new TimerTask() {
                @Override
                public void run() {
                    //closing the app
                    finish();
                    System.exit(0);
                }
            };
            timer.schedule(closeApp, interval);
        } else {
            isContactsPermissionGranted = true;
        }
    }

    /**
     * A method that checks if user of T2T exits in user's phone book
     * @param context - context
     * @param number  - the number to check
     * @return - true if the number exists in phone book, false otherwise
     */
    public static boolean contactExists(Context context, String number) {
        //number is the phone number
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        String[] mPhoneNumberProjection = {ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER, ContactsContract.PhoneLookup.DISPLAY_NAME};
        try (Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null)) {
            if (cur.moveToFirst()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //check contacts permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            isContactsPermissionGranted = false;
        } else {
            isContactsPermissionGranted = true;
            showContacts();
        }
        //register for update ui callback
        registerForCallback();
        // register connection status listener
        T2T.getInstance().setConnectivityListener(this);
        //check network connectivity
        boolean isConnected = ConnectivityReceiver.isNetworkConnected();
        if (!isConnected) {
            onNetworkConnectionChanged(ConnectivityReceiver.isNetworkConnected());
        }
        //update to online mode
        new UpdateConnectivityToAppTask(myPhone).execute(true);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //showContacts();
        registerForCallback();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (fireStoreUsersListener != null) {
            fireStoreUsersListener.setCallbacks(null);//unregister
        }
    }

    /**
     * A method that creates intent to SettingsActivity
     * @param view - view represent the button that was pressed
     */
    public void OpenSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * A method that creates intent to SelectContactsActivity
     * @param view - view represent the button that was pressed
     */
    public void AddConversation(View view) {
        Intent intent = new Intent(this, SelectContactsActivity.class);
        startActivity(intent);
    }

    /**
     * A method that checks if string is null or empty
     * @param element - the string to check
     * @return - true if the string is not null and not empty, false otherwise
     */
    public static boolean checkNullOrEmpty(String element) {
        return element != null && !element.isEmpty();
    }

    /**
     * A method that registers for ui updates callback
     */
    private void registerForCallback() {
        if (fireStoreUsersListener != null) {
            fireStoreUsersListener.setCallbacks(ConversationsActivity.this);
        }
    }

    /**
     * A callback method that called when data changed and ui update is necessary
     */
    @Override
    public void updateUi() {
        showContacts();
    }

    /**
     * A callback method that called when network connectivity changed
     * @param isConnected - network connectivity status
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        ConnectivityReceiver.showSnack(findViewById(android.R.id.content), isConnected);
        if (isConnected) {
            showContacts();
            findViewById(R.id.add_conversation).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.add_conversation).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * A method checks if contacts permission is granted
     * @return true if contacts permission is granted, false otherwise
     */
    static boolean isIsContactsPermissionGranted() {
        return isContactsPermissionGranted;
    }

    /**
     * A private static class represent async task which updates connectivity to app status
     */
     static class UpdateConnectivityToAppTask extends AsyncTask<Boolean,Void,Boolean>{

         private String myPhone;

         UpdateConnectivityToAppTask(String myPhone)
         {
             this.myPhone = myPhone;
         }

        @Override
        protected Boolean doInBackground(Boolean... booleans) {
            boolean isOnline = booleans[0];
            //firestore update
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(myPhone).update("isOnline", isOnline)
                    .addOnSuccessListener(aVoid -> Log.d(Constants.TAG, "DocumentSnapshot successfully updated!"))
                    .addOnFailureListener(e -> Log.w(Constants.TAG, "Error updating document", e));
            //realtime db update
            FirebaseDatabase firebaseDatabase =  FirebaseDatabase.getInstance();
            DatabaseReference mDatabase = firebaseDatabase.getReference();
            mDatabase.child("status").child(myPhone).child("isOnline").setValue(true);
            DatabaseReference userLastOnlineRef = firebaseDatabase.getReference("/status/" + myPhone + "/isOnline");
            userLastOnlineRef.onDisconnect().setValue(false);
            return isOnline;
        }
    }
}
