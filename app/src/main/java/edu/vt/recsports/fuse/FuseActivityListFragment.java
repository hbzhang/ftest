package edu.vt.recsports.fuse;

import android.app.Activity;
import android.app.Fragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class FuseActivityListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private ListView activityList;
    private ArrayAdapter<FuseActivityData> adapter;
    private String patronId;
    private ArrayList<FuseActivityData> detailList;
    private FuseActivityPreviewFragment.PreviewListener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fuse_activity_list,
                container, false);

        setRetainInstance(true);

        // Set up our activity list and adapter.
        detailList = new ArrayList<FuseActivityData>();
        adapter = new ArrayAdapter<FuseActivityData>(getActivity(), android.R.layout.simple_list_item_1, detailList);

        activityList = (ListView) view.findViewById(R.id.fuseActivityList);
        // Bind to an adapter that the FuseActivityData class provides
        activityList.setAdapter(adapter);
        activityList.setOnItemClickListener(this);

        getFuseActivityList();

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (FuseActivityPreviewFragment.PreviewListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    /**
     * Queries the database to get the latest list of activity data for the current user.
     */
    public void getFuseActivityList() {
        FuseActivityData empty = new FuseActivityData();
        // Clear the list each time since we're adding each item every call.
        detailList.clear();
        if (patronId != null && !patronId.isEmpty()) {
            try {
                // Query the DB for activities for the current user.
                FuseDataStoreHelper help = new FuseDataStoreHelper(getActivity().getApplicationContext());
                SQLiteDatabase db = help.getReadableDatabase();
                Cursor qc = db.query("FuseActivity", FuseDataStoreHelper.getColumns(), "patronId=?", new String[] {patronId}, null, null, "date DESC");
                if (qc != null && qc.getCount() > 0) {
                    // If there are results.
                    qc.moveToFirst();
                    while (!qc.isAfterLast()) {
                        // Then add each activity to the adapter list.
                        FuseActivityData act = FuseActivityData.fromCursor(qc);
                        detailList.add(act);
                        qc.moveToNext();
                    }
                    qc.close();
                }
                db.close();
            }
            catch (Exception e) {
                detailList.clear();
                detailList.add(empty);
                Log.e("FuseActivityList", "Error getting list from DB", e);
            }
        }
        else {
            // There is no current patron, so show just an empty activity.
            detailList.add(empty);
        }
        // Tell the adapter the data changed, so it reloads it on screen.
        adapter.notifyDataSetChanged();
    }

    /**
     * Set the current logged in user.
     */
    public void setPatron(FusePatron p) {
        if (p != null) {
            patronId = p.getPatronId();
        }
    }

    /**
     * List item click handler.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FuseActivityData selected = (FuseActivityData) parent.getItemAtPosition(position);
        if (listener != null) {
            // Notify the listener that an activity was clicked in the list.
            listener.activityClick(selected);
        }
    }
}