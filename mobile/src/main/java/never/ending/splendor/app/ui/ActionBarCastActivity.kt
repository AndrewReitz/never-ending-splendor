package never.ending.splendor.app.ui

import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl
import com.google.android.libraries.cast.companionlibrary.widgets.IntroductoryOverlay
import never.ending.splendor.R
import never.ending.splendor.app.inject
import timber.log.Timber
import javax.inject.Inject

/**
 * Abstract activity with toolbar, navigation drawer and cast support. Needs to be extended by
 * any activity that wants to be shown as a top level activity.
 *
 * The requirements for a subclass is to call [.initializeToolbar] on onCreate, after
 * setContentView() is called and have three mandatory layout elements:
 * a [androidx.appcompat.widget.Toolbar] with id 'toolbar',
 * a [androidx.drawerlayout.widget.DrawerLayout] with id 'drawerLayout' and
 * a [android.widget.ListView] with id 'drawerList'.
 */
abstract class ActionBarCastActivity : AppCompatActivity() {
    @Inject
    var mCastManager: VideoCastManager? = null
    private var mMediaRouteMenuItem: MenuItem? = null
    private var mToolbar: Toolbar? = null
    private var mToolbarInitialized = false
    private val mItemToOpenWhenDrawerCloses = -1
    private val mCastConsumer: VideoCastConsumerImpl = object : VideoCastConsumerImpl() {
        override fun onFailed(resourceId: Int, statusCode: Int) {
            Timber.d("onFailed %s status %s", resourceId, statusCode)
        }

        override fun onConnectionSuspended(cause: Int) {
            Timber.d("onConnectionSuspended() was called with cause: %s", cause)
        }

        override fun onConnectivityRecovered() {}
        override fun onCastAvailabilityChanged(castPresent: Boolean) {
            if (castPresent) {
                Handler().postDelayed(
                    {
                        if (mMediaRouteMenuItem!!.isVisible) {
                            Timber.d("Cast Icon is visible")
                            showFtu()
                        }
                    },
                    DELAY_MILLIS.toLong()
                )
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Activity onCreate")
        this.inject()

        // Ensure that Google Play Service is available.
        VideoCastManager.checkGooglePlayServices(this)
        mCastManager!!.reconnectSessionIfPossible()
    }

    override fun onStart() {
        super.onStart()
        check(mToolbarInitialized) {
            "You must run super.initializeToolbar at " +
                "the end of your onCreate method"
        }
    }

    public override fun onResume() {
        super.onResume()
        mCastManager!!.addVideoCastConsumer(mCastConsumer)
        mCastManager!!.incrementUiCounter()
    }

    public override fun onPause() {
        super.onPause()
        mCastManager!!.removeVideoCastConsumer(mCastConsumer)
        mCastManager!!.decrementUiCounter()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu)
        mMediaRouteMenuItem = mCastManager!!.addMediaRouterButton(menu, R.id.media_route_menu_item)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // If not handled by drawerToggle, home needs to be handled by returning to previous
        if (item != null && item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        // Otherwise, it may return to the previous fragment stack
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        } else {
            // Lastly, it will rely on the system behavior for back
            super.onBackPressed()
        }
    }

    override fun setTitle(title: CharSequence) {
        super.setTitle(title)
        mToolbar!!.title = title
    }

    fun setSubtitle(title: CharSequence?) {
        mToolbar!!.subtitle = title
    }

    override fun setTitle(titleId: Int) {
        super.setTitle(titleId)
        mToolbar!!.setTitle(titleId)
    }

    protected fun initializeToolbar() {
        mToolbar = findViewById<View>(R.id.toolbar) as Toolbar
        checkNotNull(mToolbar) {
            "Layout is required to include a Toolbar with id " +
                "'toolbar'"
        }
        mToolbar!!.inflateMenu(R.menu.main)
        setSupportActionBar(mToolbar)
        mToolbarInitialized = true
    }

    /**
     * Shows the Cast First Time User experience to the user (an overlay that explains what is
     * the Cast icon)
     */
    private fun showFtu() {
        val menu = mToolbar!!.menu
        val view = menu.findItem(R.id.media_route_menu_item).actionView
        if (view is MediaRouteButton) {
            val overlay = IntroductoryOverlay.Builder(this)
                .setMenuItem(mMediaRouteMenuItem)
                .setTitleText(R.string.touch_to_cast)
                .setSingleTime()
                .build()
            overlay.show()
        }
    }

    companion object {
        private const val DELAY_MILLIS = 1000
    }
}
