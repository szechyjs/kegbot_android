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
import com.goliathonline.android.kegbot.provider.KegbotContract.Users;
import com.goliathonline.android.kegbot.util.Lists;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;

import static com.goliathonline.android.kegbot.util.ParserUtils.sanitizeId;

/**
 * Handle a remote {@link XmlPullParser} that defines a set of {@link Sessions}
 * entries. Assumes that the remote source is a Google Spreadsheet.
 */
public class RemoteUserHandler extends JsonHandler {
    private static final String TAG = "UserHandler";

    public RemoteUserHandler() {
        super(KegbotContract.CONTENT_AUTHORITY);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<ContentProviderOperation> parse(JSONObject parser, ContentResolver resolver)
            throws JSONException, IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        // Walk document, parsing any incoming entries
        if(!parser.has("result"))
        	return batch;
        JSONObject result = parser.getJSONObject("result");
        JSONObject keg = result.getJSONObject("user");
        
        final String userId = sanitizeId(keg.getString("username"));
        final Uri userUri = Users.buildUserUri(userId);
        
        // Check for existing details, only update when changed
        final ContentValues values = queryUserDetails(userUri, resolver);
        final long localUpdated = values.getAsLong(SyncColumns.UPDATED);
        final long serverUpdated = 500; //entry.getUpdated();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "found user " + userId);
            Log.v(TAG, "found localUpdated=" + localUpdated + ", server=" + serverUpdated);
        }

        // Clear any existing values for this session, treating the
        // incoming details as authoritative.
        batch.add(ContentProviderOperation.newDelete(userUri).build());

        final ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Users.CONTENT_URI);

        builder.withValue(SyncColumns.UPDATED, serverUpdated);
        builder.withValue(Users.USER_ID, userId);
        
        if (keg.has("image"))
        {
        	JSONObject image = keg.getJSONObject("image");
        	if (image.has("url"))
        		builder.withValue(Users.USER_IMAGE_URL, image.getString("url"));
        }

        // Normal session details ready, write to provider
        batch.add(builder.build());

        return batch;
    }

    private static ContentValues queryUserDetails(Uri uri, ContentResolver resolver) {
        final ContentValues values = new ContentValues();
        final Cursor cursor = resolver.query(uri, UserQuery.PROJECTION, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                values.put(SyncColumns.UPDATED, cursor.getLong(UserQuery.UPDATED));
            } else {
                values.put(SyncColumns.UPDATED, KegbotContract.UPDATED_NEVER);
            }
        } finally {
            cursor.close();
        }
        return values;
    }

    private interface UserQuery {
        String[] PROJECTION = {
                SyncColumns.UPDATED,
                Users.USER_ID,
                Users.USER_IMAGE_URL,
        };

        int UPDATED = 0;
        int USER_ID = 1;
        int USER_IMAGE_URL = 2;
    }
}
