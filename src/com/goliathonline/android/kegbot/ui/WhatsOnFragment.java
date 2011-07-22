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
import com.goliathonline.android.kegbot.util.AnalyticsUtils;
import com.goliathonline.android.kegbot.util.BitmapUtils;
import com.goliathonline.android.kegbot.util.NotifyingAsyncQueryHandler;
import com.goliathonline.android.kegbot.util.UIUtils;
import com.goliathonline.android.kegbot.util.UnitUtils;
import com.goliathonline.android.kegbot.R;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A fragment used in {@link HomeActivity} that shows either a countdown, 'now playing' link to
 * current sessions, or 'thank you' text, at different times (before/during/after the conference).
 * It also shows a 'Realtime Search' button on phones, as a replacement for the
 * {@link TagStreamFragment} that is visible on tablets on the home screen.
 */
public class WhatsOnFragment extends Fragment  implements
		NotifyingAsyncQueryHandler.AsyncQueryListener {

    private ViewGroup mRootView;
    private Uri mTapUri;
    private NotifyingAsyncQueryHandler mHandler;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mTapUri = KegbotContract.Taps.CONTENT_URI;
        
        mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
        mHandler.startQuery(TapsQuery._TOKEN, mTapUri, TapsQuery.PROJECTION);
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_whats_on, container);
        refresh();
        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void refresh() {
        mRootView.removeAllViews();

        if (!UIUtils.isHoneycombTablet(getActivity())) {
            View separator = new View(getActivity());
            separator.setLayoutParams(
                    new ViewGroup.LayoutParams(1, ViewGroup.LayoutParams.FILL_PARENT));
            separator.setBackgroundResource(R.drawable.whats_on_separator);
            mRootView.addView(separator);

            View view = getActivity().getLayoutInflater().inflate(
                    R.layout.whats_on_stream, mRootView, false);
            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    AnalyticsUtils.getInstance(getActivity()).trackEvent(
                            "Home Screen Dashboard", "Click", "Realtime Stream", 0);
                    Intent intent = new Intent(getActivity(), TagStreamActivity.class);
                    startActivity(intent);
                }
            });
            mRootView.addView(view);
        }
    }

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		if (getActivity() == null) {
            return;
        }

        if (token == TapsQuery._TOKEN) {
            onTapQueryComplete(cursor);
        } else {
        	if (cursor != null)
        		cursor.close();
        }
	}
	
    private void onTapQueryComplete(Cursor cursor) {
    	if (cursor.getCount() == 0)
    		return;
    	
    	final LayoutInflater inflater = getActivity().getLayoutInflater();
    	
    	final View onTapView = inflater.inflate(R.layout.whats_on_tap, mRootView, false);
    	final TextView onTapTitleView = (TextView) onTapView.findViewById(R.id.on_tap);
    	final TextView onTapSubTitleView = (TextView) onTapView.findViewById(R.id.whats_on_subtitle);
    	final ProgressBar kegProgress = (ProgressBar) onTapView.findViewById(R.id.kegProgress);
    	final ImageView tapImage = (ImageView) onTapView.findViewById(R.id.tap_image);

    	
    	
    	cursor.moveToFirst();
    	final String tapImageUrl = cursor.getString(TapsQuery.IMAGE_URL);
    	
    	if (!TextUtils.isEmpty(tapImageUrl)) {
            BitmapUtils.fetchImage(getActivity(), tapImageUrl, null, null,
                    new BitmapUtils.OnFetchCompleteListener() {
                        public void onFetchComplete(Object cookie, Bitmap result) {
                            if (result != null) {
                            	tapImage.setImageBitmap(result);
                            }
                        }
                    });
        }
    	
    	onTapTitleView.setText(cursor.getString(TapsQuery.BEER_NAME));
    	
    	final Double mlRemain = cursor.getDouble(TapsQuery.VOL_REMAIN);
    	final String pintsRemain = UnitUtils.mlToPint(Double.toString(mlRemain));
    	final Double mlTotal = cursor.getDouble(TapsQuery.VOL_SIZE);
    	final Double mlPoured = mlTotal - mlRemain;
    	final String pintsPoured = UnitUtils.mlToPint(Double.toString(mlPoured));
    	
    	
    	onTapSubTitleView.setText("Pints Poured: " + pintsPoured + " (" + pintsRemain + " remain)");

    	kegProgress.setProgressDrawable(getResources().getDrawable(R.drawable.progress));
    	kegProgress.setProgress((int)cursor.getDouble(TapsQuery.PERCENT_FULL));

    	mRootView.addView(onTapView);
		
	}

	private interface TapsQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                KegbotContract.Taps.BEER_NAME,
                KegbotContract.Taps.PERCENT_FULL,
                KegbotContract.Taps.VOL_REMAIN,
                KegbotContract.Taps.VOL_SIZE,
                KegbotContract.Taps.LAST_TEMP,
                KegbotContract.Taps.IMAGE_URL,
        };

        int BEER_NAME = 0;
        int PERCENT_FULL = 1;
        int VOL_REMAIN = 2;
        int VOL_SIZE = 3;
        int LAST_TEMP = 4;
        int IMAGE_URL = 5;
    }
}

