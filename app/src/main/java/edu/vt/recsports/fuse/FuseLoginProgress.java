package edu.vt.recsports.fuse;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Fragment that handles the actual login task.
 */
public class FuseLoginProgress extends Fragment {

    private FusePatron loggedInUser;
    private FuseCallbacks listener;
    private FuseLoginParams loginValues;
    private LoginTask task;
    private String loginPath;
    private String cookieName;

    private boolean isRunning = false;

    private class LoginTask extends AsyncTask<FuseLoginParams, Integer, FusePatron> {
        /**
         * Attempts to login via a remote REST api.
         * @param params FuseLoginParams of the person attempting to login.
         * @return A patron if successful, null otherwise.
         */
        @Override
        protected FusePatron doInBackground(FuseLoginParams... params) {
            FusePatron p = null;
            if (params[0].isValid()) {
                // Email and password combo are valid.
                String email = params[0].getEmail();
                String passHash = params[0].getPassword();
                String result = "";
                try {
                    // Sleep for 3 seconds to allow the progress spinner to look useful.
                    Thread.sleep(3000);
                    // Open a remote connection to the REST api and attempt a login.
                    URL url = new URL(loginPath);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    // Generate a random string that we store later if they log in successfully.
                    connection.addRequestProperty("Cookie", cookieName + "=whatever");
                    // We need to post values, so we have to set these.
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setChunkedStreamingMode(0);
                    OutputStream outStream = connection.getOutputStream();
                    // "Post" our email/pass to the request by writing to the stream.
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
                    writer.write("email=" + email + "&password=" + passHash);
                    writer.flush();
                    writer.close();
                    outStream.close();
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        // Get the actual session ID from the response so we can use it later.
                        String sid = getSessionCookie(connection.getHeaderFields());
                        // Read the response as a string since it's JSON.
                        InputStream resultStream = connection.getInputStream();
                        BufferedReader read = new BufferedReader(new InputStreamReader(resultStream));
                        for (String line; (line = read.readLine()) != null;) {
                            result += line;
                        }
                        JSONObject loginResult = new JSONObject(result);
                        // Login was successful, store the FusePatron information.
                        p = new FusePatron(email, passHash);
                        p.setLoggedIn(true);
                        p.setLastName(loginResult.getJSONObject("user").getString("name"));
                        p.setPatronId(loginResult.getJSONObject("user").getString("_id"));
                        p.setSessionId(sid);
                    }
                    else {
                        // The given email/password combo couldn't log in.
                        publishProgress(-3);
                    }
                }
                catch (Exception e) {
                    // There was an error attempting to log in, not necessarily a wrong email/pass.
                    Log.e("Login", "Error logging in", e);
                    p = null;
                    publishProgress(-2);
                }
            }
            else {
                // The email or password was not provided.
                publishProgress(-1);
            }
            return p;
        }

        /**
         * Alert a listener that the task has posted some progress.
         * @param values
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (listener != null) {
                listener.handleProgress(values[0]);
            }
        }

        /**
         * Handle task setup prior to running.
         */
        @Override
        protected void onPreExecute() {
            isRunning = true;
        }

        /**
         * Handle the login task finishing.
         * @param loggedInPatron The resulting logged in patron, or null if the process was
         *                       unsuccessful.
         */
        @Override
        protected void onPostExecute(FusePatron loggedInPatron) {
            loggedInUser = loggedInPatron;
            isRunning = false;
            if (loggedInUser != null) {
                // If the returned result exists.
                if (listener != null) {
                    // Notify the listener that a user logged in.
                    listener.loggedIn(loggedInUser);
                }
            }
        }
    }

    /**
     * Begin a login task, if one isn't already running.
     * @param p The login values to use.
     */
    public void login(FuseLoginParams p) {
        if (!isRunning) {
            loginValues = p;
            task = new LoginTask();
            task.execute(loginValues);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_progress,
                container, false);

        setRetainInstance(true);

        loginPath = getResources().getString(R.string.fuse_base_url) + getResources().getString(R.string.fuse_login_path);
        cookieName = getResources().getString(R.string.cookie_name);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (FuseCallbacks) activity;
        if (loggedInUser != null && listener != null) {
            listener.loggedIn(loggedInUser);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    /**
     * Returns the session ID from the given Map of headers from our login response.
     */
    private String getSessionCookie(Map<String, List<String>> mp) {
        String sessionId = "";
        for (String k : mp.keySet()) {
            if (k != null && k.contains("set-cookie")) {
                for (String v : mp.get(k)) {
                    if (v.startsWith("connect.sid")) {
                        sessionId = v.substring(v.indexOf("=")+1);
                    }
                }
            }
        }
        return sessionId;
    }
}
