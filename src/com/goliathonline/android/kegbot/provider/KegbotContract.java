package com.goliathonline.android.kegbot.provider;

import com.goliathonline.android.kegbot.provider.ScheduleContract.Sessions;
import com.goliathonline.android.kegbot.provider.ScheduleContract.Speakers;
import com.goliathonline.android.kegbot.provider.ScheduleContract.SpeakersColumns;
import com.goliathonline.android.kegbot.provider.ScheduleContract.SyncColumns;
import com.goliathonline.android.kegbot.provider.ScheduleContract.Tracks;
import com.goliathonline.android.kegbot.provider.ScheduleContract.TracksColumns;
import com.goliathonline.android.kegbot.provider.ScheduleContract.Vendors;
import com.goliathonline.android.kegbot.util.ParserUtils;

import android.app.SearchManager;
import android.graphics.Color;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for interacting with {@link ScheduleProvider}. Unless
 * otherwise noted, all time-based fields are milliseconds since epoch and can
 * be compared against {@link System#currentTimeMillis()}.
 * <p>
 * The backing {@link android.content.ContentProvider} assumes that {@link Uri} are generated
 * using stronger {@link String} identifiers, instead of {@code int}
 * {@link BaseColumns#_ID} values, which are prone to shuffle during sync.
 */
public class KegbotContract {

    /**
     * Special value for {@link SyncColumns#UPDATED} indicating that an entry
     * has never been updated, or doesn't exist yet.
     */
    public static final long UPDATED_NEVER = -2;

    /**
     * Special value for {@link SyncColumns#UPDATED} indicating that the last
     * update time is unknown, usually when inserted from a local file source.
     */
    public static final long UPDATED_UNKNOWN = -1;

    public interface SyncColumns {
        /** Last time this entry was updated or synchronized. */
        String UPDATED = "updated";
    }

    interface DrinksColumns {
        /** Unique string identifying this session. */
        String DRINK_ID = "drink_id";
        String USER_ID = "user_id";
        String KEG_ID = "keg_id";
        String VOLUME = "volume_ml";
        String SESSION_ID = "session_id";
        String STATUS = "status";
        /** User-specific flag indicating starred status. */
        String DRINK_STARRED = "drink_starred";
    }
    
    interface UsersColumns {
        /** Unique string identifying this user. */
        String USER_ID = "user_id";
        /** Name of this user. */
        String USER_NAME = "user_name";
        /** Profile photo of this user. */
        String USER_IMAGE_URL = "user_image_url";
    }
    
    interface KegsColumns {
        /** Unique string identifying this keg. */
        String KEG_ID = "keg_id";
        /** Name describing this keg. */
        String KEG_NAME = "keg_name";
    }

    public static final String CONTENT_AUTHORITY = "com.goliathonline.android.kegbot";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_DRINKS = "drinks";
    private static final String PATH_KEGS = "kegs";
    private static final String PATH_USERS = "users";
    private static final String PATH_STARRED = "starred";
    private static final String PATH_SEARCH_SUGGEST = "search_suggest_query";

    /**
     * Each session is a block of time that has a {@link Tracks}, a
     * {@link Rooms}, and zero or more {@link Speakers}.
     */
    public static class Drinks implements DrinksColumns,
            SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_DRINKS).build();
        public static final Uri CONTENT_STARRED_URI =
                CONTENT_URI.buildUpon().appendPath(PATH_STARRED).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.kegbot.drink";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.kegbot.drink";

        // TODO: shortcut primary track to offer sub-sorting here
        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = DrinksColumns.DRINK_ID + " ASC";

        /** Build {@link Uri} for requested {@link #DRINK_ID}. */
        public static Uri buildDrinkUri(String drinkId) {
            return CONTENT_URI.buildUpon().appendPath(drinkId).build();
        }

        /** Read {@link #DRINK_ID} from {@link Drinks} {@link Uri}. */
        public static String getDrinkId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getSearchQuery(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Generate a {@link #DRINK_ID} that will always match the requested
         * {@link Drinks} details.
         */
        public static String generateDrinkId(String title) {
            return ParserUtils.sanitizeId(title);
        }
    }
    
    /**
     * Tracks are overall categories for {@link Sessions} and {@link Vendors},
     * such as "Android" or "Enterprise."
     */
    public static class Kegs implements KegsColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_KEGS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.kegbot.keg";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.kegbot.keg";

        /** Count of {@link Sessions} inside given track. */
        public static final String DRINKS_COUNT = "drinks_count";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = KegsColumns.KEG_NAME + " ASC";

        /** "All tracks" ID. */
        public static final String ALL_KEG_ID = "all";

        /** Build {@link Uri} for requested {@link #KEG_ID}. */
        public static Uri buildKegUri(String kegId) {
            return CONTENT_URI.buildUpon().appendPath(kegId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Drinks} associated
         * with the requested {@link #KEG_ID}.
         */
        public static Uri buildDrinksUri(String kegId) {
            return CONTENT_URI.buildUpon().appendPath(kegId).appendPath(PATH_DRINKS).build();
        }

        /** Read {@link #KEG_ID} from {@link Kegs} {@link Uri}. */
        public static String getKegId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Generate a {@link #KEG_ID} that will always match the requested
         * {@link Kegs} details.
         */
        public static String generateKegId(String title) {
            return ParserUtils.sanitizeId(title);
        }
    }
    
    /**
     * Speakers are individual people that lead {@link Sessions}.
     */
    public static class Users implements UsersColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_USERS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.kegbot.user";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.kegbot.user";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = UsersColumns.USER_NAME
                + " COLLATE NOCASE ASC";

        /** Build {@link Uri} for requested {@link #USER_ID}. */
        public static Uri buildUserUri(String userId) {
            return CONTENT_URI.buildUpon().appendPath(userId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Drinks} associated
         * with the requested {@link #USER_ID}.
         */
        public static Uri buildDrinksDirUri(String userId) {
            return CONTENT_URI.buildUpon().appendPath(userId).appendPath(PATH_DRINKS).build();
        }

        /** Read {@link #USER_ID} from {@link Users} {@link Uri}. */
        public static String getUserId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Generate a {@link #USER_ID} that will always match the requested
         * {@link Users} details.
         */
        public static String generateUserId(String userLdap) {
            return ParserUtils.sanitizeId(userLdap);
        }
    }

 
    public static class SearchSuggest {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEARCH_SUGGEST).build();

        public static final String DEFAULT_SORT = SearchManager.SUGGEST_COLUMN_TEXT_1
                + " COLLATE NOCASE ASC";
    }

    private KegbotContract() {
    }
}
