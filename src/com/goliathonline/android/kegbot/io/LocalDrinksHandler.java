package com.goliathonline.android.kegbot.io;

import com.goliathonline.android.kegbot.provider.KegbotContract;
import com.goliathonline.android.kegbot.provider.KegbotContract.Drinks;
import com.goliathonline.android.kegbot.provider.KegbotDatabase;
import com.goliathonline.android.kegbot.util.Lists;
import com.goliathonline.android.kegbot.util.ParserUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;


public class LocalDrinksHandler extends JsonHandler {

    public LocalDrinksHandler() {
        super(KegbotContract.CONTENT_AUTHORITY);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONObject parser, ContentResolver resolver)
            throws JSONException, IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        if (parser.has("result"))
        {
        	JSONObject resultObject = parser.getJSONObject("result");
        	JSONArray resultArray = resultObject.getJSONArray("drinks");

        	for (int a = 0; a < resultArray.length(); a++) 
        	{
				JSONObject drinkItem = resultArray.getJSONObject(a);
				parseDrink(drinkItem, batch, resolver);
			}
        }
        return batch;
    }

    private static void parseDrink(JSONObject parser,
            ArrayList<ContentProviderOperation> batch, ContentResolver resolver)
            throws JSONException, IOException {
        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Drinks.CONTENT_URI);
        builder.withValue(Drinks.UPDATED, 0);

        long startTime = -1;
        long endTime = -1;
        String title = null;
        String drinkId = null;
        String kegId = null;
        String userId = null;

        drinkId = parser.getString(Tags.ID);
        userId = parser.getString(Tags.USER);
        
        if (drinkId == null) {
            drinkId = Drinks.generateDrinkId(title);
        }

        builder.withValue(Drinks.DRINK_ID, drinkId);

        // Use empty strings to make sure SQLite search trigger has valid data
        // for updating search index.
        //builder.withValue(Drinks.SESSION_ABSTRACT, "");

        // Propagate any existing starred value
        final Uri sessionUri = Drinks.buildDrinkUri(drinkId);
        final int starred = queryDrinkStarred(sessionUri, resolver);
        if (starred != -1) {
            builder.withValue(Drinks.DRINK_STARRED, starred);
        }

        batch.add(builder.build());

    }

    public static int queryDrinkStarred(Uri uri, ContentResolver resolver) {
        final String[] projection = { Drinks.DRINK_STARRED };
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                return -1;
            }
        } finally {
            cursor.close();
        }
    }

    interface Tags {
        String ID = "id";
        String USER = "user_id";
        String START = "start";
        String END = "end";
        String TITLE = "title";
    }
}
