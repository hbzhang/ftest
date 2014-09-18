package edu.vt.recsports.fuse;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Encapsulates all data about a particular patron (user).
 */
public class FusePatron implements Parcelable {
	private String patronId;
	private String firstName;
	private String lastName;
	private String email;
	private String passport;
    private String sessionId;
    private boolean loggedIn = false;

    public FusePatron(String e, String pass) {
        email = e;
        passport = pass;
        sessionId = "";
    }

    /**
     * Set the user's session ID.
     */
    public void setSessionId(String sid) {
        sessionId = sid;
    }

    /**
     * Returns the user's session ID.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Set the logged in status of the user.
     */
    public void setLoggedIn(boolean status) {
        loggedIn = status;
    }

    /**
     * Set the user's last name.
     */
    public void setLastName(String l) {
        lastName = l;
    }

    /**
     * Set the user's patron ID.
     */
    public void setPatronId(String id) {
        patronId = id;
    }

    /**
     * Returns the user's patron ID.
     */
    public String getPatronId() {
        return patronId;
    }

    /**
     * Returns a welcome string for the user.
     */
    public String getWelcomeString() {
        if (firstName != null && firstName != "") {
            return String.format("Hi %s!", firstName);
        }
        return String.format("Logged in as %s!", email);
    }

    @Override
    public String toString() {
        return String.format("%s %s <%s>", firstName, lastName, email);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Write patron data to a parcel.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {patronId, firstName, lastName, email, passport, sessionId});
    }

    public static final Parcelable.Creator<FusePatron> CREATOR
            = new Parcelable.Creator<FusePatron>() {
        public FusePatron createFromParcel(Parcel in) {
            return new FusePatron(in);
        }

        public FusePatron[] newArray(int size) {
            return new FusePatron[size];
        }
    };

    /**
     * Create a patron from a parcel.
     */
    private FusePatron(Parcel in) {
        String[] vals = new String[6];
        in.readStringArray(vals);
        patronId = vals[0];
        firstName = vals[1];
        lastName = vals[2];
        email = vals[3];
        passport = vals[4];
        sessionId = vals[5];
    }
}
