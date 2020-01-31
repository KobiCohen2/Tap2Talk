package com.example.a201.t2t;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static com.example.a201.t2t.Constants.USER_PHONE_KEY;

/**
 * A class represent activity that shows contacts to create group communication channel
 */
public class SelectContactsActivity extends AppCompatActivity implements ConnectivityReceiverListener {

    static List<User> filteredUsers = Collections.synchronizedList(new ArrayList<>());
    private static String myPhone;

    /**
     * A callback method called when that activity starts to create
     * @param savedInstanceState - a bundle that holds save state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contacts);

        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.select_contacts_actionbar);
        View view = actionBar.getCustomView();

        ImageButton backButton = view.findViewById(R.id.backIcon);
        backButton.setOnClickListener(v -> finish());

        ListView contactsListView = findViewById(R.id.select_contacts_list_view);
        fillAdapter(this, contactsListView, true);

        myPhone = PreferenceManager.getDefaultSharedPreferences(this).getString(USER_PHONE_KEY, "");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register connection status listener
        T2T.getInstance().setConnectivityListener(this);
        //check network connectivity
        boolean isConnected = ConnectivityReceiver.isNetworkConnected();
        if (!isConnected) {
            onNetworkConnectionChanged(ConnectivityReceiver.isNetworkConnected());
        }
        //update to online mode
        new ConversationsActivity.UpdateConnectivityToAppTask(myPhone).execute(true);
    }

    /**
     * A method that called when user completes to select contacts
     * @param view - view represent the button that was pressed
     */
    public void contactsSelectionComplete(View view) {
        if(createAndAddGroup(view, myPhone))
        {
            Toast.makeText(view.getContext(), "The group communication channel created successfully", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * A method that creates and add group to firebase cloud db
     * @param view - view represent the button that was pressed
     * @param myPhone - my number phone
     * @return true if the group successfully created
     */
    static boolean createAndAddGroup(View view, String myPhone)
    {
        StringBuilder names = new StringBuilder();
        StringBuilder phones = new StringBuilder(myPhone);

        List<User> checkedUsers = getCheckedUsers();
        checkedUsers.forEach(checkedUser -> {
            names.append(checkedUser.getName()).append(",");
            phones.append("_").append(checkedUser.getPhone());

        });

        if(checkedUsers.size() < 2)
        {
            Toast.makeText(view.getContext(), "A group communication channel must have at least two contacts", Toast.LENGTH_LONG).show();
            return false;
        }

        String phone = phones.toString();
        if(checkIfGroupIsAlreadyExists(phone, myPhone))
        {
            Toast.makeText(view.getContext(), "A group communication channel with this contacts already exists", Toast.LENGTH_LONG).show();
            return false;
        }
        else
        {
            String name = names.toString().substring(0, names.length() - 1);
            User user = new User(name, phone, null);
            user.setGroup(true);
            //upload to firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("name", name);
            userMap.put("phone", phone);
            userMap.put("isGroup", true);

            db.collection("users").document(phone)
                    .set(userMap)
                    .addOnSuccessListener(aVoid -> Log.d("T2T-DB", "DocumentSnapshot successfully written!"))
                    .addOnFailureListener(e -> Log.d("T2T-DB", "Error writing document", e));

            //add to shared preferences, to know locally if it's a group
            PreferenceManager.getDefaultSharedPreferences(view.getContext())
                    .edit().putBoolean(phone, true).apply();
        }
        return true;
    }

    /**
     * A method that checks if group is already exists
     * @param myGroup - the current group to be checked
     * @param myPhone - my number phone
     * @return - true if the group is already exists, false otherwise
     */
    static boolean checkIfGroupIsAlreadyExists(String myGroup, String myPhone)
    {
        List<String> currentGroup = Arrays.asList(myGroup.split("_"));
        for(String group : FireStoreUsersListener.users.keySet())
        {
            List<String> members = Arrays.asList(group.split("_"));
            if(members.size() == currentGroup.size() && members.containsAll(currentGroup) && group.startsWith(myPhone))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * A method that retrieve all the selected users
     * @return the selects uses as list
     */
    static List<User> getCheckedUsers()
    {
        List<User> checkedUsers = new ArrayList<>();
        SelectContactsListAdapter.checkBoxesState.forEach((user, isChecked) ->{
            if (isChecked)
            {
                checkedUsers.add(user);
            }
        });
        return checkedUsers;
    }

    /**
     * A method that fills the adapter
     * @param activity - the parent of the adapter
     * @param contactsListView - the list view
     * @param isCreateState - if in create group mode
     */
    static void fillAdapter(AppCompatActivity activity, ListView contactsListView, boolean isCreateState)
    {
        ConversationsActivity.updateFilteredUserList(filteredUsers, activity, true);
        SelectContactsListAdapter adapter;

        //sort users in edit mode to see checked users first
        if(!isCreateState)
        {
            List<User> sortedUsersList = sortCheckedUsers(filteredUsers);
            adapter = new SelectContactsListAdapter(activity, sortedUsersList, false);
        }
        else
        {
            adapter = new SelectContactsListAdapter(activity, filteredUsers, true);
        }
        contactsListView.setAdapter(adapter);
    }

    /**
     * A method that bring checked users to the beginning of the list
     * @param filteredUsers - list of filtered users
     * @return - sorted users list
     */
    static List<User> sortCheckedUsers(List<User> filteredUsers)
    {
        List<User> sortedUsersList = new ArrayList<>();
        List<User> uncheckedUsers = new ArrayList<>(filteredUsers);
        filteredUsers.forEach(user -> {
            if(SelectContactsListAdapter.usersState.get(user))
            {
                sortedUsersList.add(user);
                uncheckedUsers.remove(user);
            }
        });
        sortedUsersList.addAll(uncheckedUsers);//add the rest
        return sortedUsersList;
    }

    /**
     * A callback method that called when network connectivity changed
     * @param isConnected - network connectivity status
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        ConnectivityReceiver.showSnack(findViewById(android.R.id.content), isConnected);
        if (isConnected) {
            findViewById(R.id.create_group).setVisibility(View.VISIBLE);
        }
        else
        {
            findViewById(R.id.create_group).setVisibility(View.INVISIBLE);
        }
    }
}
