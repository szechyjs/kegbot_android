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

import com.goliathonline.android.kegbot.io.JsonHandler.HandlerException;
import com.goliathonline.android.kegbot.io.RemoteWorksheetsHandler.Tables;
import com.goliathonline.android.kegbot.provider.KegbotContract;
import com.goliathonline.android.kegbot.provider.KegbotContract.Kegs;
import com.goliathonline.android.kegbot.service.SyncService;
import com.goliathonline.android.kegbot.util.Lists;
import com.goliathonline.android.kegbot.util.ParserUtils;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handle a remote {@link JSONObject} that defines a set of {@link Kegs}
 * entries.
 */
public class RemoteKegsHandler extends JsonHandler {
    private static final String TAG = "KegsHandler";

    private RemoteExecutor mExecutor;

    public RemoteKegsHandler(RemoteExecutor executor) {
        super(KegbotContract.CONTENT_AUTHORITY);
        mExecutor = executor;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<ContentProviderOperation> parse(JSONObject parser, ContentResolver resolver)
            throws JSONException, IOException {
    	
    	if (parser.has("result"))
    	{
    		JSONObject events = parser.getJSONObject("result");
        	JSONArray resultArray = events.getJSONArray("kegs");
        	int numKegs = resultArray.length();
        	List<String> kegIDs = new ArrayList<String>();
        	
        	JSONObject keg;
        	for (int i = 0; i < numKegs; i++)
        	{
        		keg = resultArray.getJSONObject(i);
        		kegIDs.add(keg.getString("id"));
        	}
        	considerUpdate(kegIDs, Kegs.CONTENT_URI, resolver);
    	}
        
    	return Lists.newArrayList();
    }
    
    private void considerUpdate(List<String> kegIDs, Uri targetDir,
    		ContentResolver resolver) throws HandlerException {
    	
    	final long localUpdated = ParserUtils.queryDirUpdated(targetDir, resolver);
    	
    	String url = SyncService.WORKSHEETS_URL + "/kegs/";
    	
    	for (int i = 0; i < kegIDs.size(); i++)
    	{
    		final HttpGet request = new HttpGet(url+kegIDs.get(i));
            final JsonHandler handler = new RemoteKegHandler();
            mExecutor.execute(request, handler);
    	}
    	
    }
}
