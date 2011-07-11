/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.goliathonline.android.kegbot.provider;

import com.goliathonline.android.kegbot.provider.KegbotContract.SearchSuggest;
import com.goliathonline.android.kegbot.provider.KegbotContract.Drinks;
import com.goliathonline.android.kegbot.provider.KegbotContract.Users;
import com.goliathonline.android.kegbot.provider.KegbotContract.Kegs;
import com.goliathonline.android.kegbot.provider.KegbotDatabase.DrinksUsers;
import com.goliathonline.android.kegbot.provider.KegbotDatabase.DrinksKegs;
import com.goliathonline.android.kegbot.provider.KegbotDatabase.Tables;
import com.goliathonline.android.kegbot.service.SyncService;
import com.goliathonline.android.kegbot.util.SelectionBuilder;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provider that stores {@link ScheduleContract} data. Data is usually inserted
 * by {@link SyncService}, and queried by various {@link Activity} instances.
 */
public class KegbotProvider extends ContentProvider {
    private static final String TAG = "KegbotProvider";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.VERBOSE);

    private KegbotDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int KEGS = 200;
    private static final int KEGS_ID = 201;
    private static final int KEGS_ID_DRINKS = 202;

    private static final int DRINKS = 400;
    private static final int DRINKS_STARRED = 401;
    private static final int DRINKS_ID = 402;
    private static final int DRINKS_ID_USERS = 403;
    private static final int DRINKS_ID_KEGS = 404;

    private static final int USERS = 500;
    private static final int USERS_ID = 501;
    private static final int USERS_ID_DRINKS = 502;

    private static final int SEARCH_SUGGEST = 800;

    private static final String MIME_JSON = "text/json";

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = KegbotContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "kegs", KEGS);
        matcher.addURI(authority, "kegs/*", KEGS_ID);
        matcher.addURI(authority, "kegs/*/drinks", KEGS_ID_DRINKS);

        matcher.addURI(authority, "drinks", DRINKS);
        matcher.addURI(authority, "drinks/starred", DRINKS_STARRED);
        matcher.addURI(authority, "drinks/*", DRINKS_ID);
        matcher.addURI(authority, "drinks/*/users", DRINKS_ID_USERS);
        matcher.addURI(authority, "drinks/*/kegs", DRINKS_ID_KEGS);

        matcher.addURI(authority, "users", USERS);
        matcher.addURI(authority, "users/*", USERS_ID);
        matcher.addURI(authority, "users/*/drinks", USERS_ID_DRINKS);

        matcher.addURI(authority, "search_suggest_query", SEARCH_SUGGEST);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mOpenHelper = new KegbotDatabase(context);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case KEGS:
                return Kegs.CONTENT_TYPE;
            case KEGS_ID:
                return Kegs.CONTENT_ITEM_TYPE;
            case KEGS_ID_DRINKS:
                return Drinks.CONTENT_TYPE;
            case DRINKS:
                return Drinks.CONTENT_TYPE;
            case DRINKS_STARRED:
                return Drinks.CONTENT_TYPE;
            case DRINKS_ID:
                return Drinks.CONTENT_ITEM_TYPE;
            case DRINKS_ID_USERS:
                return Users.CONTENT_TYPE;
            case DRINKS_ID_KEGS:
                return Kegs.CONTENT_TYPE;
            case USERS:
                return Users.CONTENT_TYPE;
            case USERS_ID:
                return Users.CONTENT_ITEM_TYPE;
            case USERS_ID_DRINKS:
                return Drinks.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (LOGV) Log.v(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                return builder.where(selection, selectionArgs).query(db, projection, sortOrder);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (LOGV) Log.v(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case KEGS: {
                db.insertOrThrow(Tables.KEGS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return Kegs.buildKegUri(values.getAsString(Kegs.KEG_ID));
            }
            case DRINKS: {
                db.insertOrThrow(Tables.DRINKS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return Drinks.buildDrinkUri(values.getAsString(Drinks.SESSION_ID));
            }
            case DRINKS_ID_USERS: {
                db.insertOrThrow(Tables.DRINKS_USERS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return Users.buildUserUri(values.getAsString(DrinksUsers.USER_ID));
            }
            case DRINKS_ID_KEGS: {
                db.insertOrThrow(Tables.DRINKS_KEGS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return Kegs.buildKegUri(values.getAsString(DrinksKegs.KEG_ID));
            }
            case USERS: {
                db.insertOrThrow(Tables.USERS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return Users.buildUserUri(values.getAsString(Users.USER_ID));
            }
            case SEARCH_SUGGEST: {
                db.insertOrThrow(Tables.SEARCH_SUGGEST, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return SearchSuggest.CONTENT_URI;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (LOGV) Log.v(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (LOGV) Log.v(TAG, "delete(uri=" + uri + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case KEGS: {
                return builder.table(Tables.KEGS);
            }
            case KEGS_ID: {
                final String trackId = Kegs.getKegId(uri);
                return builder.table(Tables.KEGS)
                        .where(Kegs.KEG_ID + "=?", trackId);
            }
            case DRINKS: {
                return builder.table(Tables.DRINKS);
            }
            case DRINKS_ID: {
                final String drinkId = Drinks.getDrinkId(uri);
                return builder.table(Tables.DRINKS)
                        .where(Drinks.DRINK_ID + "=?", drinkId);
            }
            case DRINKS_ID_USERS: {
                final String drinkId = Drinks.getDrinkId(uri);
                return builder.table(Tables.DRINKS_USERS)
                        .where(Drinks.DRINK_ID + "=?", drinkId);
            }
            case DRINKS_ID_KEGS: {
                final String drinkId = Drinks.getDrinkId(uri);
                return builder.table(Tables.DRINKS_KEGS)
                        .where(Drinks.DRINK_ID + "=?", drinkId);
            }
            case USERS: {
                return builder.table(Tables.USERS);
            }
            case USERS_ID: {
                final String userId = Users.getUserId(uri);
                return builder.table(Tables.USERS)
                        .where(Users.USER_ID + "=?", userId);
            }
            case SEARCH_SUGGEST: {
                return builder.table(Tables.SEARCH_SUGGEST);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case KEGS: {
                return builder.table(Tables.KEGS)
                        .map(Kegs.DRINKS_COUNT, Subquery.KEG_DRINKS_COUNT);
            }
            case KEGS_ID: {
                final String kegId = Kegs.getKegId(uri);
                return builder.table(Tables.KEGS)
                        .where(Kegs.KEG_ID + "=?", kegId);
            }
            case KEGS_ID_DRINKS: {
                final String kegId = Kegs.getKegId(uri);
                return builder.table(Tables.DRINKS_KEGS_JOIN_DRINKS)
                        .mapToTable(Drinks._ID, Tables.DRINKS)
                        .mapToTable(Drinks.DRINK_ID, Tables.DRINKS)
                        .where(Qualified.DRINKS_KEGS_KEG_ID + "=?", kegId);
            }
            case DRINKS: {
                return builder.table(Tables.DRINKS)
                        .mapToTable(Drinks._ID, Tables.DRINKS);
            }
            case DRINKS_STARRED: {
                return builder.table(Tables.DRINKS)
                        .mapToTable(Drinks._ID, Tables.DRINKS)
                        .where(Drinks.DRINK_STARRED + "=1");
            }
            case DRINKS_ID: {
                final String drinkId = Drinks.getDrinkId(uri);
                return builder.table(Tables.DRINKS)
                        .mapToTable(Drinks._ID, Tables.DRINKS)
                        .where(Qualified.DRINKS_DRINK_ID + "=?", drinkId);
            }
            case DRINKS_ID_USERS: {
                final String drinkId = Drinks.getDrinkId(uri);
                return builder.table(Tables.DRINKS_USERS_JOIN_USERS)
                        .mapToTable(Users._ID, Tables.USERS)
                        .mapToTable(Users.USER_ID, Tables.USERS)
                        .where(Qualified.DRINKS_USERS_DRINK_ID + "=?", drinkId);
            }
            case DRINKS_ID_KEGS: {
                final String drinkId = Drinks.getDrinkId(uri);
                return builder.table(Tables.DRINKS_KEGS_JOIN_KEGS)
                        .mapToTable(Kegs._ID, Tables.KEGS)
                        .mapToTable(Kegs.KEG_ID, Tables.KEGS)
                        .where(Qualified.DRINKS_KEGS_DRINK_ID + "=?", drinkId);
            }
            case USERS: {
                return builder.table(Tables.USERS);
            }
            case USERS_ID: {
                final String userId = Users.getUserId(uri);
                return builder.table(Tables.USERS)
                        .where(Users.USER_ID + "=?", userId);
            }
            case USERS_ID_DRINKS: {
                final String userId = Users.getUserId(uri);
                return builder.table(Tables.DRINKS_USERS_JOIN_DRINKS)
                        .mapToTable(Drinks._ID, Tables.DRINKS)
                        .mapToTable(Drinks.SESSION_ID, Tables.DRINKS)
                        .where(Qualified.DRINKS_USERS_USER_ID + "=?", userId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    private interface Subquery {
        String SESSIONS_COUNT = "(SELECT COUNT(" + Qualified.DRINKS_DRINK_ID + ") FROM "
                + Tables.DRINKS + ")";

        String DRINKS_CONTAINS_STARRED = "(SELECT MAX(" + Qualified.DRINKS_STARRED + ") FROM "
                + Tables.DRINKS + ")";

        String KEG_DRINKS_COUNT = "(SELECT COUNT(" + Qualified.DRINKS_KEGS_DRINK_ID
                + ") FROM " + Tables.DRINKS_KEGS + " WHERE "
                + Qualified.DRINKS_KEGS_KEG_ID + "=" + Qualified.KEGS_KEG_ID + ")";
    }

    /**
     * {@link ScheduleContract} fields that are fully qualified with a specific
     * parent {@link Tables}. Used when needed to work around SQL ambiguity.
     */
    private interface Qualified {
        String DRINKS_DRINK_ID = Tables.DRINKS + "." + Drinks.DRINK_ID;

        String DRINKS_KEGS_DRINK_ID = Tables.DRINKS_KEGS + "."
                + DrinksKegs.DRINK_ID;
        String DRINKS_KEGS_KEG_ID = Tables.DRINKS_KEGS + "."
                + DrinksKegs.KEG_ID;

        String DRINKS_USERS_DRINK_ID = Tables.DRINKS_USERS + "."
                + DrinksUsers.DRINK_ID;
        String DRINKS_USERS_USER_ID = Tables.DRINKS_USERS + "."
                + DrinksUsers.USER_ID;
        
        String KEGS_KEG_ID = Tables.KEGS + "." + Kegs.KEG_ID;

        @SuppressWarnings("hiding")
        String DRINKS_STARRED = Tables.DRINKS + "." + Drinks.DRINK_STARRED;
    }
}
