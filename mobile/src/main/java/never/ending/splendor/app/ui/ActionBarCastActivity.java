/*
 * Copyright (C) 2014 The Android Open Source Project
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
package never.ending.splendor.app.ui;

import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.mediarouter.app.MediaRouteButton;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import never.ending.splendor.R;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.widgets.IntroductoryOverlay;
import com.google.android.material.navigation.NavigationView;

import javax.inject.Inject;

import never.ending.splendor.app.RoboPhishApplicationKt;
import timber.log.Timber;

/**
 * Abstract activity with toolbar, navigation drawer and cast support. Needs to be extended by
 * any activity that wants to be shown as a top level activity.
 *
 * The requirements for a subclass is to call {@link #initializeToolbar()} on onCreate, after
 * setContentView() is called and have three mandatory layout elements:
 * a {@link androidx.appcompat.widget.Toolbar} with id 'toolbar',
 * a {@link androidx.drawerlayout.widget.DrawerLayout} with id 'drawerLayout' and
 * a {@link android.widget.ListView} with id 'drawerList'.
 */
public abstract class ActionBarCastActivity extends AppCompatActivity {

    private static final int DELAY_MILLIS = 1000;

    @Inject VideoCastManager mCastManager;

    private MenuItem mMediaRouteMenuItem;
    private Toolbar mToolbar;

    private boolean mToolbarInitialized;

    private int mItemToOpenWhenDrawerCloses = -1;

    private final VideoCastConsumerImpl mCastConsumer = new VideoCastConsumerImpl() {

        @Override
        public void onFailed(int resourceId, int statusCode) {
            Timber.d("onFailed %s status %s", resourceId,  statusCode);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Timber.d("onConnectionSuspended() was called with cause: %s", cause);
        }

        @Override
        public void onConnectivityRecovered() {
        }

        @Override
        public void onCastAvailabilityChanged(boolean castPresent) {
            if (castPresent) {
                new Handler().postDelayed(() -> {
                    if (mMediaRouteMenuItem.isVisible()) {
                        Timber.d("Cast Icon is visible");
                        showFtu();
                    }
                }, DELAY_MILLIS);
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("Activity onCreate");

        RoboPhishApplicationKt.inject(this);

        // Ensure that Google Play Service is available.
        VideoCastManager.checkGooglePlayServices(this);

        mCastManager.reconnectSessionIfPossible();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mToolbarInitialized) {
            throw new IllegalStateException("You must run super.initializeToolbar at " +
                "the end of your onCreate method");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mCastManager.addVideoCastConsumer(mCastConsumer);
        mCastManager.incrementUiCounter();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCastManager.removeVideoCastConsumer(mCastConsumer);
        mCastManager.decrementUiCounter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        mMediaRouteMenuItem = mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If not handled by drawerToggle, home needs to be handled by returning to previous
        if (item != null && item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Otherwise, it may return to the previous fragment stack
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            // Lastly, it will rely on the system behavior for back
            super.onBackPressed();
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mToolbar.setTitle(title);
    }

    public void setSubtitle(CharSequence title) {
        mToolbar.setSubtitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        mToolbar.setTitle(titleId);
    }

    protected void initializeToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar == null) {
            throw new IllegalStateException("Layout is required to include a Toolbar with id " +
                "'toolbar'");
        }
        mToolbar.inflateMenu(R.menu.main);
        setSupportActionBar(mToolbar);
        mToolbarInitialized = true;
    }

    private void populateDrawerItems(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    menuItem.setChecked(true);
                    mItemToOpenWhenDrawerCloses = menuItem.getItemId();
                    return true;
                });
        if (MusicPlayerActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_allmusic);
        } else if (PlaceholderActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_playlists);
        }
    }

    /**
     * Shows the Cast First Time User experience to the user (an overlay that explains what is
     * the Cast icon)
     */
    private void showFtu() {
        Menu menu = mToolbar.getMenu();
        View view = menu.findItem(R.id.media_route_menu_item).getActionView();
        if (view instanceof MediaRouteButton) {
            IntroductoryOverlay overlay = new IntroductoryOverlay.Builder(this)
                    .setMenuItem(mMediaRouteMenuItem)
                    .setTitleText(R.string.touch_to_cast)
                    .setSingleTime()
                    .build();
            overlay.show();
        }
    }
}
