package com.example.a201.t2t;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.wang.avi.AVLoadingIndicatorView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import static com.example.a201.t2t.Constants.READ_STORAGE_PERMISSION;
import static com.example.a201.t2t.Constants.RECORD_AUDIO_AND_STORAGE_PERMISSION;
import static com.example.a201.t2t.Constants.RECORD_AUDIO_PERMISSION;
import static com.example.a201.t2t.Constants.USER_NAME_KEY;
import static com.example.a201.t2t.Constants.USER_PHONE_KEY;
import static com.example.a201.t2t.Constants.WRITE_STORAGE_PERMISSION;
import static com.example.a201.t2t.Constants.requestPermissionResult;

/**
 * A class represent activity that shows specific conversation
 */
public class ConversationActivity extends AppCompatActivity implements ServiceCallbacks, ConnectivityReceiverListener {

    private ListView recordList;
    private String phone;
    private String myPhone;
    private String myName;
    private MediaRecorder myAudioRecorder;
    private MediaPlayer mediaPlayer;
    private ImageButton infoButton;
    private boolean isRecording = false;
    private boolean isGroup;
    private String currentRecord;
    private DownloadRecordTask task;
    RecordsListAdapter recordsListAdapter;
    RecordsListAdapter adapter;
    public static final String RECORDS_LOCATION = Environment.getExternalStorageDirectory().getAbsolutePath() + "/T2T/records";
    public static final String REGEX = "(.)+_(.)+_(.)+";

    /**
     * A callback method called when that activity starts to create
     * @param savedInstanceState - a bundle that holds save state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        myPhone = PreferenceManager.getDefaultSharedPreferences(this).getString(USER_PHONE_KEY, "");
        myName = PreferenceManager.getDefaultSharedPreferences(this).getString(USER_NAME_KEY, "");
        recordList = findViewById(R.id.recordsList);
        Intent intent = this.getIntent();
        String name = intent.getExtras().getString("name");
        phone = intent.getExtras().getString("phone");
        ImageButton recordBtn = findViewById(R.id.recordBtn);

        AVLoadingIndicatorView rightSpinner = findViewById(R.id.record_right_spinner);
        AVLoadingIndicatorView leftSpinner = findViewById(R.id.record_left_spinner);

        //check storage permission
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_STORAGE_PERMISSION);

        if (task == null) {
            task = new DownloadRecordTask(this);
        }

        File directory = new File(RECORDS_LOCATION + File.separator + phone);
        directory.mkdirs();

        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.conversation_actionbar);
        View view = actionBar.getCustomView();

        TextView header = view.findViewById(R.id.header_conversation);
        isGroup = false;
        try {
            isGroup = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(phone, false);
        } catch (ClassCastException e) {
            Log.d("T2T", "User " + phone + " is not a group");
        }
        header.setText(name);

        ImageButton backButton = view.findViewById(R.id.backIcon);
        backButton.setOnClickListener(v -> finish());

        //group communication channel info button
        infoButton = view.findViewById(R.id.infoButton);
        infoButton.setOnClickListener(v -> {
            Intent intentToGroupInfo = new Intent(this, GroupEditActivity.class);
            intentToGroupInfo.putExtra("phone", phone);
            startActivity(intentToGroupInfo);
        });
        if (isGroup) {
            infoButton.setVisibility(View.VISIBLE);
        } else {
            infoButton.setVisibility(View.INVISIBLE);
        }

        recordBtn.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
                uploadRecord(); //upload record to cloud
                updateUi();
                rightSpinner.smoothToHide();
                leftSpinner.smoothToHide();
                isRecording = false;
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Press longer to record", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });


        recordBtn.setOnLongClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        RECORD_AUDIO_AND_STORAGE_PERMISSION);
            }
            else
            {
                mediaPlayer = MediaPlayer.create(this, R.raw.record_press);
                mediaPlayer.start();
                currentRecord = UUID.randomUUID() + "_" + myName + "_" + myPhone;
                while (mediaPlayer.isPlaying()){ Log.d("T2T", "Waiting for media player to finish playing");}
                startRecording();
                isRecording = true;
                rightSpinner.smoothToShow();
                leftSpinner.smoothToShow();
            }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //register for callback
        registerForCallbacks();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            showRecords();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (task != null) {
            task.setCallbacks(null);//unregister
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        registerForCallbacks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED && recordsListAdapter==null) {
            showRecords();
        }
        //register for update ui callback
        registerForCallbacks();
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
     * A method that uploads new record to firebase cloud storage
     */
    private void uploadRecord() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        final StorageReference filePath = storageReference.child("records").child(phone).child(currentRecord);
        filePath.putFile(Uri.fromFile(new File(generateRecordPath()))).addOnSuccessListener(taskSnapshot -> {
            Toast toast = Toast.makeText(getApplicationContext(), "Message sent", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        });
    }

    /**
     * A method generates record file path in user's phone file system
     * @return full record path as string
     */
    private String generateRecordPath() {
        return RECORDS_LOCATION + File.separator + phone + File.separator + currentRecord + ".3gp";
    }

    /**
     * A method loads the last record from file system into records list adapter
     */
    private void getLastRecordAndUpdateUi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Path location = Paths.get(RECORDS_LOCATION + File.separator + phone);
            Optional<File> mostRecentFile =
                    Arrays
                            .stream(location.toFile().listFiles())
                            .filter(File::isFile)
                            .filter(file -> file.getName().matches(REGEX))
                            .max(Comparator.comparingLong(File::lastModified));
            mostRecentFile.ifPresent(file -> recordsListAdapter.add(new Record(location.toString(), phone, file)));
            recordsListAdapter.notifyDataSetChanged();
        } else {
            showRecords();
        }
        scrollMyListViewToBottom();
    }

    /**
     * A method that loads all records of conversation from file system,
     * creates new list adapter and updates ui
     */
     void showRecords() {
        List<Record> records = new ArrayList<>();
        String location = RECORDS_LOCATION + File.separator + phone;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try (Stream<Path> paths = Files.walk(Paths.get(location))) {
                paths
                        .filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().matches(REGEX))
                        .forEach(file -> records.add(new Record(location, phone, file.toFile())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            File directory = new File(location);
            File[] files = directory.listFiles();
            for (File file : files) {
                records.add(new Record(location, phone, file));
            }
        }
        adapter = new RecordsListAdapter(this, R.layout.records_list_item, records, myPhone, phone);
        if(recordsListAdapter == null)
        {
            recordsListAdapter = adapter;
            recordList.setAdapter(recordsListAdapter);
            scrollMyListViewToBottom();
        }
        else if(adapter.getCount() != recordsListAdapter.getCount())
        {
            recordsListAdapter = adapter;
            recordList.setAdapter(recordsListAdapter);
            scrollMyListViewToBottom();
        }
    }

    /**
     * A method that scrolls all the way down,
     * in order to show the last updated record to the user
     */
    private void scrollMyListViewToBottom() {
        recordList.post(() -> {
            // Select the last row so it will scroll into view...
            recordList.setSelection(recordsListAdapter.getCount() - 1);
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
    }

    /**
     * A callback method that called when data changed and ui update is necessary
     */
    @Override
    public void updateUi() {
        getLastRecordAndUpdateUi();
    }

    /**
     * A method that registers for ui updates callback
     */
    private void registerForCallbacks() {
        if (task != null) {
            task.setCallbacks(ConversationActivity.this);
        }
    }

    /**
     * A callback method that called when network connectivity changed
     * @param isConnected - network connectivity status
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        FrameLayout recordFrame = findViewById(R.id.record_frame);
        ConnectivityReceiver.showSnack(findViewById(android.R.id.content), isConnected);
        if (isConnected && recordFrame.getVisibility() == View.INVISIBLE) {
            showRecords();
            recordFrame.setVisibility(View.VISIBLE);
            if(isGroup)
                infoButton.setVisibility(View.VISIBLE);
        }
        else if(!isConnected && recordFrame.getVisibility() == View.VISIBLE)
        {
            recordFrame.setVisibility(View.INVISIBLE);
            infoButton.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * A method prepares and start audio recorder
     */
    private void startRecording() {
        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setOutputFile(generateRecordPath());
        myAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            myAudioRecorder.prepare();
        } catch (IOException e) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_STORAGE_PERMISSION);
        }

        try
        {
            myAudioRecorder.start();
        }catch (IllegalStateException e)
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Can't record voice message, please try again", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    /**
     * A method releases and stop audio recorder
     */
    private void stopRecording() {
        if(isRecording)
        {
            myAudioRecorder.stop();
            myAudioRecorder.release();
        }
        myAudioRecorder = null;
    }
}
