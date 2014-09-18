package edu.vt.recsports.fuse;

/**
 * Encapsulates an email and password combo for passing to a LoginTask as a single parameter.
 */
public class FuseLoginParams {
    private String email;
    private String passHash;

    public FuseLoginParams(String e, String p) {
        email = e;
        passHash = p;
    }

    /**
     * Returns the password.
     */
    public String getPassword() {
        return passHash;
    }

    /**
     * Returns the email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns a string array of the email/pass values.
     * @return
     */
    public String[] getValues() {
        return new String[] { email, passHash};
    }

    /**
     * Checks whether the email/pass combo is valid.
     * @return
     */
    public boolean isValid() {
        return (email != null && passHash != null & !email.isEmpty() && !passHash.isEmpty());
    }
}