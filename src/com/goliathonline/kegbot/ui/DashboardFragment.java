package com.goliathonline.kegbot.ui;

import com.goliathonline.kegbot.R;
import com.goliathonline.kegbot.util.AnalyticsUtils;
import com.goliathonline.kegbot.util.UIUtils;

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
        root.findViewById(R.id.home_btn_drinks).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Drinks");
                if (UIUtils.isHoneycombTablet(getActivity())) {
                    //startActivity(new Intent(getActivity(), ScheduleMultiPaneActivity.class));
                } else {
                    //startActivity(new Intent(getActivity(), ScheduleActivity.class));
                }
                
            }
            
        });

        root.findViewById(R.id.home_btn_sessions).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Sessions");
                // Launch sessions list
                if (UIUtils.isHoneycombTablet(getActivity())) {
                    //startActivity(new Intent(getActivity(), SessionsMultiPaneActivity.class));
                } else {
                    //final Intent intent = new Intent(Intent.ACTION_VIEW,
                    //        ScheduleContract.Tracks.CONTENT_URI);
                    //intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.title_session_tracks));
                    //intent.putExtra(TracksFragment.EXTRA_NEXT_TYPE,
                    //        TracksFragment.NEXT_TYPE_SESSIONS);
                    //startActivity(intent);
                }

            }
        });

        root.findViewById(R.id.home_btn_starred).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Starred");
                // Launch list of sessions and vendors the user has starred
                //startActivity(new Intent(getActivity(), StarredActivity.class));                
            }
        });

        root.findViewById(R.id.home_btn_users).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Users");
                // Launch vendors list
                if (UIUtils.isHoneycombTablet(getActivity())) {
                    //startActivity(new Intent(getActivity(), VendorsMultiPaneActivity.class));
                } else {
                    //final Intent intent = new Intent(Intent.ACTION_VIEW,
                    //        ScheduleContract.Tracks.CONTENT_URI);
                    //intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.title_vendor_tracks));
                    //intent.putExtra(TracksFragment.EXTRA_NEXT_TYPE,
                    //        TracksFragment.NEXT_TYPE_VENDORS);
                    //startActivity(intent);
                }
            }
        });

        root.findViewById(R.id.home_btn_kegs).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Launch map of conference venue
                fireTrackerEvent("Kegs");
                //startActivity(new Intent(getActivity(),
                //        UIUtils.getMapActivityClass(getActivity())));
            }
        });

        root.findViewById(R.id.home_btn_announcements).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        // splicing in tag streamer
                        fireTrackerEvent("Bulletin");
                        //Intent intent = new Intent(getActivity(), BulletinActivity.class);
                        //startActivity(intent);
                    }
                });

        return root;
    }
}
