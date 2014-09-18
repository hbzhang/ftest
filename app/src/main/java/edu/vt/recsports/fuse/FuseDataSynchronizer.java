package edu.vt.recsports.fuse;

import android.os.Handler;
import android.os.Looper;
import android.os.AsyncTask;
import android.os.Binder;
import android.content.Intent;
import android.os.IBinder;
import android.app.Service;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Data synchronizer service. We run this as a service so it can theoretically be running even when
 * the application isn't running.
 */
public class FuseDataSynchronizer extends Service
{
    public static final int FUSE_SERVICE_SYNC_ALL = 1;
    public static final int FUSE_SERVICE_SYNC_ONE = 0;
    private final IBinder mBinder = new DataBinder();
    private SQLiteDatabase localDb;
    private FuseDataStoreHelper dbHelper;
    private SyncActivityListTask task;
    private Handler dataHandler;
    private String sidCookie;
    private boolean listTaskRunning = false;
    private ArrayList<FuseDataSyncListener> listeners;

    public class DataBinder extends Binder {
        FuseDataSynchronizer getService() {
            return FuseDataSynchronizer.this;
        }
    }

    /**
     * Private helper task to query the entire activity list for a user and then trigger subsequent
     * tasks to sync each individual activity separately.
     */
    private class SyncActivityListTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPostExecute(Void aVoid) {
            listTaskRunning = false;
        }

        @Override
        protected void onCancelled() {
            listTaskRunning = false;
        }

        @Override
        protected void onCancelled(Void aVoid) {
            listTaskRunning = false;
        }

        @Override
        protected void onPreExecute() {
            listTaskRunning = true;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            // Get a list of the activities from remote server.
            // For each one, post a new Runnable to a looper/handler.
            try {
                URL url = new URL(getResources().getString(R.string.fuse_base_url) + getResources().getString(R.string.fuse_activity_list_path));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                // We need to pass in our specific session cookie, otherwise the API will not allow
                // any queries. This is for security so users can only retrieve a list of their own
                // FuseActivity data and no one else's.
                connection.addRequestProperty("Cookie", getResources().getString(R.string.cookie_name) + "=" + sidCookie);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // Read the entire response into a string, since it's JSON.
                    InputStream resultStream = connection.getInputStream();
                    BufferedReader read = new BufferedReader(new InputStreamReader(resultStream));
                    String result = "";
                    for (String line; (line = read.readLine()) != null;) {
                        result += line;
                    }
                    // Set up our JSON result.
                    final JSONArray jsonResult = new JSONArray(result);
                    for (int i = 0; i < jsonResult.length(); i++) {
                        // For each resulting FuseActivity, post a new runnable to the handler
                        // that executes a single task to synchronize that specific FuseActivity.
                        final String fuseActivityId = jsonResult.getString(i);
                        dataHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                SyncActivityTask t = new SyncActivityTask();
                                t.execute(fuseActivityId);
                            }
                        });
                    }
                }
                else {
                    Log.i("Service", "Returned: " + connection.getResponseCode());
                }
            }
            catch (Exception e) {
                // There was some error, log it for later debugging.
                Log.e("SyncActivityListTask", "Error during task", e);
            }

            return null;
        }
    }

    /**
     * Private helper task to query an individual activity to then write to the database.
     */
    private class SyncActivityTask extends AsyncTask<String, Void, FuseActivityData> {
        @Override
        protected void onPostExecute(FuseActivityData fuseActivityData) {
            if (fuseActivityData != null) {
                if (dbHelper.upsertActivity(fuseActivityData, localDb)) {
                    notifyListeners();
                }
            }
        }

        @Override
        protected FuseActivityData doInBackground(String... params) {
            FuseActivityData fuseActivity = null;
            String activityId = params[0];
            if (activityId != null && !activityId.isEmpty()) {
                try {
                    URL url = new URL(getResources().getString(R.string.fuse_base_url) + getResources().getString(R.string.fuse_activity_detail_path) + activityId);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    // We need to pass in our specific session cookie, otherwise the API will not allow
                    // any queries. This is for security so users can only retrieve only their own
                    // FuseActivity data and no one else's.
                    connection.addRequestProperty("Cookie", getResources().getString(R.string.cookie_name) + "=" + sidCookie);
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        // Read the response into a string since it's JSON.
                        InputStream resultStream = connection.getInputStream();
                        BufferedReader read = new BufferedReader(new InputStreamReader(resultStream));
                        String result = "";
                        for (String line; (line = read.readLine()) != null;) {
                            result += line;
                        }
                        // Set up our JSON result.
                        final JSONArray jsonResult = new JSONArray(result);
                        // Create a new activity and set values from the JSON result.
                        fuseActivity = new FuseActivityData();
                        fuseActivity.setId(activityId);
                        fuseActivity.setActivityDate(jsonResult.getJSONObject(0).getString("Date"));
                        fuseActivity.setActivityType(jsonResult.getJSONObject(0).getString("ActivityType"));
                        fuseActivity.setTrainer(jsonResult.getJSONObject(0).getJSONObject("TrainerId").getString("name"));
                        fuseActivity.setPatron(jsonResult.getJSONObject(0).getJSONObject("PatronId").getString("_id"));
                        fuseActivity.setThumbnailURL(null);
                        if (jsonResult.getJSONObject(0).getJSONArray("KinectFrames").length() > 0) {
                            // A thumbnail is available, so store the URL for downloading later.
                            String baseUrl = getResources().getString(R.string.fuse_base_url);
                            fuseActivity.setThumbnailURL(baseUrl + jsonResult.getJSONObject(0).getJSONArray("KinectFrames").getString(0));
                        }
                    }
                }
                catch (Exception e) {
                    fuseActivity = null;
                    Log.e("SyncActivityTask", "Error during task", e);
                }
            }
            return fuseActivity;
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        sidCookie = null;
        dbHelper = new FuseDataStoreHelper(this);
        localDb = dbHelper.getWritableDatabase();
        // We use a looper to post individual activity tasks to.
        dataHandler = new Handler(Looper.myLooper());
        // Make sure our cookies get saved between subsequent URLConnections.
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        listeners = new ArrayList<FuseDataSyncListener>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Notify any listeners that new data is ready.
     */
    public void notifyListeners() {
        for (FuseDataSyncListener listener : listeners) {
            listener.dataReady();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Delete all data from the database and resync for the current user.
     */
    public void clearAndResync() {
        localDb.delete("FuseActivity", null, null);
        requestSync(FuseDataSynchronizer.FUSE_SERVICE_SYNC_ALL, null);
    }

    /**
     * Request a sync task to occur.
     */
    public void requestSync(int sync, final String fuseActivityId) {
        if (sidCookie != null && !sidCookie.isEmpty()) {
            // Only sync if a user is logged in.
            switch (sync) {
                case FUSE_SERVICE_SYNC_ALL:
                    // User requested to refresh all FuseActivity data from remote server.
                    if (!listTaskRunning) {
                        task = new SyncActivityListTask();
                        task.execute();
                    }
                    break;
                case FUSE_SERVICE_SYNC_ONE:
                    // We need to figure out which specific FuseActivity was requested to resync
                    // via extra data in the Intent.
                    if (fuseActivityId != null && !fuseActivityId.isEmpty()) {
                        // The FuseActivityId seems valid, so attempt to synchronize it by posting
                        // a task to the looper.
                        dataHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                SyncActivityTask t = new SyncActivityTask();
                                t.execute(fuseActivityId);
                            }
                        });
                    }
                    break;
            }
        }
    }

    /**
     * Set the currently logged in user cookie.
     * @param userCookie
     */
    public void setUser(String userCookie) {
        sidCookie = userCookie;
    }

    /**
     * Add a data ready listener.
     */
    public void addListener(FuseDataSyncListener listener) {
        listeners.add(listener);
    }
}
