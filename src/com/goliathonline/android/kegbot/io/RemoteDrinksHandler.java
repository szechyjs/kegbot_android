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

package com.goliathonline.android.kegbot.io;

import com.goliathonline.android.kegbot.provider.KegbotContract;
import com.goliathonline.android.kegbot.provider.KegbotContract.Drinks;
import com.goliathonline.android.kegbot.provider.KegbotContract.SyncColumns;
import com.goliathonline.android.kegbot.util.Lists;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.goliathonline.android.kegbot.util.ParserUtils.sanitizeId;

/**
 * Handle a remote {@link XmlPullParser} that defines a set of {@link Sessions}
 * entries. Assumes that the remote source is a Google Spreadsheet.
 */
public class RemoteDrinksHandler extends JsonHandler {
    private static final String TAG = "SessionsHandler";

    /**
     * Custom format used internally that matches expected concatenation of
     * {@link Columns#SESSION_DATE} and {@link Columns#SESSION_TIME}.
     */
    private static final SimpleDateFormat sTimeFormat = new SimpleDateFormat(
            "EEEE MMM d yyyy h:mma Z", Locale.US);

    public RemoteDrinksHandler() {
        super(KegbotContract.CONTENT_AUTHORITY);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<ContentProviderOperation> parse(JSONObject parser, ContentResolver resolver)
            throws JSONException, IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        // Walk document, parsing any incoming entries
        int drink_id = 0;
        JSONObject result = parser.getJSONObject("result");
        JSONArray drinks = result.getJSONArray("drinks");
        JSONObject drink;
        for (int i =0; i < drinks.length(); i++) {
            if (drink_id == 0) { // && ENTRY.equals(parser.getName()
                // Process single spreadsheet row at a time
            	drink = drinks.getJSONObject(i);
                final String drinkId = sanitizeId(drink.getString("id"));
                final Uri drinkUri = Drinks.buildDrinkUri(drinkId);

                // Check for existing details, only update when changed
                final ContentValues values = queryDrinkDetails(drinkUri, resolver);
                final long localUpdated = values.getAsLong(SyncColumns.UPDATED);
                final long serverUpdated = 500; //entry.getUpdated();
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "found drink " + drinkId);
                    Log.v(TAG, "found localUpdated=" + localUpdated + ", server=" + serverUpdated);
                }
                if (localUpdated != KegbotContract.UPDATED_NEVER) continue;

                // Clear any existing values for this session, treating the
                // incoming details as authoritative.
                batch.add(ContentProviderOperation.newDelete(drinkUri).build());

                final ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(Drinks.CONTENT_URI);

                builder.withValue(SyncColumns.UPDATED, serverUpdated);
                builder.withValue(Drinks.DRINK_ID, drinkId);

                // Inherit starred value from previous row
                if (values.containsKey(Drinks.DRINK_STARRED)) {
                    builder.withValue(Drinks.DRINK_STARRED,
                            values.getAsInteger(Drinks.DRINK_STARRED));
                }
                
                if (drink.has("session_id"))
                	builder.withValue(Drinks.SESSION_ID, drink.getInt("session_id"));
                if (drink.has("status"))
                	builder.withValue(Drinks.STATUS, drink.getString("status"));
                if (drink.has("user_id"))
                	builder.withValue(Drinks.USER_ID, drink.getString("user_id"));
                if (drink.has("keg_id"))
                	builder.withValue(Drinks.KEG_ID, drink.getInt("keg_id"));
                if (drink.has("volume_ml"))
                	builder.withValue(Drinks.VOLUME, drink.getDouble("volume_ml"));
                
                // Normal session details ready, write to provider
                batch.add(builder.build());
            }
        }

        return batch;
    }

    private static ContentValues queryDrinkDetails(Uri uri, ContentResolver resolver) {
        final ContentValues values = new ContentValues();
        final Cursor cursor = resolver.query(uri, SessionsQuery.PROJECTION, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                values.put(SyncColumns.UPDATED, cursor.getLong(SessionsQuery.UPDATED));
                values.put(Drinks.DRINK_STARRED, cursor.getInt(SessionsQuery.STARRED));
            } else {
                values.put(SyncColumns.UPDATED, KegbotContract.UPDATED_NEVER);
            }
        } finally {
            cursor.close();
        }
        return values;
    }

    private interface SessionsQuery {
        String[] PROJECTION = {
                SyncColumns.UPDATED,
                Drinks.DRINK_STARRED,
        };

        int UPDATED = 0;
        int STARRED = 1;
    }

    /** Columns coming from remote spreadsheet. */
    private interface Columns {
        String SESSION_DATE = "sessiondate";
        String SESSION_TIME = "sessiontime";
        String SESSION_ROOM = "sessionroom";
        String SESSION_TRACK = "sessiontrack";
        String SESSION_LEVEL = "sessionlevel";
        String SESSION_TITLE = "sessiontitle";
        String SESSION_TAGS = "sessiontags";
        String SESSION_HASHTAG = "sessionhashtag";
        String SESSION_SLUG = "sessionslug";
        String SESSION_SPEAKERS = "sessionspeakers";
        String SESSION_ABSTRACT = "sessionabstract";
        String SESSION_REQUIREMENTS = "sessionrequirements";
        String SESSION_URL = "sessionurl";
        String SESSION_MODERATOR_URL = "sessionmoderatorurl";
        String SESSION_YOUTUBE_URL = "sessionyoutubeurl";
        String SESSION_PDF_URL = "sessionpdfurl";
        String SESSION_FEEDBACK_URL = "sessionfeedbackurl";
        String SESSION_NOTES_URL = "sessionnotesurl";

        // session_date: Wednesday May 19
        // session_time: 10:45am-11:45am
        // session_room: 6
        // session_track: Enterprise, App Engine
        // session_level: 201
        // session_title: Run corporate applications on Google App Engine?  Yes we do.
        // session_slug: run-corporate-applications
        // session_tags: Enterprise, SaaS, PaaS, Hosting, App Engine, Java
        // session_speakers: Ben Fried, John Smith
        // session_abstract: And you can too! Come hear Google's CIO Ben Fried describe...
        // session_requirements: None
        // session_url: http://www.google.com/events/io/2011/foo
        // session_hashtag: #io11android1
        // session_youtube_url
        // session_pdf_url
        // session_feedback_url
        // session_moderator_url
        // session_notes_url
    }
}
