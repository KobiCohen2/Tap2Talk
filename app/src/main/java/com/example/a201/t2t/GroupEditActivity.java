package com.example.a201.t2t;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import static com.example.a201.t2t.Constants.USER_PHONE_KEY;
import static com.example.a201.t2t.SelectContactsActivity.createAndAddGroup;

/**
 * A class represent activity for edit existing group channel
 */
public class GroupEditActivity extends AppCompatActivity implements ConnectivityReceiverListener {

    private String phone;
    private String myPhone;

    /**
     * A callback method called when that activity starts to create
     * @param savedInstanceState - a bundle that holds save state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_edit);

        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.group_edit_actionbar);
        View view = actionBar.getCustomView();
        ImageButton backButton = view.findViewById(R.id.backIconInfo);
        backButton.setOnClickListener(v -> finish());

        //information form intent
        phone = getIntent().getExtras().getString("phone");
        List<String> usersInGroup = new ArrayList<>(Arrays.asList(phone.split("_")));
        usersInGroup.remove(0);

        //mark user's group
        SelectContactsListAdapter.usersState.entrySet().forEach(entry -> {
            if(usersInGroup.contains(entry.getKey().getPhone()))
            {
                entry.setValue(true);
            }
        });

        //fill list view
        ListView contactsListView = findViewById(R.id.edit_group_list_view);
        SelectContactsActivity.fillAdapter(this, contactsListView, false);

        //group info
        TextView groupInfo = findViewById(R.id.group_info_name);
        groupInfo.setText(String.format("A group with %d users", usersInGroup.size()));

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
     * A method that deletes existing group
     * @param view - view represent the button that was pressed
     */
    public void deleteGroup(View view) {

        deleteGroupFromDB(false);
        backToConversationsActivity();
    }

    /**
     * A method that confirms edited group
     * @param view - view represent the button that was pressed
     */
    public void confirmGroup(View view) {
        if(createAndAddGroup(view, myPhone))
        {
            deleteGroupFromDB(true);
            Toast.makeText(view.getContext(), "The group communication channel edited successfully", Toast.LENGTH_LONG).show();
            backToConversationsActivity();
        }
    }

    /**
     * A method that deletes group from firestore db
     * @param isEditGroup - if in edit group mode
     */
    private void deleteGroupFromDB(boolean isEditGroup)
    {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(phone)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if(!isEditGroup){Toast.makeText(GroupEditActivity.this, "Group deleted successfully", Toast.LENGTH_LONG).show();}
                })
                .addOnFailureListener(e -> Log.w("T2T", "Error deleting group document", e));
    }

    /**
     * A method that back to conversations activity after delete or edit group
     */
    private void backToConversationsActivity()
    {
        //back to ConversationsActivity
        Intent intent = new Intent(this, ConversationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * A callback method that called when network connectivity changed
     * @param isConnected - network connectivity status
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        ConnectivityReceiver.showSnack(findViewById(android.R.id.content), isConnected);
        Button delete = findViewById(R.id.group_info_delete_btn);
        Button confirm = findViewById(R.id.group_info_confirm_btn);
        if (isConnected) {
            delete.setVisibility(View.VISIBLE);
            confirm.setVisibility(View.VISIBLE);
        }
        else
        {
            delete.setVisibility(View.INVISIBLE);
            confirm.setVisibility(View.INVISIBLE);
        }
    }
}
