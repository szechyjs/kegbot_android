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
import com.goliathonline.android.kegbot.provider.ScheduleContract;
import com.goliathonline.android.kegbot.ui.tablet.ScheduleMultiPaneActivity;
import com.goliathonline.android.kegbot.ui.tablet.SessionsMultiPaneActivity;
import com.goliathonline.android.kegbot.ui.tablet.VendorsMultiPaneActivity;
import com.goliathonline.android.kegbot.util.AnalyticsUtils;
import com.goliathonline.android.kegbot.util.UIUtils;
import com.goliathonline.android.kegbot.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DashboardFragment extends Fragment {

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(getActivity()).trackEvent(
                "Home Screen Dashboard", "Click", label, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container);

        // Attach event handlers
        root.findViewById(R.id.home_btn_drink).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Drinks");
                if (UIUtils.isHoneycombTablet(getActivity())) {
                    startActivity(new Intent(getActivity(), ScheduleMultiPaneActivity.class));
                } else {
                	final Intent intent = new Intent(Intent.ACTION_VIEW,
                			KegbotContract.Drinks.CONTENT_URI);
                	intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.title_drinks));
                	intent.putExtra(DrinksFragment.EXTRA_NEXT_TYPE,
                			DrinksFragment.NEXT_TYPE_DRINKS);
                	startActivity(intent);
                }
                
            }
            
        });

        root.findViewById(R.id.home_btn_sessions).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Sessions");
                // Launch sessions list
                if (UIUtils.isHoneycombTablet(getActivity())) {
                    startActivity(new Intent(getActivity(), SessionsMultiPaneActivity.class));
                } else {
                    final Intent intent = new Intent(Intent.ACTION_VIEW,
                            KegbotContract.Drinks.CONTENT_URI);
                    intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.title_session_tracks));
                    intent.putExtra(DrinksFragment.EXTRA_NEXT_TYPE,
                            DrinksFragment.NEXT_TYPE_DRINKS);
                    startActivity(intent);
                }

            }
        });

        root.findViewById(R.id.home_btn_starred).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Starred");
                // Launch list of sessions and vendors the user has starred
                startActivity(new Intent(getActivity(), StarredActivity.class));                
            }
        });

        root.findViewById(R.id.home_btn_users).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Sandbox");
                // Launch vendors list
                if (UIUtils.isHoneycombTablet(getActivity())) {
                    startActivity(new Intent(getActivity(), VendorsMultiPaneActivity.class));
                } else {
                    final Intent intent = new Intent(Intent.ACTION_VIEW,
                            ScheduleContract.Tracks.CONTENT_URI);
                    //intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.title_vendor_tracks));
                    //intent.putExtra(TracksFragment.EXTRA_NEXT_TYPE,
                    //        TracksFragment.NEXT_TYPE_VENDORS);
                    //startActivity(intent);
                }
            }
        });

        root.findViewById(R.id.home_btn_keg).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Kegs");
                if (UIUtils.isHoneycombTablet(getActivity())) {
                    startActivity(new Intent(getActivity(), ScheduleMultiPaneActivity.class));
                } else {
                	final Intent intent = new Intent(Intent.ACTION_VIEW,
                			KegbotContract.Kegs.CONTENT_URI);
                	intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.title_drinks));
                	intent.putExtra(DrinksFragment.EXTRA_NEXT_TYPE,
                			KegsFragment.NEXT_TYPE_KEGS);
                	startActivity(intent);
                }
                
            }
        });

        root.findViewById(R.id.home_btn_announcements).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        // splicing in tag streamer
                        fireTrackerEvent("Bulletin");
                        Intent intent = new Intent(getActivity(), BulletinActivity.class);
                        startActivity(intent);
                    }
                });

        return root;
    }
}
