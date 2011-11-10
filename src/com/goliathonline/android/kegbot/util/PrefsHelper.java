package com.goliathonline.android.kegbot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class PrefsHelper {
	
	private static String API_URL = "api_url";
	
	public static boolean hasAPIUrl(final Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.getString(API_URL, "example.com").equals("example.com"))
        	return false;
        else
        	return true;
    }
	
	public static String getAPIUrl(final Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(API_URL, "example.com");
	}
	
	public static void setAPIUrl(final Context context, final String url) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
            	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            	sp.edit().putString(API_URL, url).commit();
                return null;
            }
        }.execute();
    }

}
