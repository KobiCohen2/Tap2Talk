package com.example.a201.t2t;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import java.util.HashMap;
import java.util.Map;

/**
 * A class represent constants that used by the system
 */
public class Constants {

    static final String USER_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/tap2talk-321c7.appspot.com/o/images%2F{0}?alt=media";

    static final String USER_THUMB_URL = "https://firebasestorage.googleapis.com/v0/b/tap2talk-321c7.appspot.com/o/images%2Fthumb_{0}?alt=media";

    static final String TAG = "T2T";

    static final String REGISTRATION_TOKEN_KEY = "registrationToken";

    static final String USER_PHONE_KEY = "phone";

    static final String USER_NAME_KEY = "name";

    static final String IS_USER_REGISTERED_KEY = "isRegistered";

    static final int CAMERA_PERMISSION = 100;

    static final int READ_STORAGE_PERMISSION = 101;

    static final int WRITE_STORAGE_PERMISSION = 102;

    static final int READ_CONTACTS_PERMISSION = 103;

    static final int RECORD_AUDIO_PERMISSION = 104;

    static final int CAMERA_AND_WRITE_STORAGE_PERMISSION = 105;

    static final int RECORD_AUDIO_AND_STORAGE_PERMISSION = 106;

    static final int ALL_PERMISSIONS = 107;

    static final Map<String, Integer> PERMISSIONS;

    static {
        PERMISSIONS = new HashMap<>();
        PERMISSIONS.put(Manifest.permission.CAMERA, CAMERA_PERMISSION);
        PERMISSIONS.put(Manifest.permission.READ_EXTERNAL_STORAGE, READ_STORAGE_PERMISSION);
        PERMISSIONS.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_STORAGE_PERMISSION);
        PERMISSIONS.put(Manifest.permission.READ_CONTACTS, READ_CONTACTS_PERMISSION);
        PERMISSIONS.put(Manifest.permission.RECORD_AUDIO, RECORD_AUDIO_PERMISSION);
    }

    static void requestPermissionResult(Context context, int requestCode, int[] grantResults)
    {
        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(context);
        myAlertDialog.setTitle("T2T - Permissions");
        loop: for(int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                switch (requestCode) {
                    case CAMERA_PERMISSION:
                        myAlertDialog.setMessage("In order to take a picture we need camera permission");
                        myAlertDialog.show();
                        break loop;
                    case READ_STORAGE_PERMISSION:
                        myAlertDialog.setMessage("In order to pick a picture or load records we need read storage permission");
                        myAlertDialog.show();
                        break loop;
                    case WRITE_STORAGE_PERMISSION:
                        myAlertDialog.setMessage("In order to take a picture, or store records we need write storage permission");
                        myAlertDialog.show();
                        break loop;
                    case READ_CONTACTS_PERMISSION:
                        myAlertDialog.setMessage("In order to present T2T users, we need read contacts permission\nThe app will close now...");
                        myAlertDialog.show();
                        break loop;
                    case RECORD_AUDIO_PERMISSION:
                        myAlertDialog.setMessage("In order to record voice message we need record audio permission");
                        myAlertDialog.show();
                        break loop;
                    case CAMERA_AND_WRITE_STORAGE_PERMISSION:
                        myAlertDialog.setMessage("In order to take a picture we need camera and write external storage permissions");
                        myAlertDialog.show();
                        break loop;
                    case RECORD_AUDIO_AND_STORAGE_PERMISSION:
                        myAlertDialog.setMessage("In order to record voice message we need all the permissions presented earlier");
                        myAlertDialog.show();
                        break loop;
                    case ALL_PERMISSIONS:
                        myAlertDialog.setMessage("In order that the application will works properly, it needs all the permissions presented earlier");
                        myAlertDialog.show();
                        break loop;
                }
            }
            else
            {
                if(result == RECORD_AUDIO_AND_STORAGE_PERMISSION && context instanceof ConversationActivity)
                    ((ConversationActivity)context).showRecords();
            }
        }
    }

}
