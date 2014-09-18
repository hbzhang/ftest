package edu.vt.recsports.fuse;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class FuseActivityPreviewFragment extends Fragment implements View.OnClickListener, FuseActivityData.DownloadListener {
    public interface PreviewListener {
        public void activityClick(FuseActivityData activity);
        public void postToUiThread(Runnable r);
    }

    private ImageView thumbnail;
    private FuseActivityData currentActivity;
    private PreviewListener listener;
    private FuseDataStoreHelper helper;
    private FusePatron currentPatron;
    private TextView latestTitle;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (PreviewListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fuse_activity_preview,
                container, false);

        setRetainInstance(true);

        thumbnail = (ImageView) view.findViewById(R.id.fuseActivityThumb);
        thumbnail.setOnClickListener(this);

        latestTitle = (TextView) view.findViewById(R.id.fuseActivityInfo);

        helper = new FuseDataStoreHelper(inflater.getContext());

        return view;
    }

    /**
     * Set the currently logged in user.
     */
    public void setPatron(FusePatron p, Context c) {
        currentPatron = p;
        if (helper == null) {
            // Update the preview to show the latest activity for this user.
            helper = new FuseDataStoreHelper(c);
            SQLiteDatabase db = helper.getReadableDatabase();
            setFuseActivity(helper.getMostRecentActivity(currentPatron.getPatronId(), db));
            db.close();
        }
    }

    /**
     * Set the current activity for previewing.
     */
    public void setFuseActivity(FuseActivityData act) {
        currentActivity = act;
        if (currentActivity != null) {
            // Add this fragment as a listener and start downloading the thumbnail.
            currentActivity.addListener(this);
            currentActivity.downloadThumb(currentPatron.getSessionId());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    /**
     * The preview thumbnail for latest activity was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fuseActivityThumb && listener != null && currentActivity != null) {
            // Alert the listener that a click happened.
            listener.activityClick(currentActivity);
        }
    }

    /**
     * Callback for when a thumbnail is done downloading.
     */
    @Override
    public void thumbnailDownloadComplete(final Bitmap b) {
        if (listener != null) {
            listener.postToUiThread(new Runnable() {
                @Override
                public void run() {
                    // Run the UI changes in the UI thread.
                    thumbnail.setImageBitmap(b);
                    latestTitle.setText(currentActivity.getTitle());
                }
            });
        }
        else {
            thumbnail.setImageBitmap(b);
        }
    }

    /**
     * Service callback for when new data is available.
     */
    public void dataReady() {
        // Get the latest activity from the database and show it.
        SQLiteDatabase db = helper.getReadableDatabase();
        setFuseActivity(helper.getMostRecentActivity(currentPatron.getPatronId(), db));
        db.close();
    }
}
