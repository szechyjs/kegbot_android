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
import com.goliathonline.android.kegbot.provider.KegbotContract.Kegs;
import com.goliathonline.android.kegbot.provider.KegbotContract.Taps;
import com.goliathonline.android.kegbot.provider.KegbotContract.Users;
import com.goliathonline.android.kegbot.service.SyncService;
import com.goliathonline.android.kegbot.util.Lists;
import com.goliathonline.android.kegbot.util.ParserUtils;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class RemoteJsonHandler extends JsonHandler {
    private static final String TAG = "WorksheetsHandler";

    private RemoteExecutor mExecutor;

    public RemoteJsonHandler(RemoteExecutor executor) {
        super(KegbotContract.CONTENT_AUTHORITY);
        mExecutor = executor;
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONObject parser, ContentResolver resolver)
            throws JSONException, IOException {

        if (parser.has("result"))
        {
        	JSONObject events = parser.getJSONObject("result");
        	JSONArray resultArray = events.getJSONArray("events");
        	JSONObject recent = resultArray.getJSONObject(0);
        	JSONObject event = recent.getJSONObject("event");
        	int id = event.getInt("id");
        	// consider updating each spreadsheet based on update timestamp
        	considerUpdate(Tables.DRINKS, id, Drinks.CONTENT_URI, resolver);
        	considerUpdate(Tables.KEGS, id, Kegs.CONTENT_URI, resolver);
        	considerUpdate(Tables.TAPS, id, Taps.CONTENT_URI, resolver);
        	updateUsers(Users.CONTENT_URI, resolver);
        }
        return Lists.newArrayList();
    }

    private void considerUpdate(String tableName, int version,
            Uri targetDir, ContentResolver resolver) throws HandlerException {
        if (tableName == null) {
            // Silently ignore missing spreadsheets to allow sync to continue.
            Log.w(TAG, "Missing '" + tableName + "' table data");
            return;
//            throw new HandlerException("Missing '" + sheetName + "' table data");
        }

        final long localUpdated = ParserUtils.queryDirUpdated(targetDir, resolver);
        final long serverUpdated = version;
        Log.d(TAG, "considerUpdate() for " + tableName + " found localUpdated="
                + localUpdated + ", server=" + serverUpdated);
        if (localUpdated >= serverUpdated) return;

        String url = SyncService.getApiUrl();
        
        if (Tables.DRINKS.equals(tableName))
        	url = url + "/drinks";
        else if (Tables.KEGS.equals(tableName))
        	url = url + "/kegs";
        else if (Tables.TAPS.equals(tableName))
        	url = url + "/taps";
        
        final HttpGet request = new HttpGet(url);
        final JsonHandler handler = createRemoteHandler(tableName);
        mExecutor.execute(request, handler);
    }

    private JsonHandler createRemoteHandler(String tableName) {
        if (Tables.DRINKS.equals(tableName)) {
            return new RemoteDrinksHandler();
        }
        else if (Tables.KEGS.equals(tableName))
        	return new RemoteKegsHandler(mExecutor);
        else if (Tables.TAPS.equals(tableName))
        	return new RemoteTapHandler(mExecutor);
        else {
            throw new IllegalArgumentException("Unknown worksheet type");
        }
    }
    
    private void updateUsers(Uri targetDir, ContentResolver resolver) 
    		throws HandlerException {
    	Log.d(TAG, "updateUsers() for " + Tables.USERS);
    	RemoteUsersHandler handler = new RemoteUsersHandler(resolver);
    	handler.updateUsers(mExecutor);
    }

    interface Tables {
        String DRINKS = "drinks";
        String USERS = "users";
        String KEGS = "kegs";
        String TAPS = "taps";
    }
}
