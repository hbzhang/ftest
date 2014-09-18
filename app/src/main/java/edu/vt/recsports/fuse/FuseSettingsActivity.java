package edu.vt.recsports.fuse;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FuseSettingsActivity extends PreferenceActivity {
    private static final String FRAG_TAG_PREFS = "PREFERENCES";
    private FusePreferenceFragment prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = (FusePreferenceFragment) getFragmentManager().findFragmentByTag(FRAG_TAG_PREFS);
        if (prefs == null) {
            prefs = new FusePreferenceFragment();
            getFragmentManager().beginTransaction().add(android.R.id.content, prefs, FRAG_TAG_PREFS).commit();
        }
    }

    public static class FusePreferenceFragment extends PreferenceFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            addPreferencesFromResource(R.xml.settings);

            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }
}
