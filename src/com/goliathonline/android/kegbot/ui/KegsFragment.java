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

import com.goliathonline.android.kegbot.provider.KegbotContract;
import com.goliathonline.android.kegbot.util.BitmapUtils;
import com.goliathonline.android.kegbot.util.NotifyingAsyncQueryHandler;
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
public class KegsFragment extends ListFragment implements
        NotifyingAsyncQueryHandler.AsyncQueryListener {
	private static final String TAG = "KegsFragment";


	public static final String EXTRA_NEXT_TYPE = "com.goliathonline.android.kegbot.extra.NEXT_TYPE";
	public static final String NEXT_TYPE_KEGS = "kegs";
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

        mHandler.cancelOperation(KegQuery._TOKEN);

        // Load new arguments
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(arguments);
        final Uri kegsUri = intent.getData();
        final int kegQueryToken;

        if (kegsUri == null) {
            return;
        }

        String[] projection;
        mAdapter = new KegsAdapter(getActivity());
        projection = KegQuery.PROJECTION;
        kegQueryToken = KegQuery._TOKEN;


        setListAdapter(mAdapter);

        // Start background query to load drinks
        mHandler.startQuery(kegQueryToken, null, kegsUri, projection, null, null,
                KegbotContract.Kegs.DEFAULT_SORT);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if (!mHasSetEmptyText) {
            // Could be a bug, but calling this twice makes it become visible when it shouldn't
            // be visible.
            setEmptyText(getString(R.string.empty_sessions));
            mHasSetEmptyText = true;
        }
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        if (token == KegQuery._TOKEN)  {
            onKegQueryComplete(cursor);
        } else {
            Log.d("Kegs/onQueryComplete", "Query complete, Not Actionable: " + token);
            cursor.close();
        }
    }

    /**
     * Handle {@link SessionsQuery} {@link Cursor}.
     */
    private void onKegQueryComplete(Cursor cursor) {
        mCursor = cursor;
        getActivity().startManagingCursor(mCursor);
        mAdapter.changeCursor(mCursor);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMessageQueueHandler.post(mRefreshSessionsRunnable);
        getActivity().getContentResolver().registerContentObserver(
                KegbotContract.Kegs.CONTENT_URI, true, mSessionChangesObserver);
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
        final String kegId = cursor.getString(cursor.getColumnIndex(
                KegbotContract.Kegs.KEG_ID));
        final Uri kegUri = KegbotContract.Kegs.buildKegUri(kegId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, kegUri);
        intent.putExtra(KegDetailFragment.EXTRA_TRACK, mTrackUri);
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
    private class KegsAdapter extends CursorAdapter {
        public KegsAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getActivity().getLayoutInflater().inflate(R.layout.list_item_keg, parent,
                    false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {        	
            final TextView titleView = (TextView) view.findViewById(R.id.keg_title);
            final TextView subtitleView = (TextView) view.findViewById(R.id.keg_subtitle);
            final ImageView userImgView = (ImageView) view.findViewById(R.id.keg_image);

            final String keg_id = cursor.getString(KegQuery.KEG_ID);
            
            final String keg_name = cursor.getString(KegQuery.KEG_NAME);
            
           	titleView.setText(keg_name);
            
            subtitleView.setText(keg_id);
            
            // Set Starred
            final boolean starred = cursor.getInt(KegQuery.STARRED) != 0;
            view.findViewById(R.id.star_button).setVisibility(
                    starred ? View.VISIBLE : View.INVISIBLE);

            final String kegImageUrl = cursor.getString(KegQuery.IMAGE_URL);
	            
            if (!TextUtils.isEmpty(kegImageUrl)) {
            	BitmapUtils.fetchImage(getActivity(), kegImageUrl, null, null,
	    			   new BitmapUtils.OnFetchCompleteListener() {
	    		   			public void onFetchComplete(Object cookie, Bitmap result) {
	    		   				if (result != null) {
	    		   					userImgView.setImageBitmap(result);
	    		   				}
	    		   			}
	    	   			});
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
     * {@link com.goliathonline.android.kegbot.provider.KegbotContract.Kegs} query parameters.
     */
    private interface KegQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
        		BaseColumns._ID,
                KegbotContract.Kegs.KEG_ID,
                KegbotContract.Kegs.KEG_NAME,
                KegbotContract.Kegs.IMAGE_URL,
                KegbotContract.Kegs.KEG_STARRED,
        };

        int _ID = 0;
        int KEG_ID = 1;
        int KEG_NAME = 2;
        int IMAGE_URL = 3;
        int STARRED = 4;
    }
}
    
