package com.example.a201.t2t;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.wang.avi.AVLoadingIndicatorView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import de.hdodenhof.circleimageview.CircleImageView;
import static com.example.a201.t2t.Constants.ALL_PERMISSIONS;
import static com.example.a201.t2t.Constants.CAMERA_AND_WRITE_STORAGE_PERMISSION;
import static com.example.a201.t2t.Constants.IS_USER_REGISTERED_KEY;
import static com.example.a201.t2t.Constants.PERMISSIONS;
import static com.example.a201.t2t.Constants.READ_STORAGE_PERMISSION;
import static com.example.a201.t2t.Constants.USER_NAME_KEY;
import static com.example.a201.t2t.Constants.USER_PHONE_KEY;
import static com.example.a201.t2t.Constants.requestPermissionResult;
import static com.example.a201.t2t.ImageUtils.CAMERA_REQUEST;
import static com.example.a201.t2t.ImageUtils.GALLERY_PICTURE;
import static com.example.a201.t2t.ImageUtils.rotateImageIfRequired;

/**
 * A class represent activity that shows registration screen
 */
public class RegistrationActivity extends AppCompatActivity implements ConnectivityReceiverListener {

    CircleImageView userProfilePhoto;
    private Button register;
    private boolean userExists;
    private Uri imageUri;
    private Bitmap userPhoto;
    private AVLoadingIndicatorView registerProgressBar;
    private boolean isConnect;
    /**
     * A callback method called when that activity starts to create
     * @param savedInstanceState - a bundle that holds save state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //enable full screen

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).hide();
        setContentView(R.layout.activity_registration);
        userProfilePhoto = findViewById(R.id.user_profile_photo);
        register = findViewById(R.id.btnRegister);

        //Ask permissions
        ActivityCompat.requestPermissions(this, PERMISSIONS.keySet().toArray(new String[0]), ALL_PERMISSIONS);

        //initialize progress bar
        registerProgressBar = findViewById(R.id.registerProgressBar);
        registerProgressBar.hide();

        //get default image user
        imageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getBaseContext().getResources().getResourcePackageName(R.drawable.default_image)
                + '/' + getBaseContext().getResources().getResourceTypeName(R.drawable.default_image)
                + '/' + getBaseContext().getResources().getResourceEntryName(R.drawable.default_image));

        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + "T2T" + File.separator + "records");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register connection status listener
        T2T.getInstance().setConnectivityListener(this);
        //check network connectivity
        boolean isConnected = ConnectivityReceiver.isNetworkConnected();
        isConnect = isConnected;
        if (!isConnected) {
            onNetworkConnectionChanged(ConnectivityReceiver.isNetworkConnected());
        }
    }

    /**
     * A callback method that called when network connectivity changed
     * @param isConnected - network connectivity status
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        ConnectivityReceiver.showSnack(findViewById(android.R.id.content), isConnected);
        if (!isConnected && register.isEnabled()) {
            register.setEnabled(false);
            register.setBackground(ContextCompat.getDrawable(this, R.drawable.round_shape_btn_gray));
            AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
            myAlertDialog.setTitle("T2T - Connectivity");
            myAlertDialog.setMessage("In order to register T2T system, Internet access is required");
            myAlertDialog.show();
        }
        else if(isConnected && !register.isEnabled())
        {
            register.setEnabled(true);
            register.setBackground(ContextCompat.getDrawable(this, R.drawable.round_shape_btn_green));
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

    /**
     * A method that registers new user to T2T system
     * @param view - view represent the button that was pressed
     */
    public void register(View view) {
        registerProgressBar.smoothToShow();
        String user = ((EditText) findViewById(R.id.userNameEditText)).getText().toString();
        String phone = ((EditText) findViewById(R.id.phoneNumberEditText)).getText().toString();
        if (validateUserData(user, phone)) {
            registerUser(user, phone);
        } else {
            registerProgressBar.smoothToHide();
        }
    }

    /**
     *A method that validates the data the user inserted
     * @param user - user's name
     * @param phone - user's phone
     * @return - true if the data is valid, false otherwise
     */
    private boolean validateUserData(String user, String phone) {
        if (user.trim().equals("")) {
            Toast.makeText(this, "User name can not be empty", Toast.LENGTH_SHORT).show();
            return false;
        } else if (phone.length() != 10) {
            Toast.makeText(this, "Phone number length must be 10 numbers", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!(phone.startsWith("052") || phone.startsWith("050") || phone.startsWith("054") ||
                phone.startsWith("053") || phone.startsWith("057") || phone.startsWith("055"))) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * A method that inserts user to firebase cloud db,
     * if the user is not already registered to system
     * @param userName - user's name
     * @param phone - user's phone
     */
    private void registerUser(String userName, String phone) {
        userExists = false;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                Log.d("T2T-DB", "document => " + document.get("phone"));
                if (phone.equals(document.get("phone"))) {
                    userExists = true;
                    registerProgressBar.smoothToHide();
                    Toast.makeText(this, "User with phone number " + phone + " already exists in our system",
                            Toast.LENGTH_SHORT).show();
                }
            }
            if (!userExists) {
                //createNewRegistrationToken();
                addContactToDB(userName, phone);
                Toast.makeText(this, "Registration complete", Toast.LENGTH_SHORT).show();
                registerProgressBar.smoothToHide();
                //move to conversations activity
                finish();
                Intent intent = new Intent(this, ConversationsActivity.class);
                startActivity(intent);
                //upload user photo to cloud
                ImageUtils.uploadImage(phone, userPhoto, imageUri, this);
                //add user phone number to shared preferences
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                editor.putString(USER_NAME_KEY, userName);
                editor.putString(USER_PHONE_KEY, phone);
                editor.putBoolean(IS_USER_REGISTERED_KEY, true);
                editor.apply();
                //register for topic to receive records
                FirebaseMessaging.getInstance().subscribeToTopic(phone)
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Log.d("T2T_TOPICS", "Failed to subscribe topic");
                            }
                        });
            }
        });
    }

    /**
     * Private method that does the actual insert to firebase cloud db
     * @param userName - user's name
     * @param phoneNumber - user's phone
     */
    private void addContactToDB(String userName, String phoneNumber) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();
        user.put("name", userName);
        user.put("phone", phoneNumber);
        user.put("isGroup", false);
        user.put("isOnline", false);
        user.put("imageUrl", "");
        user.put("thumbUrl", "");
        user.put("registrationToken", T2tMessagingService.getToken(this));

        db.collection("users").document(phoneNumber)
                .set(user)
                .addOnSuccessListener(aVoid -> Log.d("T2T-DB", "DocumentSnapshot successfully written!"))
                .addOnFailureListener(e -> Log.d("T2T-DB", "Error writing document", e));
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
                Drawable drawable = new BitmapDrawable(getResources(), rotateImageIfRequired(bitmap, imageUri, this));
                userProfilePhoto.setImageDrawable(drawable);
                Toast.makeText(this, "Profile photo added", Toast.LENGTH_SHORT).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
                Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CAMERA_REQUEST) {
            try {
                userPhoto = rotateImageIfRequired(MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri), imageUri, this);
                userProfilePhoto.setImageBitmap(userPhoto);
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
}
