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

package com.goliathonline.android.kegbot.ui;

import com.goliathonline.android.kegbot.io.ImageLoader;
import com.goliathonline.android.kegbot.provider.KegbotContract;
import com.goliathonline.android.kegbot.util.BitmapUtils;
import com.goliathonline.android.kegbot.util.NotifyingAsyncQueryHandler;
import com.goliathonline.android.kegbot.util.UnitUtils;
import com.goliathonline.android.kegbot.R;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * A {@link ListFragment} showing a list of sessions.
 */
public class DrinksFragment extends ListFragment implements
        NotifyingAsyncQueryHandler.AsyncQueryListener {
	private static final String TAG = "DrinksFragment";


	public static final String EXTRA_NEXT_TYPE = "com.goliathonline.android.kegbot.extra.NEXT_TYPE";
	public static final String NEXT_TYPE_DRINKS = "drinks";
    public static final String EXTRA_SCHEDULE_TIME_STRING =
            "com.goliathonline.android.kegbot.extra.SCHEDULE_TIME_STRING";

    private static final String STATE_CHECKED_POSITION = "checkedPosition";

    private Uri mTrackUri;
    private Cursor mCursor;
    private CursorAdapter mAdapter;
    private int mCheckedPosition = -1;
    private boolean mHasSetEmptyText = false;

    private NotifyingAsyncQueryHandler mHandler;
    private Handler mMessageQueueHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
        reloadFromArguments(getArguments());
    }

    public void reloadFromArguments(Bundle arguments) {
        // Teardown from previous arguments
        if (mCursor != null) {
            getActivity().stopManagingCursor(mCursor);
            mCursor = null;
        }

        mCheckedPosition = -1;
        setListAdapter(null);

        mHandler.cancelOperation(DrinksQuery._TOKEN);
        mHandler.cancelOperation(KegQuery._TOKEN);

        // Load new arguments
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(arguments);
        final Uri drinksUri = intent.getData();
        final int drinkQueryToken;

        if (drinksUri == null) {
            return;
        }

        String[] projection;
        mAdapter = new DrinksAdapter(getActivity());
        projection = DrinksQuery.PROJECTION;
        drinkQueryToken = DrinksQuery._TOKEN;


        setListAdapter(mAdapter);

        // Start background query to load drinks
        mHandler.startQuery(drinkQueryToken, null, drinksUri, projection, null, null,
                KegbotContract.Drinks.DEFAULT_SORT);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if (savedInstanceState != null) {
            mCheckedPosition = savedInstanceState.getInt(STATE_CHECKED_POSITION, -1);
        }

        if (!mHasSetEmptyText) {
            // Could be a bug, but calling this twice makes it become visible when it shouldn't
            // be visible.
            setEmptyText(getString(R.string.empty_drinks));
            mHasSetEmptyText = true;
        }
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        if (token == DrinksQuery._TOKEN)  {
            onSessionOrSearchQueryComplete(cursor);
        } else if (token == KegQuery._TOKEN) {
            onTrackQueryComplete(cursor);
        } else {
            Log.d("SessionsFragment/onQueryComplete", "Query complete, Not Actionable: " + token);
            cursor.close();
        }
    }

    /**
     * Handle {@link SessionsQuery} {@link Cursor}.
     */
    private void onSessionOrSearchQueryComplete(Cursor cursor) {
        mCursor = cursor;
        getActivity().startManagingCursor(mCursor);
        mAdapter.changeCursor(mCursor);
        if (mCheckedPosition >= 0 && getView() != null) {
            getListView().setItemChecked(mCheckedPosition, true);
        }
    }

    /**
     * Handle {@link TracksQuery} {@link Cursor}.
     */
    private void onTrackQueryComplete(Cursor cursor) {
        try {
            if (!cursor.moveToFirst()) {
                return;
            }

            // Use found track to build title-bar
            //ActivityHelper activityHelper = ((BaseActivity) getActivity()).getActivityHelper();
            //String trackName = cursor.getString(KegsQuery.TRACK_NAME);
            //activityHelper.setActionBarTitle(trackName);
            //activityHelper.setActionBarColor(cursor.getInt(KegsQuery.TRACK_COLOR));

            //AnalyticsUtils.getInstance(getActivity()).trackPageView("/Tracks/" + trackName);
        } finally {
            cursor.close();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMessageQueueHandler.post(mRefreshSessionsRunnable);
        getActivity().getContentResolver().registerContentObserver(
                KegbotContract.Drinks.CONTENT_URI, true, mSessionChangesObserver);
        if (mCursor != null) {
            mCursor.requery();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMessageQueueHandler.removeCallbacks(mRefreshSessionsRunnable);
        getActivity().getContentResolver().unregisterContentObserver(mSessionChangesObserver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CHECKED_POSITION, mCheckedPosition);
    }

    /** {@inheritDoc} */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Launch viewer for specific session, passing along any track knowledge
        // that should influence the title-bar.
        final Cursor cursor = (Cursor)mAdapter.getItem(position);
        final String drinkId = cursor.getString(cursor.getColumnIndex(
                KegbotContract.Drinks.DRINK_ID));
        final Uri drinkUri = KegbotContract.Drinks.buildDrinkUri(drinkId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, drinkUri);
        intent.putExtra(DrinkDetailFragment.EXTRA_TRACK, mTrackUri);
        ((BaseActivity) getActivity()).openActivityOrFragment(intent);

        getListView().setItemChecked(position, true);
        mCheckedPosition = position;
    }

    public void clearCheckedPosition() {
        if (mCheckedPosition >= 0) {
            getListView().setItemChecked(mCheckedPosition, false);
            mCheckedPosition = -1;
        }
    }

    /**
     * {@link CursorAdapter} that renders a {@link SessionsQuery}.
     */
    private class DrinksAdapter extends CursorAdapter {
    	public ImageLoader imageLoader; 
    	
        public DrinksAdapter(Context context) {
            super(context, null);
            imageLoader=new ImageLoader(getActivity().getApplicationContext());
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getActivity().getLayoutInflater().inflate(R.layout.list_item_drink, parent,
                    false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {        	
            final TextView titleView = (TextView) view.findViewById(R.id.drink_title);
            final TextView subtitleView = (TextView) view.findViewById(R.id.drink_subtitle);
            final ImageView userImgView = (ImageView) view.findViewById(R.id.user_image);

            final String drink_id = cursor.getString(DrinksQuery.DRINK_ID);
            final String keg_id = cursor.getString(DrinksQuery.KEG_ID);
            String user_id = cursor.getString(DrinksQuery.USER_ID);
            
            final String vol_oz = UnitUtils.mlToOz(Double.toString(cursor.getDouble(DrinksQuery.VOLUME)));
            
            // Set Title - User Name
            if (TextUtils.isEmpty(user_id))
            	user_id = "guest";
           	titleView.setText(user_id + " - " + vol_oz + "oz");
            
            // Set Subtitle - Beer Name
            final Uri kegUri = KegbotContract.Kegs.buildKegUri(keg_id);
            Cursor kegCursor = getActivity().getContentResolver().query(kegUri, KegQuery.PROJECTION, null, null, null);
            kegCursor.moveToFirst();
            subtitleView.setText(kegCursor.getString(KegQuery.KEG_NAME));
            kegCursor.close();

            // Set Starred
            final boolean starred = cursor.getInt(DrinksQuery.STARRED) != 0;
            view.findViewById(R.id.star_button).setVisibility(
                    starred ? View.VISIBLE : View.INVISIBLE);
            
            // Set User Image
            if (user_id != null)
            {
	            final Uri userUri = KegbotContract.Users.buildUserUri(user_id);
	        	
	        	Cursor userCursor = getActivity().getContentResolver().query(userUri, UserQuery.PROJECTION, null, null, null);
	        	userCursor.moveToFirst();
	            final String userImageUrl = userCursor.getString(UserQuery.USER_IMAGE_URL);
	            
	            if (!TextUtils.isEmpty(userImageUrl)) {
	            	
	            	imageLoader.DisplayImage(userImageUrl, getActivity(), userImgView);
	            }
	            userCursor.close();
            }
            
        }
    }
    private ContentObserver mSessionChangesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (mCursor != null) {
                mCursor.requery();
            }
        }
    };

    private Runnable mRefreshSessionsRunnable = new Runnable() {
        public void run() {
            if (mAdapter != null) {
                // This is used to refresh session title colors.
                mAdapter.notifyDataSetChanged();
            }

            // Check again on the next quarter hour, with some padding to account for network
            // time differences.
            long nextQuarterHour = (SystemClock.uptimeMillis() / 900000 + 1) * 900000 + 5000;
            mMessageQueueHandler.postAtTime(mRefreshSessionsRunnable, nextQuarterHour);
        }
    };

    /**
     * {@link com.goliathonline.android.kegbot.provider.KegbotContract.Drinks} query parameters.
     */
    private interface DrinksQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                KegbotContract.Drinks.DRINK_ID,
                KegbotContract.Drinks.SESSION_ID,
                KegbotContract.Drinks.STATUS,
                KegbotContract.Drinks.USER_ID,
                KegbotContract.Drinks.KEG_ID,
                KegbotContract.Drinks.VOLUME,
                KegbotContract.Drinks.DRINK_STARRED,
        };

        int _ID = 0;
        int DRINK_ID = 1;
        int SESSION_ID = 2;
        int STATUS = 3;
        int USER_ID = 4;
        int KEG_ID = 5;
        int VOLUME = 6;
        int STARRED = 7;
    }

    /**
     * {@link com.goliathonline.android.kegbot.provider.KegbotContract.Kegs} query parameters.
     */
    private interface KegQuery {
        int _TOKEN = 0x2;

        String[] PROJECTION = {
                KegbotContract.Kegs.KEG_ID,
                KegbotContract.Kegs.KEG_NAME,
        };

        int KEG_ID = 0;
        int KEG_NAME = 1;
    }
    
    private interface UserQuery {
        int _TOKEN = 0x3;

        String[] PROJECTION = {
        		KegbotContract.Users.USER_ID,
        		KegbotContract.Users.USER_IMAGE_URL,
        };

        int USER_ID = 0;
        int USER_IMAGE_URL = 1;
    }
}
