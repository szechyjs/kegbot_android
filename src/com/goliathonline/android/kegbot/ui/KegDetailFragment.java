package com.goliathonline.android.kegbot.ui;

import com.goliathonline.android.kegbot.io.ImageLoader;
import com.goliathonline.android.kegbot.provider.KegbotContract;
import com.goliathonline.android.kegbot.util.ActivityHelper;
import com.goliathonline.android.kegbot.util.AnalyticsUtils;
import com.goliathonline.android.kegbot.util.CatchNotesHelper;
import com.goliathonline.android.kegbot.util.FractionalTouchDelegate;
import com.goliathonline.android.kegbot.util.NotifyingAsyncQueryHandler;
import com.goliathonline.android.kegbot.util.UIUtils;
import com.goliathonline.android.kegbot.util.UnitUtils;
import com.goliathonline.android.kegbot.R;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

/**
 * A fragment that shows detail information for a session, including session title, abstract,
 * time information, speaker photos and bios, etc.
 */
public class KegDetailFragment extends Fragment implements
        NotifyingAsyncQueryHandler.AsyncQueryListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "KegDetailFragment";

    /**
     * Since sessions can belong tracks, the parent activity can send this extra specifying a
     * track URI that should be used for coloring the title-bar.
     */
    public static final String EXTRA_TRACK = "com.goliathonline.android.kegbot.extra.KEG";

    private static final String TAG_SUMMARY = "summary";
    private static final String TAG_KEG = "keg";
    private static final String TAG_DRINKS = "drinks";

    private static StyleSpan sBoldSpan = new StyleSpan(Typeface.BOLD);

    private String mKegId;
    private Uri mKegUri;

    private String mTitleString;
    private String mHashtag;
    private String mUrl;
    private TextView mTagDisplay;
    private String mRoomId;
    private ImageView kegImage;

    private ViewGroup mRootView;
    private TabHost mTabHost;
    private TextView mTitle;
    private TextView mSubtitle;
    private CompoundButton mStarred;
    
    private TextView mAbstract;
    private TextView mRequirements;
    
    public ImageLoader imageLoader;

    private NotifyingAsyncQueryHandler mHandler;

    private boolean mSessionCursor = false;
    private boolean mSpeakersCursor = false;
    private boolean mHasSummaryContent = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        mKegUri = intent.getData();
        mKegId = KegbotContract.Kegs.getKegId(mKegUri);
        
        mKegUri = resolveKegUri(intent);
        
        imageLoader = new ImageLoader(getActivity().getApplicationContext());

        if (mKegUri == null) {
            return;
        }

        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        //updateNotesTab();

        // Start listening for time updates to adjust "now" bar. TIME_TICK is
        // triggered once per minute, which is how we move the bar over time.
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mPackageChangesReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mPackageChangesReceiver);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mKegUri == null) {
            return;
        }

        // Start background queries to load session and track details
        final Uri drinksUri = KegbotContract.Kegs.buildDrinksUri(mKegId);

        mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
        mHandler.startQuery(KegsQuery._TOKEN, mKegUri, KegsQuery.PROJECTION);
        mHandler.startQuery(DrinksQuery._TOKEN, drinksUri, DrinksQuery.PROJECTION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_keg_detail, null);
        mTabHost = (TabHost) mRootView.findViewById(android.R.id.tabhost);
        mTabHost.setup();

        kegImage = (ImageView) mRootView.findViewById(R.id.keg_image);
        mTitle = (TextView) mRootView.findViewById(R.id.keg_title);
        mSubtitle = (TextView) mRootView.findViewById(R.id.keg_subtitle);
        mStarred = (CompoundButton) mRootView.findViewById(R.id.star_button);

        mStarred.setFocusable(true);
        mStarred.setClickable(true);

        // Larger target triggers star toggle
        final View starParent = mRootView.findViewById(R.id.header_keg);
        FractionalTouchDelegate.setupDelegate(starParent, mStarred, new RectF(0.6f, 0f, 1f, 0.8f));

        mAbstract = (TextView) mRootView.findViewById(R.id.session_abstract);
        mRequirements = (TextView) mRootView.findViewById(R.id.keg_volume_details);

        setupSummaryTab();
        setupNotesTab();
        setupLinksTab();

        return mRootView;
    }

    /**
     * Build and add "summary" tab.
     */
    private void setupSummaryTab() {
        // Summary content comes from existing layout
        mTabHost.addTab(mTabHost.newTabSpec(TAG_SUMMARY)
                .setIndicator(buildIndicator(R.string.drink_summary))
                .setContent(R.id.tab_keg_summary));
    }

    /**
     * Build a {@link View} to be used as a tab indicator, setting the requested string resource as
     * its label.
     *
     * @param textRes
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
     * Derive {@link com.goliathonline.android.kegbot.provider.KegbotContract.Kegs#CONTENT_ITEM_TYPE}
     * {@link Uri} based on incoming {@link Intent}, using
     * {@link #EXTRA_TRACK} when set.
     * @param intent
     * @return Uri
     */
    private Uri resolveKegUri(Intent intent) {
        final Uri kegUri = intent.getParcelableExtra(EXTRA_TRACK);
        if (kegUri != null) {
            return kegUri;
        } else {
            return KegbotContract.Kegs.buildKegUri(mKegId);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        if (token == DrinksQuery._TOKEN) {
            //onDrinkQueryComplete(cursor);
        } else if (token == KegsQuery._TOKEN) {
            onKegQueryComplete(cursor);
        } else {
        	if (cursor != null)
        		cursor.close();
        }
    }

    /**
     * Handle {@link SessionsQuery} {@link Cursor}.
     */
    private void onDrinkQueryComplete(Cursor cursor) {
        try {
        	if (!cursor.moveToFirst()) {
                return;
            }


        } finally {
            cursor.close();
        }
    }

    /**
     * Handle {@link TracksQuery} {@link Cursor}.
     */
    private void onKegQueryComplete(Cursor cursor) {
        try {
        	mSessionCursor = true;
            if (!cursor.moveToFirst()) {
                return;
            }
            
            // Use found keg to build title-bar
            ActivityHelper activityHelper = ((BaseActivity) getActivity()).getActivityHelper();
            activityHelper.setActionBarTitle("Keg " + cursor.getString(KegsQuery.KEG_ID));
            
            final String kegImageUrl = cursor.getString(KegsQuery.IMAGE_URL);
            if (!TextUtils.isEmpty(kegImageUrl)) {
            	
            	imageLoader.DisplayImage(kegImageUrl, getActivity(), kegImage);
            }

            mTitleString = cursor.getString(KegsQuery.KEG_NAME);
            mTitle.setText(mTitleString);
            String desc = cursor.getString(KegsQuery.DESCRIPTION);
            mSubtitle.setText(desc);

            //mUrl = cursor.getString(DrinksQuery.URL);
            if (TextUtils.isEmpty(mUrl)) {
                mUrl = "";
            }

            //mHashtag = cursor.getString(DrinksQuery.HASHTAG);
            mTagDisplay = (TextView) mRootView.findViewById(R.id.session_tags_button);
            if (!TextUtils.isEmpty(mHashtag)) {
                // Create the button text
                SpannableStringBuilder sb = new SpannableStringBuilder();
                sb.append(getString(R.string.tag_stream) + " ");
                int boldStart = sb.length();
                sb.append(getHashtagsString());
                sb.setSpan(sBoldSpan, boldStart, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                mTagDisplay.setText(sb);

                mTagDisplay.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), TagStreamActivity.class);
                        intent.putExtra(TagStreamFragment.EXTRA_QUERY, getHashtagsString());
                        startActivity(intent);
                    }
                });
            } else {
                mTagDisplay.setVisibility(View.GONE);
            }

            //mRoomId = cursor.getString(DrinksQuery.ROOM_ID);

            // Unregister around setting checked state to avoid triggering
            // listener since change isn't user generated.
            mStarred.setOnCheckedChangeListener(null);
            mStarred.setChecked(cursor.getInt(KegsQuery.STARRED) != 0);
            mStarred.setOnCheckedChangeListener(this);

            final String sessionAbstract = ""; //cursor.getString(DrinksQuery.ABSTRACT);
            if (!TextUtils.isEmpty(sessionAbstract)) {
                UIUtils.setTextMaybeHtml(mAbstract, sessionAbstract);
                mAbstract.setVisibility(View.VISIBLE);
                mHasSummaryContent = true;
            } else {
                mAbstract.setVisibility(View.GONE);
            }

            final View requirementsBlock = mRootView.findViewById(R.id.keg_detail_block);
            final String sessionRequirements = UnitUtils.mlToPint(cursor.getString(KegsQuery.VOLUME_REMAIN)) + " pints";
            if (!TextUtils.isEmpty(sessionRequirements)) {
                UIUtils.setTextMaybeHtml(mRequirements, sessionRequirements);
                requirementsBlock.setVisibility(View.VISIBLE);
                mHasSummaryContent = true;
            } else {
                requirementsBlock.setVisibility(View.GONE);
            }

            // Show empty message when all data is loaded, and nothing to show
            if (mSpeakersCursor && !mHasSummaryContent) {
                mRootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            }

            AnalyticsUtils.getInstance(getActivity()).trackPageView("/Kegs/" + mTitleString);

            //updateLinksTab(cursor);
            //updateNotesTab();
            
        } finally {
            cursor.close();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.session_detail_menu_items, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String shareString;
        final Intent intent;

        switch (item.getItemId()) {
            case R.id.menu_map:
                intent = new Intent(getActivity().getApplicationContext(),
                        UIUtils.getMapActivityClass(getActivity()));
                intent.putExtra(MapFragment.EXTRA_ROOM, mRoomId);
                startActivity(intent);
                return true;

            case R.id.menu_share:
                // TODO: consider bringing in shortlink to session
                shareString = getString(R.string.share_template, mTitleString, getHashtagsString(),
                        mUrl);
                intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, shareString);
                startActivity(Intent.createChooser(intent, getText(R.string.title_share)));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle toggling of starred checkbox.
     */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final ContentValues values = new ContentValues();
        values.put(KegbotContract.Kegs.KEG_STARRED, isChecked ? 1 : 0);
        mHandler.startUpdate(mKegUri, values);

        // Because change listener is set to null during initialization, these won't fire on
        // pageview.
        AnalyticsUtils.getInstance(getActivity()).trackEvent(
                "Sandbox", isChecked ? "Starred" : "Unstarred", mTitleString, 0);
    }

    /**
     * Build and add "notes" tab.
     */
    private void setupNotesTab() {


    }

    /*
     * Event structure:
     * Category -> "Session Details"
     * Action -> "Create Note", "View Note", etc
     * Label -> Session's Title
     * Value -> 0.
     */
    public void fireNotesEvent(int actionId) {
        AnalyticsUtils.getInstance(getActivity()).trackEvent(
                "Session Details", getActivity().getString(actionId), mTitleString, 0);
    }

    /*
     * Event structure:
     * Category -> "Session Details"
     * Action -> Link Text
     * Label -> Session's Title
     * Value -> 0.
     */
    public void fireLinkEvent(int actionId) {
        AnalyticsUtils.getInstance(getActivity()).trackEvent(
                "Link Details", getActivity().getString(actionId), mTitleString, 0);
    }

    private void updateNotesTab() {
        final CatchNotesHelper helper = new CatchNotesHelper(getActivity());
        final boolean notesInstalled = helper.isNotesInstalledAndMinimumVersion();

        final Intent marketIntent = helper.notesMarketIntent();
        final Intent newIntent = helper.createNoteIntent(
                getString(R.string.note_template, mTitleString, getHashtagsString()));
        
        final Intent viewIntent = helper.viewNotesIntent(getHashtagsString());

        // Set icons and click listeners
        ((ImageView) mRootView.findViewById(R.id.notes_catch_market_icon)).setImageDrawable(
                UIUtils.getIconForIntent(getActivity(), marketIntent));
        ((ImageView) mRootView.findViewById(R.id.notes_catch_new_icon)).setImageDrawable(
                UIUtils.getIconForIntent(getActivity(), newIntent));
        ((ImageView) mRootView.findViewById(R.id.notes_catch_view_icon)).setImageDrawable(
                UIUtils.getIconForIntent(getActivity(), viewIntent));

        // Set click listeners
        mRootView.findViewById(R.id.notes_catch_market_link).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        startActivity(marketIntent);
                        fireNotesEvent(R.string.notes_catch_market_title);
                    }
                });

        mRootView.findViewById(R.id.notes_catch_new_link).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        startActivity(newIntent);
                        fireNotesEvent(R.string.notes_catch_new_title);
                    }
                });

        mRootView.findViewById(R.id.notes_catch_view_link).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        startActivity(viewIntent);
                        fireNotesEvent(R.string.notes_catch_view_title);
                    }
                });

        // Show/hide elements
        mRootView.findViewById(R.id.notes_catch_market_link).setVisibility(
                notesInstalled ? View.GONE : View.VISIBLE);
        mRootView.findViewById(R.id.notes_catch_market_separator).setVisibility(
                notesInstalled ? View.GONE : View.VISIBLE);

        mRootView.findViewById(R.id.notes_catch_new_link).setVisibility(
                !notesInstalled ? View.GONE : View.VISIBLE);
        mRootView.findViewById(R.id.notes_catch_new_separator).setVisibility(
                !notesInstalled ? View.GONE : View.VISIBLE);

        mRootView.findViewById(R.id.notes_catch_view_link).setVisibility(
                !notesInstalled ? View.GONE : View.VISIBLE);
        mRootView.findViewById(R.id.notes_catch_view_separator).setVisibility(
                !notesInstalled ? View.GONE : View.VISIBLE);
    }

    /**
     * Build and add "summary" tab.
     */
    private void setupLinksTab() {
        // Summary content comes from existing layout
        mTabHost.addTab(mTabHost.newTabSpec(TAG_DRINKS)
                .setIndicator(buildIndicator(R.string.drink_session))
                .setContent(R.id.tab_session_links));
    }

    private void updateLinksTab(Cursor cursor) {
        ViewGroup container = (ViewGroup) mRootView.findViewById(R.id.links_container);

        // Remove all views but the 'empty' view
        int childCount = container.getChildCount();
        if (childCount > 1) {
            container.removeViews(1, childCount - 1);
        }

        LayoutInflater inflater = getLayoutInflater(null);

        boolean hasLinks = false;
        
        container.findViewById(R.id.empty_links).setVisibility(hasLinks ? View.GONE : View.VISIBLE);
    }

    private String getHashtagsString() {
        if (!TextUtils.isEmpty(mHashtag)) {
            return TagStreamFragment.KEGBOT_HASHTAG + " #" + mHashtag;
        } else {
            return TagStreamFragment.KEGBOT_HASHTAG;
        }
    }

    private BroadcastReceiver mPackageChangesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateNotesTab();
        }
    };

    /**
     * {@link com.goliathonline.android.kegbot.provider.KegbotContract.Kegs} query parameters.
     */
    private interface KegsQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
        		BaseColumns._ID,
                KegbotContract.Kegs.KEG_ID,
                KegbotContract.Kegs.KEG_NAME,
                KegbotContract.Kegs.KEG_ABV,
                KegbotContract.Kegs.DESCRIPTION,
                KegbotContract.Kegs.IMAGE_URL,
                KegbotContract.Kegs.STATUS,
                KegbotContract.Kegs.VOLUME_SIZE,
                KegbotContract.Kegs.VOLUME_REMAIN,
                KegbotContract.Kegs.SIZE_NAME,
                KegbotContract.Kegs.PERCENT_FULL,
                KegbotContract.Kegs.KEG_STARRED,
        };

        int _ID = 0;
        int KEG_ID = 1;
        int KEG_NAME = 2;
        int KEG_ABV = 3;
        int DESCRIPTION = 4;
        int IMAGE_URL = 5;
        int STATUS = 6;
        int VOLUME_SIZE = 7;
        int VOLUME_REMAIN = 8;
        int SIZE_NAME = 9;
        int PERCENT_FULL = 10;
        int STARRED = 11;
    }

    /**
     * {@link com.goliathonline.android.kegbot.provider.KegbotContract.Drinks} query parameters.
     */
    private interface DrinksQuery {
        int _TOKEN = 0x2;

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
