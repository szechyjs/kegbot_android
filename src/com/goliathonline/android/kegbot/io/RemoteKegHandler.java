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
import com.goliathonline.android.kegbot.provider.KegbotContract.Kegs;
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

import org.json.JSONException;
import org.json.JSONObject;

import static com.goliathonline.android.kegbot.util.ParserUtils.sanitizeId;

/**
 * Handle a remote {@link XmlPullParser} that defines a set of {@link Sessions}
 * entries. Assumes that the remote source is a Google Spreadsheet.
 */
public class RemoteKegHandler extends JsonHandler {
    private static final String TAG = "KegHandler";

    /**
     * Custom format used internally that matches expected concatenation of
     * {@link Columns#SESSION_DATE} and {@link Columns#SESSION_TIME}.
     */
    private static final SimpleDateFormat sTimeFormat = new SimpleDateFormat(
            "EEEE MMM d yyyy h:mma Z", Locale.US);

    public RemoteKegHandler() {
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
        JSONObject keg = result.getJSONObject("keg");
        JSONObject type = result.getJSONObject("type");
        JSONObject image = type.getJSONObject("image");
        
        final String kegId = sanitizeId(keg.getString("id"));
        final Uri kegUri = Kegs.buildKegUri(kegId);
        
        // Check for existing details, only update when changed
        final ContentValues values = queryKegDetails(kegUri, resolver);
        final long localUpdated = values.getAsLong(SyncColumns.UPDATED);
        final long serverUpdated = 500; //entry.getUpdated();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "found keg " + kegId);
            Log.v(TAG, "found localUpdated=" + localUpdated + ", server=" + serverUpdated);
        }

        // Clear any existing values for this session, treating the
        // incoming details as authoritative.
        batch.add(ContentProviderOperation.newDelete(kegUri).build());

        final ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Kegs.CONTENT_URI);

        builder.withValue(SyncColumns.UPDATED, serverUpdated);
        builder.withValue(Kegs.KEG_ID, kegId);
        
        // Inherit starred value from previous row
        if (values.containsKey(Kegs.KEG_STARRED)) {
            builder.withValue(Kegs.KEG_STARRED,
                    values.getAsInteger(Kegs.KEG_STARRED));
        }
        
        if (keg.has("status"))
        	builder.withValue(Kegs.STATUS, keg.getString("status"));
        
        if (keg.has("volume_ml_remain"))
        	builder.withValue(Kegs.VOLUME_REMAIN, keg.getDouble("volume_ml_remain"));
        
        if (keg.has("description"))
        	builder.withValue(Kegs.DESCRIPTION, keg.getString("description"));
        
        if (keg.has("type_id"))
        	builder.withValue(Kegs.TYPE_ID, keg.getString("type_id"));
        
        if (keg.has("size_id"))
        	builder.withValue(Kegs.SIZE_ID, keg.getInt("size_id"));

        if (keg.has("percent_full"))
        	builder.withValue(Kegs.PERCENT_FULL, keg.getDouble("percent_full"));

        if (keg.has("size_name"))
        	builder.withValue(Kegs.SIZE_NAME, keg.getString("size_name"));

        if (keg.has("spilled_ml"))
        	builder.withValue(Kegs.VOLUME_SPILL, keg.getDouble("spilled_ml"));
        
        if (keg.has("size_volume_ml"))
        	builder.withValue(Kegs.VOLUME_SIZE, keg.getDouble("size_volume_ml"));
        
        if (type.has("name"))
        	builder.withValue(Kegs.KEG_NAME, type.getString("name"));
        
        if (type.has("abv"))
        	builder.withValue(Kegs.KEG_ABV, type.getDouble("abv"));
        
        if (image.has("url"))
        	builder.withValue(Kegs.IMAGE_URL, image.getString("url"));
        
        // Normal session details ready, write to provider
        batch.add(builder.build());

        return batch;
    }

    private static ContentValues queryKegDetails(Uri uri, ContentResolver resolver) {
        final ContentValues values = new ContentValues();
        final Cursor cursor = resolver.query(uri, KegsQuery.PROJECTION, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                values.put(SyncColumns.UPDATED, cursor.getLong(KegsQuery.UPDATED));
                values.put(Kegs.KEG_STARRED, cursor.getInt(KegsQuery.STARRED));
            } else {
                values.put(SyncColumns.UPDATED, KegbotContract.UPDATED_NEVER);
            }
        } finally {
            cursor.close();
        }
        return values;
    }

    private interface KegsQuery {
        String[] PROJECTION = {
                SyncColumns.UPDATED,
                Kegs.KEG_STARRED,
        };

        int UPDATED = 0;
        int STARRED = 1;
    }
}
