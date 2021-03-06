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
import com.goliathonline.android.kegbot.util.ActivityHelper;
import com.goliathonline.android.kegbot.util.AnalyticsUtils;
import com.goliathonline.android.kegbot.util.BitmapUtils;
import com.goliathonline.android.kegbot.util.FractionalTouchDelegate;
import com.goliathonline.android.kegbot.util.NotifyingAsyncQueryHandler;
import com.goliathonline.android.kegbot.R;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A fragment that shows detail information for a sandbox company, including company name,
 * description, product description, logo, etc.
 */
public class VendorDetailFragment extends Fragment implements
        NotifyingAsyncQueryHandler.AsyncQueryListener,
        CompoundButton.OnCheckedChangeListener {
    private Uri mVendorUri;

    private ViewGroup mRootView;
    private TextView mName;
    private CompoundButton mStarred;

    private ImageView mLogo;
    private TextView mUrl;
    private TextView mDesc;
    private TextView mProductDesc;
    
    private String mNameString;

    private NotifyingAsyncQueryHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        mVendorUri = intent.getData();
        if (mVendorUri== null) {
            return;
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mVendorUri == null) {
            return;
        }

        // Start background query to load vendor details
        mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
        mHandler.startQuery(mVendorUri, UsersQuery.PROJECTION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_vendor_detail, null);

        mName = (TextView) mRootView.findViewById(R.id.vendor_name);
        mStarred = (CompoundButton) mRootView.findViewById(R.id.star_button);

        mStarred.setFocusable(true);
        mStarred.setClickable(true);

        // Larger target triggers star toggle
        final View starParent = mRootView.findViewById(R.id.header_vendor);
        FractionalTouchDelegate.setupDelegate(starParent, mStarred, new RectF(0.6f, 0f, 1f, 0.8f));

        mLogo = (ImageView) mRootView.findViewById(R.id.vendor_logo);
        mUrl = (TextView) mRootView.findViewById(R.id.vendor_url);
        mDesc = (TextView) mRootView.findViewById(R.id.vendor_desc);
        mProductDesc = (TextView) mRootView.findViewById(R.id.vendor_product_desc);

        return mRootView;
    }

    /**
     * Build a {@link android.view.View} to be used as a tab indicator, setting the requested string resource as
     * its label.
     *
     * @return View
     */
    private View buildIndicator(int textRes) {
        final TextView indicator = (TextView) getActivity().getLayoutInflater()
                .inflate(R.layout.tab_indicator,
                        (ViewGroup) mRootView.findViewById(android.R.id.tabs), false);
        indicator.setText(textRes);
        return indicator;
    }

    /**
     * {@inheritDoc}
     */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        try {
            if (!cursor.moveToFirst()) {
                return;
            }

            mNameString = cursor.getString(UsersQuery.NAME);
            mName.setText(mNameString);

            // Start background fetch to load vendor logo
            final String logoUrl = cursor.getString(UsersQuery.IMAGE_URL);

            if (!TextUtils.isEmpty(logoUrl)) {
                BitmapUtils.fetchImage(getActivity(), logoUrl, null, null,
                        new BitmapUtils.OnFetchCompleteListener() {
                            public void onFetchComplete(Object cookie, Bitmap result) {
                                if (result == null) {
                                    mLogo.setVisibility(View.GONE);
                                } else {
                                    mLogo.setVisibility(View.VISIBLE);
                                    mLogo.setImageBitmap(result);
                                }
                            }
                        });
            }



            // Assign track details when found
            // TODO: handle vendors not attached to track
            ActivityHelper activityHelper = ((BaseActivity) getActivity()).getActivityHelper();
            activityHelper.setActionBarTitle(cursor.getString(UsersQuery.NAME));

            AnalyticsUtils.getInstance(getActivity()).trackPageView(
                    "/Users/" + mNameString);

        } finally {
            cursor.close();
        }
    }

    /**
     * Handle toggling of starred checkbox.
     */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.map_menu_items, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    /**
     * {@link com.goliathonline.android.kegbot.provider.ScheduleContract.Vendors} query parameters.
     */
    private interface UsersQuery {
        String[] PROJECTION = {
        		KegbotContract.Users.USER_ID,
        		KegbotContract.Users.USER_NAME,
        		KegbotContract.Users.USER_IMAGE_URL,
        };

        int NAME = 1;
        int IMAGE_URL = 2;
    }
}
