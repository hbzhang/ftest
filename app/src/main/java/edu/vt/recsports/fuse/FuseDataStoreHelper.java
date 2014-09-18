package edu.vt.recsports.fuse;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Custom database helper class.
 */
public class FuseDataStoreHelper extends SQLiteOpenHelper {
    private final static String[] COLUMNS = new String[] {"id", "patronId", "trainerId", "date", "type", "thumbImage", "thumbImageURL"};
    private final static String DB_SCHEMA = "CREATE TABLE IF NOT EXISTS FuseActivity " +
    		"(_id INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT NOT NULL, type TEXT, patronId TEXT," +
            "trainerId TEXT, date TEXT, thumbImageURL TEXT, thumbImage BLOB, fullImageURL TEXT, comments TEXT)";
    private final static String DB_NAME = "FuseActivity.db";
    private final static int DB_VERSION = 1;

    public FuseDataStoreHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Database creation.
     */
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(DB_SCHEMA);
    }

    /**
     * Database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS FuseActivity");
        onCreate(db);
    }

    /**
     * Returns all the columns for the activity table.
     */
    public static String[] getColumns() {
        return COLUMNS;
    }

    /**
     * Primary function. Handles updating when an activity exists in DB already, or inserts a new
     * record otherwise.
     */
    public boolean upsertActivity(FuseActivityData activity, SQLiteDatabase db) {
        Cursor qc = db.query("FuseActivity", new String[] {"id"}, "id=?", new String[] {activity.getId()}, null, null, null);
        if (qc != null && qc.getCount() == 0) {
            // Activity is new, so add it.
            qc.close();
            return insertActivity(activity, db);
        }
        else {
            // Activity exists, so update it.
            qc.close();
            return updateActivity(activity, db);
        }
    }

    /**
     * Private helper function to insert a new activity.
     */
    private boolean insertActivity(FuseActivityData activity, SQLiteDatabase db) {
        boolean result = false;
        db.beginTransaction();
        try {
            // Put all our values into a query.
            ContentValues vals = new ContentValues();
            vals.put("id", activity.getId());
            vals.put("patronId", activity.getPatron());
            vals.put("type", activity.getActivityType());
            vals.put("trainerId", activity.getTrainerName());
            vals.put("date", activity.getActivityDate());
            vals.put("thumbImage", activity.getThumbnail());
            vals.put("thumbImageURL", activity.getThumbnailURL());
            vals.put("fullImageURL", "");
            vals.put("comments", "");
            if (db.insert("FuseActivity", null, vals) != -1) {
                // Insert was successful, so make sure the transaction knows it.
                db.setTransactionSuccessful();
                result = true;
            }
        }
        catch (Exception e) {
            Log.e("DataStoreHelper", "Error inserting", e);
        }
        db.endTransaction();
        return result;
    }

    /**
     * Returns the most recent activity for the given user.
     */
    public FuseActivityData getMostRecentActivity(String patronId, SQLiteDatabase db) {
        FuseActivityData recent = null;
        Cursor qc = db.query("FuseActivity", COLUMNS, "patronId=?", new String[] {patronId}, null, null, "date DESC");
        if (qc != null && qc.getCount() > 0) {
            qc.moveToFirst();
            recent = new FuseActivityData();
            recent.setId(qc.getString(0));
            recent.setPatron(qc.getString(1));
            recent.setTrainer(qc.getString(2));
            recent.setActivityDate(qc.getString(3));
            recent.setActivityType(qc.getString(4));
            recent.setThumbnail(qc.getBlob(5));
            recent.setThumbnailURL(qc.getString(6));
        }
        qc.close();
        return recent;
    }

    /**
     * Private helper function to update an existing activity.
     */
    private boolean updateActivity(FuseActivityData activity, SQLiteDatabase db) {
        boolean result = false;
        db.beginTransaction();
        try {
            // Put our values into the query.
            ContentValues vals = new ContentValues();
            vals.put("patronId", activity.getPatron());
            vals.put("type", activity.getActivityType());
            vals.put("trainerId", activity.getTrainerName());
            vals.put("date", activity.getActivityDate());
            vals.put("thumbImage", activity.getThumbnail());
            vals.put("thumbImageURL", activity.getThumbnailURL());
            vals.put("fullImageURL", "");
            vals.put("comments", "");
            if (db.update("FuseActivity", vals, "id=?", new String[]{activity.getId()}) != -1) {
                // Update was successful, so flag the transaction as a success.
                result = true;
                db.setTransactionSuccessful();
            }
        }
        catch (Exception e) {
            Log.e("DataStoreHelper", "Error updating", e);
        }
        db.endTransaction();
        return result;
    }
}
