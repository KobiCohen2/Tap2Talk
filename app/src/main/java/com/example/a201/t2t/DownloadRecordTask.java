package com.example.a201.t2t;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import static com.example.a201.t2t.RecordsDownloadJobService.LOG_TAG;

/**
 * A class represent async task for download record
 */
public class DownloadRecordTask extends AsyncTask<String, Void, String> {
    private String fromName;
    private String fromPhone;
    private static ServiceCallbacks serviceCallbacks;
    private final ThreadLocal<Context> context = new ThreadLocal<>();

    /**
     * Constructor
     * @param context - context that execute the task
     */
    DownloadRecordTask(Context context) {
        this.context.set(context);
    }

    /**
     * A method for set callback of ui update
     * @param callbacks - the callback function
     */
    void setCallbacks(ServiceCallbacks callbacks)
    {
        serviceCallbacks = callbacks;
    }

    @Override
    protected String doInBackground(String... params) {
        String recordUrlString = params[0];
        fromName = params[1];
        fromPhone = params[2];
        URL recordUrl = null;
        HttpURLConnection connection = null;
        try {
            recordUrl = new URL(recordUrlString);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Record url is malformed");
        }
        try {
            if (recordUrl != null) {
                connection = (HttpURLConnection) recordUrl.openConnection();
                //set the read time out (10s)
                connection.setReadTimeout(10000);
                //set the connect time out (15s)
                connection.setConnectTimeout(15000);
                // do a GET request (this is the default)
                connection.setRequestMethod("GET");
                // allow the download of information (input) (this is the default)
                connection.setDoInput(true);
                //start
                connection.connect();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Connection error");
        }
        File outFile = null;
        FileOutputStream fos = null;
        if (connection != null) {
            try (InputStream is = connection.getInputStream()) {
                //see what the response is
                int responseCode = connection.getResponseCode();
                //see if it's downloadable
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String downloadDir = ConversationActivity.RECORDS_LOCATION + File.separator + fromPhone;
                    // open an output file
                    outFile = new File(downloadDir, UUID.randomUUID() + "_" + fromName + "_" + fromPhone + ".3gp");
                    fos = new FileOutputStream(outFile);
                    byte[] buf = new byte[4096];
                    int count;
                    while ((count = is.read(buf)) > 0)
                        fos.write(buf, 0, count);
                }

            } catch (IOException e2) {
                e2.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (outFile != null) {
            return outFile.toString();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String result) {
        if (serviceCallbacks != null) {
            serviceCallbacks.updateUi();
        }
        new T2tMessagingService().sendNotification(fromName, fromPhone, context.get());
        if (result != null) {
            Log.d(LOG_TAG, "Record download async task finished");
        }
    }
}
