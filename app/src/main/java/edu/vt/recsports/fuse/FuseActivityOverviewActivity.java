package edu.vt.recsports.fuse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class FuseActivityOverviewActivity extends Activity implements FuseActivityPreviewFragment.PreviewListener, FuseDataSyncListener {

    private final static String FRAG_TAG_ACTIVITY_LIST = "ACTIVITY_LIST";
    private final static String FRAG_TAG_ACTIVITY_PREVIEW = "ACTIVITY_PREVIEW";
    private final static String FRAG_TAG_ACTIVITY_DETAIL = "ACTIVITY";

    private FuseActivityDetailFragment detailFrag;
    private FuseActivityListFragment listFrag;
    private FuseActivityPreviewFragment previewFrag;
    private FusePatron currentPatron;

    private boolean serviceBound = false;
    private FuseDataSynchronizer dataSyncService;
    private ServiceConnection dataConnection = new ServiceConnection() {
        /**
         * Callback for when the service is bound.
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FuseDataSynchronizer.DataBinder binder = (FuseDataSynchronizer.DataBinder) service;
            dataSyncService = binder.getService();
            // Set the current patron for the service, and request to sync all their data.
            dataSyncService.setUser(currentPatron.getSessionId());
            dataSyncService.requestSync(FuseDataSynchronizer.FUSE_SERVICE_SYNC_ALL, null);
            dataSyncService.addListener(FuseActivityOverviewActivity.this);
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fuse_overview_activity);

        // Initialize and add our fragments.
        previewFrag = (FuseActivityPreviewFragment) getFragmentManager().findFragmentByTag(FRAG_TAG_ACTIVITY_PREVIEW);
        if (previewFrag == null) {
            previewFrag = new FuseActivityPreviewFragment();
            getFragmentManager().beginTransaction().add(R.id.preview, previewFrag, FRAG_TAG_ACTIVITY_PREVIEW).commit();
        }
        listFrag = (FuseActivityListFragment) getFragmentManager().findFragmentByTag(FRAG_TAG_ACTIVITY_LIST);
        if (listFrag == null) {
            listFrag = new FuseActivityListFragment();
            getFragmentManager().beginTransaction().add(R.id.listView, listFrag, FRAG_TAG_ACTIVITY_LIST).commit();
        }
        detailFrag = (FuseActivityDetailFragment) getFragmentManager().findFragmentByTag(FRAG_TAG_ACTIVITY_DETAIL);
        if (detailFrag == null) {
            // The login fragment hasn't been created yet, so create one and add it.
            detailFrag = new FuseActivityDetailFragment();
            getFragmentManager().beginTransaction().add(R.id.preview, detailFrag, FRAG_TAG_ACTIVITY_DETAIL).hide(detailFrag).commit();
        }
        getFragmentManager().executePendingTransactions();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // If being launched, get the logged in patron from the Intent that launched this activity.
            if (extras.getParcelable("patron") != null) {
                Log.i("overview", "Getting patron from intent");
                setPatron((FusePatron) extras.getParcelable("patron"));
            }
        }
        else if (savedInstanceState != null) {
            // Otherwise check if restoring from a save, and set current patron.
            if (savedInstanceState.getParcelable("patron") != null) {
                setPatron((FusePatron) savedInstanceState.getParcelable("patron"));
            }
        }

        // Bind to the data service.
        bind();
    }

    /**
     * Private helper to unbind to the data sync service.
     */
    private void unbind() {
        if (serviceBound) {
            // Unbind the service on request.
            unbindService(dataConnection);
            serviceBound = false;
        }
    }

    /**
     * Private helper to bind to the data sync service.
     */
    private void bind() {
        if (!serviceBound) {
            // If not bound already, go ahead and start and then bind the service.
            // Start first since we don't want the service to end upon unbinding.
            Intent serv = new Intent(this, FuseDataSynchronizer.class);
            startService(serv);
            bindService(serv, dataConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Set the current patron and set the patrons for our fragments.
     * @param p Currently logged in patron.
     */
    public void setPatron(FusePatron p) {
        currentPatron = p;
        if (currentPatron != null) {
            previewFrag.setPatron(currentPatron, getBaseContext());
            listFrag.setPatron(currentPatron);
        }
    }

    /**
     * Our data service listener callback telling us the data sync service has new data.
     */
    @Override
    public void dataReady() {
        listFrag.getFuseActivityList();
        previewFrag.dataReady();
    }

    /**
     * We need this for when an Async task needs to update the UI with new data.
     * @param r Task to run on UI thread.
     */
    @Override
    public void postToUiThread(Runnable r) {
        runOnUiThread(r);
    }

    /**
     * Listener callback for when a list item or preview thumbnail is clicked to load the details
     * for the activity.
     * @param activity Activity that was clicked.
     */
    @Override
    public void activityClick(FuseActivityData activity) {
        if (activity != null) {
            if (!detailFrag.isVisible()) {
                // Preview fragment is showing, so hide it and show the detail fragment.
                FragmentTransaction tx = getFragmentManager().beginTransaction();
                tx.setCustomAnimations(R.anim.slide_in_right, 0);
                tx.show(detailFrag).hide(previewFrag).addToBackStack(null).commit();
                getFragmentManager().executePendingTransactions();
            }
            // Set the activity for the detail fragment.
            detailFrag.setFuseActivity(activity, currentPatron.getSessionId());
        }
    }

    /**
     * Unbind the service when the activity is stopped.
     */
    @Override
    protected void onStop() {
        super.onStop();
        unbind();
    }

    /**
     * Put the current patron into the savedInstanceState when necessary.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentPatron != null) {
            outState.putParcelable("patron", currentPatron);
        }
    }

    /**
     * Inflate the options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_overview, menu);
        // We add a menu item showing who is logged in.
        menu.getItem(0).setTitle(currentPatron.getWelcomeString());
        return true;
    }

    /**
     * An options menu item was clicked.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent settings = new Intent(this, FuseSettingsActivity.class);
                startActivity(settings);
                return true;
            case R.id.logout:
                // Log the user out.
                logout();
                return true;
            case R.id.refresh:
                // Clear and resync all data for the user.
                dataSyncService.clearAndResync();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Back button was pressed.
     */
    @Override
    public void onBackPressed() {
        if (detailFrag.isVisible()) {
            // The detail fragment is showing, so just do the normal back behavior.
            super.onBackPressed();
        }
        else {
            // The dashboard is up, so hitting back means the user wants to log out.
            logout();
        }
    }

    /**
     * Show a logout confirmation dialog.
     */
    public void logout() {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Logging out")
            .setMessage("Do you want to log out?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // User wants to log out, so clear the patron, remove fragments, clear backstack,
                    // and finish the activity.
                    currentPatron = null;
                    getFragmentManager().beginTransaction().remove(detailFrag).remove(previewFrag).remove(listFrag).commit();
                    for (int i=0; i<getFragmentManager().getBackStackEntryCount(); i++) {
                        getFragmentManager().popBackStack();
                    }
                    setResult(RESULT_OK);
                    finish();
                }
            })
            .setNegativeButton("No", null)
            .show();
    }
}
