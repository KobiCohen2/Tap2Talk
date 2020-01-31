package com.example.a201.t2t;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import java.text.MessageFormat;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;
import static com.example.a201.t2t.Constants.USER_THUMB_URL;

/**
 * A class represent list adapter to present list view of users
 */
public class UsersListAdapter extends ArrayAdapter<User> {

    private Context mContext;
    private List<User> list;

    /**
     * A constructor
     * @param context - context that filled the adapter
     * @param list - list of users
     */
    UsersListAdapter(Context context, List<User> list) {
        super(context,0, list);
        mContext = context;
        this.list = list;
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
            listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item,parent,false);

        User user = list.get(position);
        String currentPerson = user.getName();
        String currentPhone = user.getPhone();
        TextView name = listItem.findViewById(R.id.listItemName);
        TextView phone = listItem.findViewById(R.id.select_contact_list_item_phone);
        name.setText(currentPerson);
        phone.setText(currentPhone);


        CircleImageView imageView = listItem.findViewById(R.id.listItemUserImage);
        imageView.setImageResource(R.drawable.default_image);
        Uri userImage = user.getThumbUrl();
        Uri oldUserImage = user.getOldThumbUrl();
        Uri thumbUrl = Uri.parse(MessageFormat.format(USER_THUMB_URL, currentPhone));
        Picasso picasso = Picasso.get();
        if(userImage != null)
        {
            if (oldUserImage!= null && !oldUserImage.equals(userImage))
            {
                picasso.invalidate(thumbUrl);
                user.setOldThumbUrl(userImage.toString());
                picasso.load(thumbUrl).networkPolicy(NetworkPolicy.NO_CACHE).placeholder(R.drawable.default_image).into(imageView);
            }
            else
            {
                picasso.load(thumbUrl).placeholder(R.drawable.default_image).into(imageView);
            }
        }

        if(user.isGroup())
        {
            imageView.setImageResource(R.drawable.megaphone);
            String localPhone = currentPhone;
            //String localName = currentPerson;
            String[] numOfUsers = localPhone.split("_");
            localPhone = "A group communication channel with " + (numOfUsers.length-1) + " people";
            phone.setText(localPhone);
            phone.setTextSize(12);
        }
        else {
            //set up border color
            int color = user.isOnline() ? ContextCompat.getColor(mContext, R.color.green) : ContextCompat.getColor(mContext, R.color.softRed);
            imageView.setBorderColor(color);
            //fill non group users for edit group option
            SelectContactsListAdapter.usersState.put(user, false);
        }

        return listItem;
    }
}
