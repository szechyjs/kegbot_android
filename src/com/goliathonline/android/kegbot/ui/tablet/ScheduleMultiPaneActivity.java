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

package com.goliathonline.android.kegbot.ui.tablet;

import com.goliathonline.android.kegbot.ui.BaseMultiPaneActivity;
import com.goliathonline.android.kegbot.ui.DrinkDetailFragment;
import com.goliathonline.android.kegbot.ui.DrinksFragment;
import com.goliathonline.android.kegbot.ui.phone.DrinkDetailActivity;
import com.goliathonline.android.kegbot.ui.phone.DrinksActivity;
import com.goliathonline.android.kegbot.R;

import android.app.FragmentBreadCrumbs;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;

/**
 * A multi-pane activity, where the primary navigation pane is a
 * {@link com.goliathonline.android.kegbot.ui.ScheduleFragment}, that
 * shows {@link SessionsFragment} and {@link SessionDetailFragment} as popups.
 *
 * This activity requires API level 11 or greater because of its use of {@link FragmentBreadCrumbs}.
 */
public class ScheduleMultiPaneActivity extends BaseMultiPaneActivity implements
        View.OnClickListener, FragmentManager.OnBackStackChangedListener {

    private FragmentManager mFragmentManager;
    private FragmentBreadCrumbs mFragmentBreadCrumbs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        mFragmentManager = getSupportFragmentManager();
        mFragmentBreadCrumbs = (FragmentBreadCrumbs) findViewById(R.id.breadcrumbs);
        mFragmentBreadCrumbs.setActivity(this);
        mFragmentManager.addOnBackStackChangedListener(this);

        updateBreadCrumb();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getActivityHelper().setupSubActivity();

        ViewGroup detailContainer = (ViewGroup)
                findViewById(R.id.fragment_container_schedule_detail);
        if (detailContainer != null && detailContainer.getChildCount() > 0) {
            findViewById(R.id.fragment_container_schedule_detail).setBackgroundColor(0);
        }
    }

    @Override
    public FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(String activityClassName) {
        if (DrinksActivity.class.getName().equals(activityClassName)) {
            getSupportFragmentManager().popBackStack();
            findViewById(R.id.fragment_container_schedule_detail).setBackgroundColor(0);
            return new FragmentReplaceInfo(
                    DrinksFragment.class,
                    "sessions",
                    R.id.fragment_container_schedule_detail);
        } else if (DrinkDetailActivity.class.getName().equals(activityClassName)) {
            findViewById(R.id.fragment_container_schedule_detail).setBackgroundColor(0);
            return new FragmentReplaceInfo(
                    DrinkDetailFragment.class,
                    "session_detail",
                    R.id.fragment_container_schedule_detail);
        }
        return null;
    }

    @Override
    protected void onBeforeCommitReplaceFragment(FragmentManager fm, FragmentTransaction ft,
            Fragment fragment) {
        super.onBeforeCommitReplaceFragment(fm, ft, fragment);
        if (fragment instanceof DrinkDetailFragment) {
            ft.addToBackStack(null);
        } else if (fragment instanceof DrinksFragment) {
            fm.popBackStack();
        }
        updateBreadCrumb();
    }

    /**
     * Handler for the breadcrumb parent.
     */
    public void onClick(View view) {
        mFragmentManager.popBackStack();
    }

    public void onBackStackChanged() {
        updateBreadCrumb();
    }

    public void updateBreadCrumb() {
        final String title = getString(R.string.title_drinks);
        final String detailTitle = getString(R.string.title_drink_detail);

        if (mFragmentManager.getBackStackEntryCount() >= 1) {
            mFragmentBreadCrumbs.setParentTitle(title, title, this);
            mFragmentBreadCrumbs.setTitle(detailTitle, detailTitle);
        } else {
            mFragmentBreadCrumbs.setParentTitle(null, null, null);
            mFragmentBreadCrumbs.setTitle(title, title);
        }
    }
}
