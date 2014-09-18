package edu.vt.recsports.fuse;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class FuseActivityDetailFragment extends Fragment implements FuseActivityData.DownloadListener, View.OnClickListener, MediaPlayer.OnCompletionListener {
    private ImageView thumbnail;
    private VideoView videoView;
    private MediaController player;
    private FuseActivityData activityData;
    private TextView trainerName;
    private TextView dateValue;
    private TextView activityType;
    private TextView activityNotes;
    private String currentSession;
    private FuseActivityPreviewFragment.PreviewListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (FuseActivityPreviewFragment.PreviewListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fuse_activity_detail,
                container, false);

        setRetainInstance(true);

        trainerName = (TextView) view.findViewById(R.id.trainerValue);
        dateValue = (TextView) view.findViewById(R.id.dateValue);
        activityType = (TextView) view.findViewById(R.id.workoutValue);
        activityNotes = (TextView) view.findViewById(R.id.trainerNotesValue);

        videoView = (VideoView) view.findViewById(R.id.fuseActivityVideo);
        player = new MediaController(getActivity().getApplicationContext());
        player.setAnchorView(videoView);
        videoView.setMediaController(player);

        thumbnail = (ImageView) view.findViewById(R.id.fuseActivityThumb);
        thumbnail.setImageResource(R.drawable.thumbnail);
        thumbnail.setOnClickListener(this);

        if (activityData != null) {
            // If there is activity data, show it. This is for orientation changes where the data
            // can get cleared.
            showActivityData();
        }

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    /**
     * Upon hiding, reset the thumbnail.
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden && thumbnail != null) {
            thumbnail.setImageResource(R.drawable.thumbnail);
        }
    }

    /**
     * Populate the textviews with the activity data.
     */
    public void showActivityData() {
        dateValue.setText(activityData.getActivityDate());
        trainerName.setText(activityData.getTrainerName());
        activityType.setText(activityData.getActivityType());
        activityNotes.setText(activityData.getActivityNotes());
        activityData.downloadThumb(currentSession);
    }

    /**
     * Set the current activity.
     */
    public void setFuseActivity(FuseActivityData data, String sessionId) {
        videoView.stopPlayback();
        playbackComplete();
        // Store the session so we can use it for downloading thumbnails later if need be.
        currentSession = sessionId;
        thumbnail.setImageResource(R.drawable.thumbnail);
        activityData = data;
        // Add a listener for the thumbnail download.
        data.addListener(this);
        showActivityData();
    }

    /**
     * Callback for when the thumbnail is done downloading.
     */
    @Override
    public void thumbnailDownloadComplete(final Bitmap b) {
        if (listener != null) {
            // UI updates need to run on the UI thread.
            listener.postToUiThread(new Runnable() {
                @Override
                public void run() {
                    thumbnail.setImageBitmap(b);
                }
            });
        }
        else {
            // Attempt to set the thumbnail if there is no listener.
            thumbnail.setImageBitmap(b);
        }
    }

    private void showAndPlayVideo() {
        String vidAddress = activityData.getActivityVideoURL();
        Uri vidUri = Uri.parse(vidAddress);
        videoView.setVideoURI(vidUri);
        thumbnail.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
        videoView.start();
        videoView.setOnCompletionListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fuseActivityThumb:
                // hide the thumbnail and show a video player
                showAndPlayVideo();
                break;
        }
    }

    private void playbackComplete() {
        player.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
        thumbnail.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playbackComplete();
    }
}
