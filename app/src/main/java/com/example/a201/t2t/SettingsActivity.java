package com.example.a201.t2t;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Objects;
import static com.example.a201.t2t.Constants.CAMERA_AND_WRITE_STORAGE_PERMISSION;
import static com.example.a201.t2t.Constants.READ_STORAGE_PERMISSION;
import static com.example.a201.t2t.Constants.USER_IMAGE_URL;
import static com.example.a201.t2t.Constants.USER_NAME_KEY;
import static com.example.a201.t2t.Constants.USER_PHONE_KEY;
import static com.example.a201.t2t.Constants.requestPermissionResult;
import static com.example.a201.t2t.ImageUtils.CAMERA_REQUEST;
import static com.example.a201.t2t.ImageUtils.GALLERY_PICTURE;
import static com.example.a201.t2t.ConversationActivity.RECORDS_LOCATION;

/**
 * A class represent activity that shows settings screen
 */
public class SettingsActivity extends AppCompatActivity implements ConnectivityReceiverListener {

    private ImageView userProfilePhoto;
    private Uri imageUri;
    private User user;
    private String myPhone;
    private TextView nickname;
    private ImageView editNickname;

    /**
     * A callback method called when that activity starts to create
     * @param savedInstanceState - a bundle that holds save state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        userProfilePhoto = findViewById(R.id.settings_user_profile_photo);
        nickname = findViewById(R.id.nickname);
        editNickname = findViewById(R.id.editNickname);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.settings_actionbar);
        View view = getSupportActionBar().getCustomView();
        ImageButton backButton = view.findViewById(R.id.backIconSettings);
        backButton.setOnClickListener(v -> finish());

        myPhone = PreferenceManager.getDefaultSharedPreferences(this).getString(USER_PHONE_KEY, "");
        user = FireStoreUsersListener.users.get(myPhone);

        //load user image
        Uri imageUrl = Uri.parse(MessageFormat.format(USER_IMAGE_URL, myPhone));
        Picasso picasso = Picasso.get();
        picasso.invalidate(imageUrl);
        picasso.load(imageUrl).networkPolicy(NetworkPolicy.NO_CACHE).placeholder(R.drawable.default_image).into(userProfilePhoto);

        //set user nickname
        if (user != null) {
            nickname.setText(user.getName());
        } else {
            nickname.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(USER_NAME_KEY, ""));
        }
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
     * A method that starts picture picking dialog
     * @param view - view represent the button that was pressed
     */
    public void addPicture(View view) {
        startDialog();
    }

    /**
     * A callback method called when user chose option in the dialog
     * @param requestCode - the code to identify the chosen option
     * @param resultCode - the result code
     * @param data - the retrieve data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GALLERY_PICTURE) {
            try {
                imageUri = data.getData();
                InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                bitmap = ImageUtils.rotateImageIfRequired(bitmap, imageUri, this);
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                userProfilePhoto.setImageDrawable(drawable);
                Toast.makeText(this, "Profile photo changed", Toast.LENGTH_SHORT).show();
                ImageUtils.uploadImage(user.getPhone(), bitmap, imageUri, this);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
                Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CAMERA_REQUEST) {
            try {
                Bitmap userPhoto = ImageUtils.rotateImageIfRequired(MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri), imageUri, this);
                userProfilePhoto.setImageBitmap(userPhoto);
                ImageUtils.uploadImage(user.getPhone(), userPhoto, imageUri, this);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "No photo captured", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * A method that starts the picking picture dialog
     */
    private void startDialog() {
        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
        myAlertDialog.setTitle("Upload Pictures Option");
        myAlertDialog.setMessage("How do you want to set your picture?");

        myAlertDialog.setPositiveButton("Gallery",
                (arg0, arg1) -> {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                READ_STORAGE_PERMISSION);
                    } else {
                        Intent pictureActionIntent = new Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(pictureActionIntent, GALLERY_PICTURE);
                    }
                });

        myAlertDialog.setNegativeButton("Camera",
                (arg0, arg1) -> {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            CAMERA_AND_WRITE_STORAGE_PERMISSION);

                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    == PackageManager.PERMISSION_GRANTED) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.TITLE, "New Picture");
                        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
                        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                        startActivityForResult(intent, CAMERA_REQUEST);
                    }
                });
        myAlertDialog.show();
    }


    /**
     * A method for change user nickname
     * @param view - view represent the button that was pressed
     */
    public void changeNickname(View view) {
        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptsView = layoutInflater.inflate(R.layout.popup_change_nickname, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alert dialog builder
        alertDialogBuilder.setView(promptsView);
        final EditText userInput = promptsView.findViewById(R.id.new_nickname_et);
        final Button okBtn = promptsView.findViewById(R.id.popup_ok_btn);
        final Button cancelBtn = promptsView.findViewById(R.id.popup_cancel_btn);
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        //fill edit text
        String currentNickname = nickname.getText().toString();
        userInput.setText(currentNickname);
        userInput.setSelection(currentNickname.length());
        // show it
        alertDialog.show();

        okBtn.setOnClickListener(v -> {
            String editedNickname = userInput.getText().toString();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(user.getPhone())
                    .update(
                            "name", editedNickname
                    );
            Toast.makeText(SettingsActivity.this, "Nickname changed", Toast.LENGTH_SHORT).show();
            nickname.setText(editedNickname);
            alertDialog.dismiss();
        });

        cancelBtn.setOnClickListener(v -> alertDialog.dismiss());
    }

    /**
     * A method for navigate to records in file system
     * @param view - view represent the button that was pressed
     */
    public void goToRecordsDirectory(View view) {
        Intent intent = new Intent();
        Uri uri = Uri.parse(RECORDS_LOCATION);
        intent.setDataAndType(uri, "resource/folder");
        startActivity(Intent.createChooser(intent, "Open folder"));
    }

    /**
     * A method for navigate to application permissions in system
     * @param view - view represent the button that was pressed
     */
    public void changePermissions(View view) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    /**
     * A callback method that called when network connectivity changed
     * @param isConnected - network connectivity status
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        ConnectivityReceiver.showSnack(findViewById(android.R.id.content), isConnected);
        if (!isConnected && userProfilePhoto.isEnabled()) {
            userProfilePhoto.setEnabled(false);
            editNickname.setVisibility(View.INVISIBLE);
        }
        else if(isConnected && !userProfilePhoto.isEnabled())
        {
            userProfilePhoto.setEnabled(true);
            editNickname.setVisibility(View.VISIBLE);
        }
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
    }
}
