package edu.vt.recsports.fuse;

import android.content.Intent;
import android.util.Log;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity implements FuseCallbacks {

	private final static String FRAG_TAG_LOGIN = "LOGIN";
	private final static String FRAG_TAG_LOGIN_STATUS = "LOGINSTATUS";

	private FuseLoginFragment loginFrag;
    private FuseLoginProgress loginProg;
    private boolean loggingIn = false;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Initialize all the fragments.
        loginProg = (FuseLoginProgress) getFragmentManager().findFragmentByTag(FRAG_TAG_LOGIN_STATUS);
        loginFrag = (FuseLoginFragment) getFragmentManager().findFragmentByTag(FRAG_TAG_LOGIN);
        if (loginProg == null) {
            loginProg = new FuseLoginProgress();
            getFragmentManager().beginTransaction().add(R.id.mainView, loginProg, FRAG_TAG_LOGIN_STATUS).hide(loginProg).commit();
        }
        if (loginFrag == null) {
            // The login fragment hasn't been created yet, so create one and add it.
            loginFrag = new FuseLoginFragment();
            getFragmentManager().beginTransaction().add(R.id.mainView, loginFrag, FRAG_TAG_LOGIN).commit();
        }

        // If the orientation changed while the user is logging in, show the login status fragment.
        if (savedInstanceState != null && savedInstanceState.getBoolean("loggingIn")) {
            showLoginProgressFragment();
        }
        else {
            showLoginFormFragment();
        }
	}

    /**
     * Click handler callback for fragments to notify this activity that one of their views was
     * clicked.
     * @param clickedViewId The ID of the view that was clicked.
     * @param values A string array of values that are utilized.
     */
	public void onClicked(int clickedViewId, String[] values) {
		switch (clickedViewId) {
			case R.id.loginButton:
                // Login was clicked, so attempt to log in the user.
				String email = values[0];
				String passHash = values[1];
                FuseLoginParams lp = new FuseLoginParams(email, passHash);
                if (lp.isValid() && !loggingIn) {
                    loggingIn = true;
                    showLoginProgressFragment();
                    loginProg.login(lp);
                }
				break;
		}
	}

    /**
     * Show the login fragment.
     */
    public void showLoginFormFragment() {
        FragmentTransaction tx = getFragmentManager().beginTransaction();
        tx.hide(loginProg).show(loginFrag).commit();
        getFragmentManager().executePendingTransactions();
    }

    /**
     * There was an error logging in, so switch back to the login form fragment.
     */
    public void loginError() {
        FragmentTransaction tx = getFragmentManager().beginTransaction();
        tx.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        tx.show(loginFrag).hide(loginProg).commit();
        loggingIn = false;
    }

    /**
     * Replace the login form fragment with the login progress fragment when the user clicks
     * the login button.
     */
    public void showLoginProgressFragment() {
        FragmentTransaction tx = getFragmentManager().beginTransaction();
        tx.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        tx.hide(loginFrag).show(loginProg).commit();
    }

    /**
     * Interface callback when a user successfully logs in.
     * @param p The logged in user.
     */
    public void loggedIn(FusePatron p) {
        loggingIn = false;
        // Launch overview activity with given patron.
        Intent overviewActivity = new Intent(this, FuseActivityOverviewActivity.class);
        Bundle launchData = new Bundle();
        // Put the logged in patron into the bundle extras of the intent.
        launchData.putParcelable("patron", p);
        overviewActivity.putExtras(launchData);
        startActivityForResult(overviewActivity, 83);
    }

    /**
     * Interface callback to handle any errors that occur during login attempts.
     * @param progress The progress code. Negative values are errors.
     */
    public void handleProgress(int progress) {
        if (progress < 0) {
            loginError();
        }
        switch (progress) {
            case -1:
                Log.i("Main", "Email or pass not entered");
                // Email or password wasn't entered.
                break;
            case -2:
                Log.i("Main", "Exception thrown trying to log in");
                // Exception thrown trying to log in.
                Toast.makeText(this, R.string.error_login, Toast.LENGTH_SHORT).show();
                break;
            case -3:
                Log.i("Main", "Email or pass wrong, couldn't log in");
                // Email or password was wrong so couldn't successfully log in.
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 83 && resultCode == RESULT_OK) {
            recreate();
            showLoginFormFragment();
        }
    }
}
