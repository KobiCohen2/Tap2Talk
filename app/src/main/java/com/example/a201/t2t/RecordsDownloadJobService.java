package com.example.a201.t2t;

import android.os.Bundle;
import android.util.Log;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

/**
 * A class represent service execute an async task for download new incoming record
 */
public class RecordsDownloadJobService extends JobService {
    public final static String EXTRA_RECORD_URL = "recordUrl";
    public final static String EXTRA_RECORD_FROM_PHONE = "recordFromPhone";
    public final static String EXTRA_RECORD_FROM_NAME = "recordFromName";
    static final String LOG_TAG = "T2T_REC_DOWNLOAD_SERVICE";
    private String recordUrlString;
    private String fromPhone;
    private String fromName;

    /**
     * A callback method, called when service starts
     * @param job - the job parameters
     * @return boolean value
     */
    @Override
    public boolean onStartJob(JobParameters job) {
        Bundle extras = job.getExtras();
        if (extras != null && extras.size() > 0) {
            recordUrlString = extras.getString(EXTRA_RECORD_URL);
            fromPhone = extras.getString(EXTRA_RECORD_FROM_PHONE);
            fromName = extras.getString(EXTRA_RECORD_FROM_NAME);
        } else {
            Log.e(LOG_TAG, "Intent extras is empty");
        }

        if (ConnectivityReceiver.isNetworkConnected()) {

            DownloadRecordTask task = new DownloadRecordTask(this);
            task.execute(recordUrlString, fromName, fromPhone);
        }
        return false;
    }

    /**
     * A callback method, called when service stops
     * @param job - the job parameters
     * @return boolean value
     */
    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }
}
