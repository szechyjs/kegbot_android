package com.goliathonline.android.kegbot.ui.phone;

import com.goliathonline.android.kegbot.ui.BaseSinglePaneActivity;
import com.goliathonline.android.kegbot.ui.KegsFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class KegsActivity extends BaseSinglePaneActivity {
    @Override
    protected Fragment onCreatePane() {
        return new KegsFragment();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getActivityHelper().setupSubActivity();
    }
}
