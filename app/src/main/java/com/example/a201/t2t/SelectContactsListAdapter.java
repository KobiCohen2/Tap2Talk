package com.example.a201.t2t;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A class represent list adapter to present list view of users
 */
public class SelectContactsListAdapter extends ArrayAdapter<User> {
    private Context mContext;
    List<User> list;
    static Map<User, Boolean> usersState = Collections.synchronizedMap(new HashMap<>());
    static Map<User, Boolean> checkBoxesState = Collections.synchronizedMap(new HashMap<>());
    private boolean isInCreateGroupState;

    /**
     * A constructor
     * @param context - context that filled the adapter
     * @param list - list of users
     * @param isInCreateGroupState - if in create group mode
     */
    SelectContactsListAdapter(Context context, List<User> list, boolean isInCreateGroupState) {
        super(context,0, list);
        mContext = context;
        this.list = list;
        this.isInCreateGroupState = isInCreateGroupState;
    }

    /**
     * A method that prepare ui view in specific position
     * @param position - view position
     * @param convertView - the convert view
     * @param parent - the parent view
     * @return the convert view
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.select_contact_list_item,parent,false);

        User user = list.get(position);
        String currentPerson = user.getName();
        String currentPhone = user.getPhone();
        TextView name = listItem.findViewById(R.id.select_contact_list_item_name);
        TextView phone = listItem.findViewById(R.id.select_contact_list_item_phone);
        CheckBox checkBox = listItem.findViewById(R.id.select_contacts_checkbox);

        //set name and phone
        name.setText(currentPerson);
        phone.setText(currentPhone);

        //if not in create group mode, i.e. in edit group mode
        //check all checkBoxes of the group members
        if(isInCreateGroupState) { checkBoxesState.put(user, false); } //clear state in create group mode
        else { checkBox.setChecked(usersState.get(user)); } //mark checkBoxes in edit mode

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> checkBoxesState.put(user, isChecked));

        //in edit group mode, set checkBoxes state according to group members
        if(!isInCreateGroupState)
        {
            //check existing members
            SelectContactsListAdapter.usersState.forEach((userState, isChecked) -> SelectContactsListAdapter.checkBoxesState.put(userState, isChecked));
        }

        CircleImageView imageView = listItem.findViewById(R.id.listItemUserImage);
        imageView.setImageResource(R.drawable.default_image);
        Uri userImage = user.getThumbUrl();
        Uri oldUserImage = user.getOldThumbUrl();
        if(userImage != null)
        {
            if (oldUserImage!= null && !oldUserImage.equals(userImage))
            {
                Picasso.get().invalidate(oldUserImage);
                user.setOldThumbUrl(userImage.toString());
            }
            Picasso.get().load(userImage).placeholder(R.drawable.default_image).into(imageView);
        }

        return listItem;
    }
}
