package edu.vt.recsports.fuse;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

/**
 * Represents the data contained within a FuseActivity object.
 */
public class FuseActivityData {
    public interface DownloadListener {
        public void thumbnailDownloadComplete(final Bitmap b);
    }

    private String patronInfo;
    private String activityId;
    private String trainerInfo;
    private String activityType;
    private String activityDate;
    private byte[] thumbnail;
    private String thumbnailURL;
    private Bitmap b;
    private boolean downloaded = false;
    private ArrayList<DownloadListener> listeners;
    private boolean downloadInProgress = false;

    public FuseActivityData() {
        listeners = new ArrayList<DownloadListener>();
        activityId = "";
        patronInfo = "";
        trainerInfo = "";
        activityType = "";
        activityDate = "---";
        thumbnailURL = "";
    }

    /**
     * Add a listener for when the thumbnail download is complete.
     * @param listener Listener to add.
     */
    public void addListener(DownloadListener listener) {
        listeners.add(listener);
    }

    /**
     * Notify all listeners that the thumbnail download is complete.
     * @param bmp The downloaded thumbnail.
     */
    public void notifyListeners(Bitmap bmp) {
        for (DownloadListener listener : listeners) {
            if (listener != null) {
                listener.thumbnailDownloadComplete(bmp);
            }
        }
    }

    /**
     * Download the thumbnail for the activity in an ASyncTask.
     * @param sessionId Current user's session cookie.
     */
    public void downloadThumb(final String sessionId) {
        if (!downloaded && thumbnailURL != null && !downloadInProgress) {
            AsyncTask<Void, Void, Void> dl = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    downloadInProgress = true;
                    try {
                        // Using an HttpURLConnection works better with session cookies.
                        URL url = new URL(thumbnailURL);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.addRequestProperty("Cookie", "connect.sid=" + sessionId);
                        // We need to pass in our specific session cookie, otherwise the API will not allow
                        // any queries. This is for security so users can only retrieve only their own
                        // FuseActivity data and no one else's.
                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            // If the response is OK, read and parse the thumbnail.
                            InputStream resultStream = connection.getInputStream();
                            BufferedInputStream reader = new BufferedInputStream(resultStream);
                            ByteArrayBuffer byteBuffer = new ByteArrayBuffer(50);
                            int current;
                            // Read all bytes from the response.
                            while ((current = reader.read()) != -1) {
                                byteBuffer.append((byte) current);
                            }
                            byte[] buffer = byteBuffer.toByteArray();
                            resultStream.close();
                            // Encode the byte data to a Base64 string to store in the DB.
                            thumbnail = buffer;
                            b = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
                            downloaded = true;
                            // Notify any listeners that the download is done.
                            notifyListeners(b);
                        }
                    }
                    catch (Exception e) {
                        b = null;
                        downloaded = false;
                        Log.e("ActivityData", "Error downloading thumb from " + thumbnailURL, e);
                    }
                    downloadInProgress = false;
                    return null;
                }
            };
            dl.execute();
        }
        else if (downloaded) {
            notifyListeners(b);
        }
    }

    /**
     * Set the patron ID for the activity.
     */
    public void setId(String id) {
        activityId = id;
    }

    /**
     * Set the thumbnail URL for the activity.
     */
    public void setThumbnailURL(String url) {
        thumbnailURL = url;
    }

    /**
     * Set the Base64 thumbnail value.
     */
    public void setThumbnail(byte[] thumb) {
        thumbnail = thumb;
    }

    /**
     * Set the patron ID.
     */
    public void setPatron(String patron) {
        patronInfo = patron;
    }

    /**
     * Set the trainer name.
     */
    public void setTrainer(String trainer) {
        trainerInfo = trainer;
    }

    /**
     * Set the activity type.
     */
    public void setActivityType(String type) {
        activityType = type;
    }

    /**
     * Set the activity date.
     * Attempt to parse it from the API format, otherwise just set it to the passed in value.
     */
    public void setActivityDate(String date) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
            input.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat target = new SimpleDateFormat("ccc, MMM dd, yyyy 'at' KK:mm a");
            target.setTimeZone(TimeZone.getDefault());
            activityDate = target.format(input.parse(date));
        }
        catch (Exception e) {
            activityDate = date;
        }
    }

    /**
     * Returns the Base64 thumbnail string.
     */
    public byte[] getThumbnail() {
        return thumbnail;
    }

    /**
     * Returns the thumbnail URL.
     */
    public String getThumbnailURL() {
        return thumbnailURL;
    }

    /**
     * Returns the activity ID.
     */
    public String getId() {
        return activityId;
    }

    /**
     * Returns the patron ID.
     */
    public String getPatron() {
        return patronInfo;
    }

    /**
     * Returns the activity date.
     */
    public String getActivityDate() {
        return activityDate;
    }

    /**
     * Returns the trainer name.
     */
    public String getTrainerName() {
        return trainerInfo;
    }

    /**
     * Returns the activity type.
     */
    public String getActivityType() {
        return activityType;
    }

    /**
     * Returns the activity notes.
     */
    public String getActivityNotes() {
        return "Overall your workout was good. Make sure you have better balance through your heels next time.";
    }

    public String getActivityVideoURL() {
        return "https://ia600401.us.archive.org/19/items/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";
    }

    /**
     * Returns a string of the activity.
     */
    @Override
    public String toString() {
        boolean showDate = false;
        boolean showType = false;
        if (activityDate != null && !activityDate.isEmpty()) {
            showDate = true;
        }
        if (activityType != null & !activityType.isEmpty()) {
            showType = true;
        }
        if (showDate && showType) {
            return activityType + " on " + activityDate;
        }
        else if (showDate) {
            return activityDate;
        }
        else if (showType) {
            return activityType;
        }
        else {
            return "-- no details --";
        }
    }

    /**
     * Returns a string for use in the activity preview fragment.
     */
    public String getTitle() {
        return activityType + " with " + trainerInfo + " on " + activityDate;
    }

    /**
     * Creates a FuseActivity object from a database cursor.
     */
    public static FuseActivityData fromCursor(Cursor cursor) {
        FuseActivityData act = new FuseActivityData();
        act.setId(cursor.getString(0));
        act.setPatron(cursor.getString(1));
        act.setTrainer(cursor.getString(2));
        act.setActivityDate(cursor.getString(3));
        act.setActivityType(cursor.getString(4));
        act.setThumbnail(cursor.getBlob(5));
        act.setThumbnailURL(cursor.getString(6));
        return act;
    }
}
