package com.example.a201.t2t;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import de.hdodenhof.circleimageview.CircleImageView;
import static com.example.a201.t2t.Constants.TAG;
import static com.example.a201.t2t.Constants.USER_THUMB_URL;

/**
 * A class represent list adapter to present list view of records
 */
public class RecordsListAdapter extends ArrayAdapter<Record> {

    private Context context;
    private int resource;
    private List<Record> records;
    private String myPhone;
    private String phone;
    private MediaPlayer mPlayer;
    private int length;
    private SeekBar mSeekBar;
    private Handler mHandler;
    private ImageView playImage;
    private ImageView pauseImage;
    private TextView runTimeText;
    private Runnable mRunnable;
    private final SimpleDateFormat format = new SimpleDateFormat("E dd/MM/yyyy HH:mm");

    /**
     * A class represent a view holder to increase adapter performance
     */
    static class ViewHolder {
        CircleImageView userImage;
        LinearLayout record_item;
        LinearLayout player;
        ImageView play;
        ImageView pause;
        SeekBar currentSeekBar;
        TextView recordDate;
        TextView totalTime;
        TextView runTime;
    }

    /**
     * A constructor
     * @param context  - context that filled the adapter
     * @param resource - resource of the adapter list item
     * @param records  - list of records to present
     * @param myPhone  - my phone number
     * @param phone    - user's phone number
     */
    RecordsListAdapter(Context context, int resource, List<Record> records, String myPhone, String phone) {
        super(context, resource, records);
        this.context = context;
        this.resource = resource;
        this.records = records;
        this.myPhone = myPhone;
        this.phone = phone;
        this.mHandler = new Handler();
    }

    /**
     * A method that prepare ui view in specific position
     * @param position    - view position
     * @param convertView - the convert view
     * @param parent      - the parent view
     * @return the convert view
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        ViewHolder holder;
        Record record = null;
        String recordProducer;
        boolean isMyRecord = false;

        if(position < records.size())
        {
            record = records.get(position);
            recordProducer = record.getRecord().getName().split("_")[2];//extract the phone number of the recorder
            isMyRecord = recordProducer.startsWith(myPhone);
        }

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(resource, parent, false);
            holder = new ViewHolder();
            holder.userImage = convertView.findViewById(R.id.recordUserImage);
            holder.record_item = convertView.findViewById(R.id.record_item);
            holder.player = convertView.findViewById(R.id.player);
            holder.play = convertView.findViewById(R.id.play);
            holder.pause = convertView.findViewById(R.id.pause);
            holder.currentSeekBar = convertView.findViewById(R.id.mediaPlayer_seekbar);
            holder.recordDate = convertView.findViewById(R.id.record_date);
            holder.totalTime = convertView.findViewById(R.id.total_time);
            holder.runTime = convertView.findViewById(R.id.run_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        try {
            Uri thumbUrl = isMyRecord ?
                    Uri.parse(MessageFormat.format(USER_THUMB_URL, myPhone)) :
                    Uri.parse(MessageFormat.format(USER_THUMB_URL, phone));

            //load thumbnail
            Picasso.get().load(thumbUrl).placeholder(R.drawable.default_image).into(holder.userImage);


            if (isMyRecord) {
                holder.player.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corners_layout_green));
                holder.record_item.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            } else {
                holder.player.setBackground(ContextCompat.getDrawable(context, R.drawable.round_corners_layout_white));
                holder.record_item.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            }

            //enable seek bar
            holder.currentSeekBar.setEnabled(true);
            //update date
            holder.recordDate.setText(format.format(record.getRecord().lastModified()));
            //update run time
            holder.runTime.setText("00:00");
            //update total time
            holder.totalTime.setText(getTotalTime(record.getRecord()));

            // Click listener for playing button
            Record finalRecord = record;
            holder.play.setOnClickListener(view -> {
                //check if media player already initialized (for pause)
                if (mPlayer != null && !mPlayer.isPlaying() && holder.play == playImage) {
                    togglePlayPauseButtons(holder.play, holder.pause, true);
                    mPlayer.seekTo(length);
                    mPlayer.start();
                    return;
                }

                //reset run time
                if (runTimeText != null) {
                    runTimeText.setText("00:00");
                }

                //stop current media player
                stopPlaying(holder.runTime);

                // Initialize media player
                mPlayer = MediaPlayer.create(context, Uri.fromFile(finalRecord.getRecord()));

                //get record total time
                String totalRecordTime = getTotalTime(finalRecord.getRecord());

                playImage = holder.play;
                pauseImage = holder.pause;
                runTimeText = holder.runTime;

                mPlayer.setOnCompletionListener(mp -> {
                    // If media player another instance already running then stop it first
                    holder.runTime.setText(totalRecordTime);
                    stopPlaying(holder.runTime);
                });

                // Initialize the seek bar
                int duration = mPlayer.getDuration();
                mSeekBar = holder.currentSeekBar;
                mSeekBar.setMax(duration / 100);
                changeSeekBar(holder.runTime);

                // Start the media player
                mPlayer.start();

                //update buttons
                togglePlayPauseButtons(holder.play, holder.pause, true);
            });

            holder.currentSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    if (mPlayer != null && b) {
                        mPlayer.seekTo(i * 100);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            holder.pause.setOnClickListener(v -> {
                togglePlayPauseButtons(holder.play, holder.pause, false);
                if (mPlayer != null && mPlayer.isPlaying()) {
                    mPlayer.pause();
                    length = mPlayer.getCurrentPosition();
                }
            });
        } catch (Exception e) {
            //remove the bad record from the list
            try {
                this.remove(record);
                ((ConversationActivity)context).adapter.records.remove(record);
                ((ConversationActivity)context).recordsListAdapter.records.remove(record);
            }catch (Exception ex){ /* do nothing */ }
            Log.e(TAG, "Error while loading record");
        }
        return convertView;
    }

    /**
     * A method stops the media player
     * @param runTime - the media player running time text view
     */
    private void stopPlaying(TextView runTime) {
        // If media player is not null then try to stop it
        if (mPlayer != null) {
            mSeekBar.setProgress(0);
            runTime.setText("00:00");
            togglePlayPauseButtons(playImage, pauseImage, false);//toggle buttons
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
            if (mHandler != null) {
                mHandler.removeCallbacks(mRunnable);
            }
        }
    }

    /**
     * A method that updates seek bar according to media player progress
     * @param runTime - the media player running time text view
     */
    private void changeSeekBar(TextView runTime) {
        ((ConversationActivity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mPlayer != null && mPlayer.isPlaying()) {
                    int currentPosition = mPlayer.getCurrentPosition();
                    mSeekBar.setProgress(currentPosition / 100);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(currentPosition);
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(currentPosition);
                    runTime.setText(convertTo2Digits(minutes) + ":" + convertTo2Digits(seconds));
                }
                mRunnable = this;
                mHandler.postDelayed(this, 100);
            }
        });
    }

    /**
     * A method that adds 0 to the left of the digits,
     * if the digits < 10
     * @param time - the requested number to check
     * @return - 2 digits number if necessary
     */
    private String convertTo2Digits(long time) {
        if (time < 10)
            return "0" + time;
        return String.valueOf(time);
    }

    /**
     * A method that calculates record total time
     * @param record - the record to calculate
     * @return - the total time as string
     */
    private String getTotalTime(File record) {
        int duration = MediaPlayer.create(context, Uri.fromFile(record)).getDuration();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        return convertTo2Digits(minutes) + ":" + convertTo2Digits(seconds);
    }

    /**
     * A method that toggles pause and play buttons
     * @param play   - play button image view
     * @param pause  - pause button image view
     * @param isPlay - mode
     */
    private void togglePlayPauseButtons(ImageView play, ImageView pause, boolean isPlay) {
        if (isPlay) {
            play.setVisibility(View.GONE);
            pause.setVisibility(View.VISIBLE);
        } else {
            play.setVisibility(View.VISIBLE);
            pause.setVisibility(View.GONE);
        }
    }
}
