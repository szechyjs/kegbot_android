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

import static com.goliathonline.android.kegbot.util.ParserUtils.sanitizeId;

import com.goliathonline.android.kegbot.provider.KegbotContract;
import com.goliathonline.android.kegbot.provider.KegbotContract.Kegs;
import com.goliathonline.android.kegbot.provider.KegbotContract.SyncColumns;
import com.goliathonline.android.kegbot.provider.KegbotContract.Taps;
import com.goliathonline.android.kegbot.util.Lists;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;


import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handle a remote {@link JSONObject} that defines a set of {@link Kegs}
 * entries.
 */
public class RemoteTapHandler extends JsonHandler {
    private static final String TAG = "KegsHandler";

    public RemoteTapHandler(RemoteExecutor executor) {
        super(KegbotContract.CONTENT_AUTHORITY);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<ContentProviderOperation> parse(JSONObject parser, ContentResolver resolver)
            throws JSONException, IOException {
    	final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
    	
    	if (parser.has("result"))
    	{
    		JSONObject events = parser.getJSONObject("result");
        	JSONArray resultArray = events.getJSONArray("taps");
        	int numKegs = resultArray.length();
        	
        	JSONObject taps;
        	for (int i = 0; i < numKegs; i++)
        	{
        		taps = resultArray.getJSONObject(i);
        		
        		JSONObject keg = taps.getJSONObject("keg");
        		JSONObject tap = taps.getJSONObject("tap");
        		JSONObject beer = taps.getJSONObject("beer_type");
        		JSONObject image = beer.getJSONObject("image");

        		final String tapId = sanitizeId(tap.getString("id"));
                final Uri tapUri = Taps.buildTapUri(tapId);
                
                // Check for existing details, only update when changed
                final ContentValues values = queryTapDetails(tapUri, resolver);
                final long localUpdated = values.getAsLong(SyncColumns.UPDATED);
                final long serverUpdated = 500; //entry.getUpdated();
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "found tap " + tapId);
                    Log.v(TAG, "found localUpdated=" + localUpdated + ", server=" + serverUpdated);
                }

                // Clear any existing values for this session, treating the
                // incoming details as authoritative.
                batch.add(ContentProviderOperation.newDelete(tapUri).build());

                final ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(Taps.CONTENT_URI);

                builder.withValue(SyncColumns.UPDATED, serverUpdated);
                builder.withValue(Taps.TAP_ID, tapId);
                
                if (tap.has("name"))
                	builder.withValue(Taps.TAP_NAME, tap.getString("name"));
                
                if (tap.has("current_keg_id"))
                	builder.withValue(Taps.KEG_ID, tap.getDouble("current_keg_id"));
                
                if (keg.has("status"))
                	builder.withValue(Taps.STATUS, keg.getString("status"));
                
                if (keg.has("percent_full"))
                	builder.withValue(Taps.PERCENT_FULL, keg.getString("percent_full"));

                if (keg.has("size_name"))
                	builder.withValue(Taps.SIZE_NAME, keg.getString("size_name"));

                if (keg.has("volume_ml_remain"))
                	builder.withValue(Taps.VOL_REMAIN, keg.getDouble("volume_ml_remain"));
                
                if (keg.has("size_volume_ml"))
                	builder.withValue(Taps.VOL_SIZE, keg.getDouble("size_volume_ml"));
                
                if (beer.has("name"))
                	builder.withValue(Taps.BEER_NAME, beer.getString("name"));
                
                if (keg.has("description"))
                	builder.withValue(Taps.DESCRIPTION, keg.getString("description"));
                
                JSONObject last_temp = tap.getJSONObject("last_temperature");
                
                if (last_temp.has("temperature_c"))
                	builder.withValue(Taps.LAST_TEMP, last_temp.getString("temperature_c"));
                
                if (image.has("url"))
                	builder.withValue(Taps.IMAGE_URL, image.getString("url"));
                
             // Normal tap details ready, write to provider
                batch.add(builder.build());
        	}
    	}
        
    	return batch;
    }
    
    private static ContentValues queryTapDetails(Uri uri, ContentResolver resolver) {
        final ContentValues values = new ContentValues();
        final Cursor cursor = resolver.query(uri, TapsQuery.PROJECTION, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                values.put(SyncColumns.UPDATED, cursor.getLong(TapsQuery.UPDATED));
            } else {
                values.put(SyncColumns.UPDATED, KegbotContract.UPDATED_NEVER);
            }
        } finally {
            cursor.close();
        }
        return values;
    }
    
    private interface TapsQuery {
        String[] PROJECTION = {
                SyncColumns.UPDATED,
        };

        int UPDATED = 0;
    }
}
