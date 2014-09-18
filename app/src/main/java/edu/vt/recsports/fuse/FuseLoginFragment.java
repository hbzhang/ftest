/**
 * The login screen fragment.
 *
 * @author Joseph Pontani <jpontani@vt.edu>
 * @date 23 July 2014
 */

package edu.vt.recsports.fuse;

import java.security.MessageDigest;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class FuseLoginFragment extends Fragment implements OnClickListener {

	private EditText emailAddressValue;
	private EditText passwordValue;
	private Button loginButton;
	private FuseCallbacks listener;
    private boolean isRunning = false;

    /**
     * Handler for when attaching to an activity.
     * @param activity
     */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		listener = (FuseCallbacks) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_login,
				container, false);

        setRetainInstance(true);

		loginButton = (Button) view.findViewById(R.id.loginButton);
		loginButton.setOnClickListener(this);

        emailAddressValue = (EditText) view.findViewById(R.id.emailAddressValue);
        passwordValue = (EditText) view.findViewById(R.id.passwordValue);

		return view;
	}

    /**
     * Handler when detaching from an activity.
     */
	@Override
	public void onDetach() {
		super.onDetach();
		listener = null;
	}

    /**
     * Onclick handler for the login button.
     * @param v View that was clicked.
     */
    public void onClick(View v) {
        emailAddressValue.setBackground(null);
        passwordValue.setBackground(null);
        try {
            // Get the email and password values and pass them on to the listener.
            String password = passwordValue.getText().toString();
            if (password.isEmpty()) {
                // Password is empty so put a red border around it.
                passwordValue.setBackgroundResource(R.drawable.redborder);
            }
            String email = emailAddressValue.getText().toString();
            if (email.isEmpty()) {
                // Email is empty so put a red border around it.
                emailAddressValue.setBackgroundResource(R.drawable.redborder);
            }
            FuseLoginParams p = new FuseLoginParams(email, password);
            listener.onClicked(v.getId(), p.getValues());
        }
        catch (Exception e) {
            Log.e("Login", "Error with login form", e);
        }
    }

    /**
     * Generate an MD5 hash of a password.
     * @param password Plaintext password to hash.
     * @return Hashed password, or null if an error occurred.
     */
	public static String generatePasswordHash(String password) {
		String hash = null;
		if (password != null && password.length() > 0) {
            // Only generate a hash if a password has been specified.
			try {
                // Convert the password to bytes.
				byte[] buffer = password.getBytes("UTF-8");
                // MD5 hash the bytes.
				buffer = MessageDigest.getInstance("MD5").digest(buffer);
				StringBuilder sb = new StringBuilder(buffer.length);
                // Build a string from the hashed bytes.
				for (int i=0; i< buffer.length; i++) {
					sb.append(String.format("%02X", buffer[i] & 0xff));
				}
				hash = sb.toString();
			}
			catch (Exception e) {
				hash = null;
			}
		}
		return hash;
	}
}
