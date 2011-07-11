package com.goliathonline.android.kegbot.provider;

import com.goliathonline.android.kegbot.provider.KegbotContract.DrinksColumns;
import com.goliathonline.android.kegbot.provider.KegbotContract.SyncColumns;

import android.app.SearchManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Helper for managing {@link SQLiteDatabase} that stores data for
 * {@link ScheduleProvider}.
 */
public class KegbotDatabase extends SQLiteOpenHelper {
    private static final String TAG = "KegbotDatabase";

    private static final String DATABASE_NAME = "kegbot.db";

    // NOTE: carefully update onUpgrade() when bumping database versions to make
    // sure user data is saved.

    private static final int VER_LAUNCH = 1;


    private static final int DATABASE_VERSION = VER_LAUNCH;

    interface Tables {
    	String USERS = "users";
    	String DRINKS = "drinks";
    	String KEGS = "kegs";
    	String TAPS = "taps";
        String DRINKS_USERS = "drinkss_userss";
        String DRINKS_KEGS = "drinks_kegs";

        String SEARCH_SUGGEST = "search_suggest";
        
        String DRINKS_JOIN_USERS_KEG = "drinks "
        		+ "LEFT OUTER JOIN users ON drinks.user=users.id "
        		+ "LEFT OUTER JOIN kegs ON drinks.keg=kegs.id";
        
        String DRINKS_USERS_JOIN_USERS = "drinks_users "
            + "LEFT OUTER JOIN users ON drinks_users.user_id=users.user_id";
        
        String DRINKS_KEGS_JOIN_DRINKS = "drinks_kegs "
            + "LEFT OUTER JOIN drinks ON drinks_kegs.drink_id=drinks.drink_id";
        
        String DRINKS_KEGS_JOIN_KEGS = "drinks_kegs "
            + "LEFT OUTER JOIN kegs ON drinks_kegs.keg_id=keg.keg_id";
        
        String DRINKS_USERS_JOIN_DRINKS = "drinks_users "
            + "LEFT OUTER JOIN drinks ON drinks_users.drink_id=drinks.drink_id";

    }

    public interface DrinksUsers {
    	String DRINK_ID = "drink_id";
    	String USER_ID = "user_id";
    }
    
    public interface DrinksKegs {
    	String DRINK_ID = "drink_id";
    	String KEG_ID = "keg_id";
    }

    public KegbotDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.DRINKS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + DrinksColumns.DRINK_ID + " INTEGER NOT NULL,"
                + DrinksColumns.SESSION_ID + " INTEGER NOT NULL,"
                + DrinksColumns.STATUS + " TEXT NOT NULL,"
                + DrinksColumns.USER_ID + " TEXT NOT NULL,"
                + DrinksColumns.KEG_ID + " INTEGER NOT NULL,"
                + DrinksColumns.VOLUME + " TEXT NOT NULL,"
                + DrinksColumns.DRINK_STARRED + " INTEGER NOT NULL DEFAULT 0,"
                + "UNIQUE (" + DrinksColumns.DRINK_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SEARCH_SUGGEST + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SearchManager.SUGGEST_COLUMN_TEXT_1 + " TEXT NOT NULL)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // NOTE: This switch statement is designed to handle cascading database
        // updates, starting at the current version and falling through to all
        // future upgrade cases. Only use "break;" when you want to drop and
        // recreate the entire database.
        int version = oldVersion;

        switch (version) {

            case VER_LAUNCH:
                version = VER_LAUNCH;
        }

        Log.d(TAG, "after upgrade logic, at version " + version);
        if (version != DATABASE_VERSION) {
            Log.w(TAG, "Destroying old data during upgrade");

            db.execSQL("DROP TABLE IF EXISTS " + Tables.DRINKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.USERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.KEGS);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.SEARCH_SUGGEST);

            onCreate(db);
        }
    }
}