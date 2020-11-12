package never.ending.splendor.app.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl
import com.google.android.libraries.cast.companionlibrary.widgets.IntroductoryOverlay
import never.ending.splendor.R
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.di
import org.kodein.di.instance
import timber.log.Timber

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
abstract class ActionBarCastActivity : AppCompatActivity(), DIAware {

    override val di: DI by di()

    private val castManager: VideoCastManager by instance()

    private lateinit var toolbar: Toolbar

    private var mediaRouteMenuItem: MenuItem? = null

    private val castConsumer: VideoCastConsumerImpl = object : VideoCastConsumerImpl() {
        override fun onFailed(resourceId: Int, statusCode: Int) {
            Timber.d("onFailed %s status %s", resourceId, statusCode)
        }

        override fun onConnectionSuspended(cause: Int) {
            Timber.d("onConnectionSuspended() was called with cause: %s", cause)
        }

        override fun onConnectivityRecovered() {}
        override fun onCastAvailabilityChanged(castPresent: Boolean) {
            if (castPresent) {
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        if (mediaRouteMenuItem!!.isVisible) {
                            Timber.d("Cast Icon is visible")
                            showFirstTimeCastMessage()
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

        // Ensure that Google Play Service is available.
        VideoCastManager.checkGooglePlayServices(this)
        castManager.reconnectSessionIfPossible()
    }

    public override fun onResume() {
        super.onResume()
        castManager.addVideoCastConsumer(castConsumer)
        castManager.incrementUiCounter()
    }

    public override fun onPause() {
        super.onPause()
        castManager.removeVideoCastConsumer(castConsumer)
        castManager.decrementUiCounter()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu)
        mediaRouteMenuItem = castManager.addMediaRouterButton(menu, R.id.media_route_menu_item)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // If not handled by drawerToggle, home needs to be handled by returning to previous
        if (item.itemId == android.R.id.home) {
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
        toolbar.title = title
    }

    fun setSubtitle(title: CharSequence?) {
        toolbar.subtitle = title
    }

    override fun setTitle(titleId: Int) {
        super.setTitle(titleId)
        toolbar.setTitle(titleId)
    }

    protected fun initializeToolbar(toolbar: Toolbar) {
        this.toolbar = toolbar
        toolbar.inflateMenu(R.menu.main)
        setSupportActionBar(toolbar)
    }

    /**
     * Shows the Cast First Time User experience to the user (an overlay that explains what is
     * the Cast icon)
     */
    private fun showFirstTimeCastMessage() {
        val menu = toolbar.menu
        val view = menu.findItem(R.id.media_route_menu_item).actionView
        if (view is MediaRouteButton) {
            val overlay = IntroductoryOverlay.Builder(this)
                .setMenuItem(mediaRouteMenuItem)
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
