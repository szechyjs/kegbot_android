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
import com.goliathonline.android.kegbot.R;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

/**
 * A {@link android.widget.CursorAdapter} that renders a {@link TracksQuery}.
 */
public class TracksAdapter extends CursorAdapter {
    private static final int ALL_ITEM_ID = Integer.MAX_VALUE;

    private Activity mActivity;
    private boolean mHasAllItem;
    private int mPositionDisplacement;
    private boolean mIsSessions = true;

    public TracksAdapter(Activity activity) {
        super(activity, null);
        mActivity = activity;
    }

    public void setHasAllItem(boolean hasAllItem) {
        mHasAllItem = hasAllItem;
        mPositionDisplacement = mHasAllItem ? 1 : 0;
    }

    public void setIsSessions(boolean isSessions) {
        mIsSessions = isSessions;
    }

    @Override
    public int getCount() {
        return super.getCount() + mPositionDisplacement;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mHasAllItem && position == 0) {
            if (convertView == null) {
                convertView = mActivity.getLayoutInflater().inflate(
                        R.layout.list_item_track, parent, false);
            }

            // Custom binding for the first item
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(
                    "(" + mActivity.getResources().getString(mIsSessions
                            ? R.string.all_sessions_title
                            : R.string.all_sandbox_title)
                            + ")");
            convertView.findViewById(android.R.id.icon1).setVisibility(View.INVISIBLE);

            return convertView;
        }
        return super.getView(position - mPositionDisplacement, convertView, parent);
    }

    @Override
    public Object getItem(int position) {
        if (mHasAllItem && position == 0) {
            return null;
        }
        return super.getItem(position - mPositionDisplacement);
    }

    @Override
    public long getItemId(int position) {
        if (mHasAllItem && position == 0) {
            return ALL_ITEM_ID;
        }
        return super.getItemId(position - mPositionDisplacement);
    }

    @Override
    public boolean isEnabled(int position) {
        if (mHasAllItem && position == 0) {
            return true;
        }
        return super.isEnabled(position - mPositionDisplacement);
    }

    @Override
    public int getViewTypeCount() {
        // Add an item type for the "All" view.
        return super.getViewTypeCount() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (mHasAllItem && position == 0) {
            return getViewTypeCount() - 1;
        }
        return super.getItemViewType(position - mPositionDisplacement);
    }

    /** {@inheritDoc} */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mActivity.getLayoutInflater().inflate(R.layout.list_item_track, parent,
                false);
    }

    /** {@inheritDoc} */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(cursor.getString(TracksQuery.KEG_NAME));

    }

    /** {@link com.goliathonline.android.kegbot.provider.ScheduleContract.Tracks} query parameters. */
    public interface TracksQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                KegbotContract.Kegs.KEG_ID,
                KegbotContract.Kegs.KEG_NAME,
        };

        String[] PROJECTION_WITH_DRINKS_COUNT = {
                BaseColumns._ID,
                KegbotContract.Kegs.KEG_ID,
                KegbotContract.Kegs.KEG_NAME,
                KegbotContract.Kegs.DRINKS_COUNT,
        };

        int _ID = 0;
        int KEG_ID = 1;
        int KEG_NAME = 2;
    }
}
