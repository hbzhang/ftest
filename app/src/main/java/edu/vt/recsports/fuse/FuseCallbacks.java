package edu.vt.recsports.fuse;

/**
 * Callbacks for our main activity to deal with logging in.
 */
public interface FuseCallbacks {
	public void onClicked(int clickedViewId, String[] values);
    public void loggedIn(FusePatron p);
    public void handleProgress(int progress);
}