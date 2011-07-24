package com.goliathonline.android.kegbot.provider;

import com.goliathonline.android.kegbot.provider.KegbotContract.Drinks;
import com.goliathonline.android.kegbot.provider.KegbotContract.DrinksColumns;
import com.goliathonline.android.kegbot.provider.KegbotContract.Kegs;
import com.goliathonline.android.kegbot.provider.KegbotContract.KegsColumns;
import com.goliathonline.android.kegbot.provider.KegbotContract.SyncColumns;
import com.goliathonline.android.kegbot.provider.KegbotContract.TapsColumns;
import com.goliathonline.android.kegbot.provider.KegbotContract.Users;
import com.goliathonline.android.kegbot.provider.KegbotContract.UsersColumns;

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
        String DRINKS_USER = "drinks_user";
        String DRINKS_KEG = "drinks_keg";

        String SEARCH_SUGGEST = "search_suggest";
        
        String DRINKS_JOIN_USERS_KEG = "drinks "
        		+ "LEFT OUTER JOIN users ON drinks.user=users.id "
        		+ "LEFT OUTER JOIN kegs ON drinks.keg=kegs.id";
        
        String DRINKS_USERS_JOIN_USERS = "drinks_user "
            + "LEFT OUTER JOIN users ON drinks_user.user_id=users.user_id";
        
        String DRINKS_KEGS_JOIN_DRINKS = "drinks_keg "
            + "LEFT OUTER JOIN drinks ON drinks_keg.drink_id=drinks.drink_id";
        
        String DRINKS_KEGS_JOIN_KEGS = "drinks_keg "
            + "LEFT OUTER JOIN kegs ON drinks_keg.keg_id=kegs.keg_id";
        
        String DRINKS_USERS_JOIN_DRINKS = "drinks_user "
            + "LEFT OUTER JOIN drinks ON drinks_user.drink_id=drinks.drink_id";

    }

    public interface DrinksUser {
    	String DRINK_ID = "drink_id";
    	String USER_ID = "user_id";
    }
    
    public interface DrinksKeg {
    	String DRINK_ID = "drink_id";
    	String KEG_ID = "keg_id";
    }
    
    /** {@code REFERENCES} clauses. */
    private interface References {
        String KEG_ID = "REFERENCES " + Tables.KEGS + "(" + Kegs.KEG_ID + ")";
        String DRINK_ID = "REFERENCES " + Tables.DRINKS + "(" + Drinks.DRINK_ID + ")";
        String USER_ID = "REFERENCES " + Tables.USERS + "(" + Users.USER_ID + ")";
    }

    public KegbotDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.DRINKS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + DrinksColumns.DRINK_ID + " INTEGER NOT NULL,"
                + DrinksColumns.SESSION_ID + " INTEGER NOT NULL,"
                + DrinksColumns.STATUS + " TEXT NOT NULL,"
                + DrinksColumns.USER_ID + " TEXT,"
                + DrinksColumns.KEG_ID + " INTEGER NOT NULL,"
                + DrinksColumns.VOLUME + " DOUBLE NOT NULL,"
                + DrinksColumns.POUR_TIME + " TEXT NOT NULL,"
                + DrinksColumns.DRINK_STARRED + " INTEGER NOT NULL DEFAULT 0,"
                + SyncColumns.UPDATED + " INTEGER,"
                + "UNIQUE (" + DrinksColumns.DRINK_ID + ") ON CONFLICT REPLACE)");
        
        db.execSQL("CREATE TABLE " + Tables.KEGS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KegsColumns.STATUS + " TEXT NOT NULL,"
                + KegsColumns.VOLUME_REMAIN + " DOUBLE NOT NULL,"
                + KegsColumns.DESCRIPTION + " TEXT NOT NULL,"
                + KegsColumns.TYPE_ID + " TEXT NOT NULL,"
                + KegsColumns.SIZE_ID + " INTEGER NOT NULL,"
                + KegsColumns.PERCENT_FULL + " DOUBLE NOT NULL,"
                + KegsColumns.SIZE_NAME + " TEXT NOT NULL,"
                + KegsColumns.VOLUME_SPILL + " DOUBLE NOT NULL,"
                + KegsColumns.KEG_ID + " INTEGER NOT NULL,"
                + KegsColumns.VOLUME_SIZE + " DOUBLE NOT NULL,"
                + KegsColumns.KEG_STARRED + " INTEGER NOT NULL DEFAULT 0,"
                + KegsColumns.KEG_NAME + " TEXT NOT NULL,"
                + KegsColumns.KEG_ABV + " DOUBLE NOT NULL,"
                + KegsColumns.IMAGE_URL + " TEXT,"
                + SyncColumns.UPDATED + " INTEGER,"
                + "UNIQUE (" + KegsColumns.KEG_ID + ") ON CONFLICT REPLACE)");
        
        db.execSQL("CREATE TABLE " + Tables.TAPS + " ("
        		+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        		+ TapsColumns.TAP_ID + " INTEGER NOT NULL,"
        		+ TapsColumns.TAP_NAME + " TEXT,"
        		+ TapsColumns.KEG_ID + " INTEGER,"
        		+ TapsColumns.STATUS + " TEXT NOT NULL,"
        		+ TapsColumns.PERCENT_FULL + " DOUBLE,"
        		+ TapsColumns.SIZE_NAME + " TEXT,"
            	+ TapsColumns.VOL_REMAIN + " DOUBLE,"
            	+ TapsColumns.VOL_SIZE + " DOUBLE,"
        		+ TapsColumns.BEER_NAME + " TEXT,"
        		+ TapsColumns.DESCRIPTION + " TEXT,"
        		+ TapsColumns.LAST_TEMP + " DOUBLE,"
        		+ TapsColumns.LAST_TEMP_TIME + " TEXT,"
        		+ TapsColumns.IMAGE_URL + " TEXT,"
        		+ SyncColumns.UPDATED + " INTEGER,"
        		+ "UNIQUE (" + TapsColumns.TAP_ID + ") ON CONFLICT REPLACE)");
        
        db.execSQL("CREATE TABLE " + Tables.USERS + " ("
        		+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        		+ UsersColumns.USER_ID + " TEXT NOT NULL,"
        		+ UsersColumns.USER_NAME + " TEXT,"
        		+ UsersColumns.USER_IMAGE_URL + " TEXT,"
        		+ SyncColumns.UPDATED + " INTEGER,"
        		+ "UNIQUE (" + UsersColumns.USER_ID + ") ON CONFLICT REPLACE)");
        
        db.execSQL("CREATE TABLE " + Tables.DRINKS_KEG + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + DrinksKeg.DRINK_ID + " INTEGER NOT NULL " + References.DRINK_ID + ","
                + DrinksKeg.KEG_ID + " INTEGER NOT NULL " + References.KEG_ID + ","
                + "UNIQUE (" + DrinksKeg.DRINK_ID + ","
                        + DrinksKeg.KEG_ID + ") ON CONFLICT REPLACE)");
        
        db.execSQL("CREATE TABLE " + Tables.DRINKS_USER + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + DrinksUser.DRINK_ID + " INTEGER NOT NULL " + References.DRINK_ID + ","
                + DrinksUser.USER_ID + " INTEGER NOT NULL " + References.USER_ID + ","
                + "UNIQUE (" + DrinksUser.DRINK_ID + ","
                        + DrinksUser.USER_ID + ") ON CONFLICT REPLACE)");

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
            
            db.execSQL("DROP TABLE IF EXISTS " + Tables.DRINKS_KEG);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.DRINKS_USER);

            onCreate(db);
        }
    }
}
