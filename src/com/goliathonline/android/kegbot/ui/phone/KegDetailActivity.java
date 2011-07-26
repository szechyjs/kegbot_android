package com.goliathonline.android.kegbot.ui.phone;

import com.goliathonline.android.kegbot.ui.BaseSinglePaneActivity;
import com.goliathonline.android.kegbot.ui.KegDetailFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class KegDetailActivity extends BaseSinglePaneActivity {
    @Override
    protected Fragment onCreatePane() {
        return new KegDetailFragment();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getActivityHelper().setupSubActivity();
    }
}
