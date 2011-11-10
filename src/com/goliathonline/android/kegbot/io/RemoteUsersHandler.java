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

import java.util.HashSet;
import java.util.Set;

import com.goliathonline.android.kegbot.io.JsonHandler.HandlerException;
import com.goliathonline.android.kegbot.provider.KegbotContract;
import com.goliathonline.android.kegbot.provider.KegbotContract.Kegs;
import com.goliathonline.android.kegbot.service.SyncService;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

/**
 * Handle a remote {@link JSONObject} that defines a set of {@link Kegs}
 * entries.
 */
public class RemoteUsersHandler {
    private static final String TAG = "UsersHandler";
    private Set<String> mUsers = new HashSet<String>();
    private Cursor mCursor;

    public RemoteUsersHandler(ContentResolver resolver) {
    	final Uri usersUri = KegbotContract.Drinks.buildUsersDirUri();
    	
    	mCursor = resolver.query(usersUri, UsersQuery.PROJECTION, null, null, null);
    	
    	while(mCursor.moveToNext())
		{
			final String userId = mCursor.getString(UsersQuery.USER_ID);
			mUsers.add(userId);
		}
    	mCursor.close();
    }
    
    public void updateUsers(RemoteExecutor executor) throws HandlerException
    {
    	for (String user : mUsers)
    	{
    		final String url = SyncService.getApiUrl() + "/users/" + user;
    		final HttpGet request = new HttpGet(url);
            final JsonHandler handler = new RemoteUserHandler();
            executor.execute(request, handler);
    	}
    }
	
	private interface UsersQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
        		BaseColumns._ID,
        		KegbotContract.Drinks.DRINK_ID,
        		KegbotContract.Users.USER_ID,
        };

        int _ID = 0;
        int DRINK_ID = 1;
        int USER_ID = 2;
    }
}
