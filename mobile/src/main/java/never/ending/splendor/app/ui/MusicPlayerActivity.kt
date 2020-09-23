package never.ending.splendor.app.ui

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.text.TextUtils
import never.ending.splendor.R
import never.ending.splendor.app.ui.MediaBrowserFragment.MediaFragmentListener
import never.ending.splendor.app.utils.MediaIDHelper
import timber.log.Timber

/**
 * Main activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 */
class MusicPlayerActivity : BaseActivity(), MediaFragmentListener {
    private var mVoiceSearchParams: Bundle? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Activity onCreate")
        setContentView(R.layout.activity_player)
        initializeToolbar()
        initializeFromParams(savedInstanceState, intent)

        // Only check if a full screen player is needed on the first time:
        if (savedInstanceState == null) {
            startFullScreenActivityIfNeeded(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val mediaId = mediaId
        if (mediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mediaId)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onMediaItemSelected(item: MediaBrowserCompat.MediaItem) {
        Timber.d("onMediaItemSelected, mediaId=%s", item.mediaId)
        if (item.isPlayable) {
            supportMediaController!!.transportControls
                .playFromMediaId(item.mediaId, null)
        } else if (item.isBrowsable) {
            var title = ""
            var subtitle = ""
            if (item.description.title != null) {
                title = item.description.title.toString()
            }
            if (item.description.subtitle != null) {
                subtitle = item.description.subtitle.toString()
            }
            navigateToBrowser(title, subtitle, item.mediaId)
        } else {
            Timber.w(
                "Ignoring MediaItem that is neither browsable nor playable: mediaId=%s",
                item.mediaId
            )
        }
    }

    override fun setToolbarTitle(title: CharSequence) {
        var title: CharSequence? = title
        Timber.d("Setting toolbar title to %s", title)
        if (title == null) {
            title = getString(R.string.app_name)
        }
        setTitle(title)
    }

    override fun setToolbarSubTitle(subTitlle: CharSequence) {
        var subTitlle: CharSequence? = subTitlle
        Timber.d("Setting toolbar title to %s", subTitlle)
        if (subTitlle == null) {
            subTitlle = ""
        }
        setSubtitle(subTitlle)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent, intent=%s", intent)
        initializeFromParams(null, intent)
        startFullScreenActivityIfNeeded(intent)
    }

    private fun startFullScreenActivityIfNeeded(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra(EXTRA_START_FULLSCREEN, false)) {
            val fullScreenIntent = Intent(this, FullScreenPlayerActivity::class.java)
                .setFlags(
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                .putExtra(
                    EXTRA_CURRENT_MEDIA_DESCRIPTION,
                    intent.getParcelableExtra(
                        EXTRA_CURRENT_MEDIA_DESCRIPTION
                    ) as Parcelable?
                )
            startActivity(fullScreenIntent)
        }
    }

    protected fun initializeFromParams(savedInstanceState: Bundle?, intent: Intent) {
        var mediaId: String? = null
        // check if we were started from a "Play XYZ" voice search. If so, we save the extras
        // (which contain the query details) in a parameter, so we can reuse it later, when the
        // MediaSession is connected.
        if (intent.action != null &&
            intent.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
        ) {
            mVoiceSearchParams = intent.extras
            Timber.d(
                "Starting from voice search query=%s",
                mVoiceSearchParams!!.getString(SearchManager.QUERY)
            )
        } else if (intent.action != null &&
            intent.action == MediaStore.INTENT_ACTION_MEDIA_SEARCH
        ) {
            navigateToBrowser(null, null, null)
            val extras = intent.extras
            val title = extras!!.getString("title")
            val subtitle = extras.getString("subtitle")
            mediaId = extras.getString("showid")
            val year = subtitle!!.split("-").toTypedArray()[0]
            // browse to year...
            navigateToBrowser(null, null, MediaIDHelper.MEDIA_ID_SHOWS_BY_YEAR + "/" + year)

            // now launch as show
            navigateToBrowser(title, subtitle, mediaId)
        } else {
            if (savedInstanceState != null) {
                // If there is a saved media ID, use it
                mediaId = savedInstanceState.getString(SAVED_MEDIA_ID)
            }
            navigateToBrowser(null, null, mediaId)
        }
    }

    private fun navigateToBrowser(title: String?, subtitle: String?, mediaId: String?) {
        Timber.d("navigateToBrowser, mediaId=%s", mediaId)
        var fragment = browseFragment
        if (fragment == null || !TextUtils.equals(fragment.mediaId, mediaId)) {
            fragment = MediaBrowserFragment()
            fragment.setMediaId(title, subtitle, mediaId)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.setCustomAnimations(
                R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                R.animator.slide_in_from_left, R.animator.slide_out_to_right
            )
            transaction.replace(R.id.container, fragment, FRAGMENT_TAG)
            // If this is not the top level media (root), we add it to the fragment back stack,
            // so that actionbar toggle and Back will work appropriately:
            if (mediaId != null) {
                transaction.addToBackStack(null)
            }
            transaction.commit()
        }
    }

    val mediaId: String?
        get() {
            val fragment = browseFragment ?: return null
            return fragment.mediaId
        }
    private val browseFragment: MediaBrowserFragment?
        private get() = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as MediaBrowserFragment?

    override fun onMediaControllerConnected() {
        if (mVoiceSearchParams != null) {
            // If there is a bootstrap parameter to start from a search query, we
            // send it to the media session and set it to null, so it won't play again
            // when the activity is stopped/started or recreated:
            val query = mVoiceSearchParams!!.getString(SearchManager.QUERY)
            supportMediaController!!.transportControls
                .playFromSearch(query, mVoiceSearchParams)
            mVoiceSearchParams = null
        }
        browseFragment!!.onConnected()
    }

    companion object {
        private const val SAVED_MEDIA_ID = "com.example.android.uamp.MEDIA_ID"
        private const val FRAGMENT_TAG = "uamp_list_container"
        const val EXTRA_START_FULLSCREEN = "com.example.android.uamp.EXTRA_START_FULLSCREEN"

        /**
         * Optionally used with [.EXTRA_START_FULLSCREEN] to carry a MediaDescription to
         * the [FullScreenPlayerActivity], speeding up the screen rendering
         * while the [android.support.v4.media.session.MediaControllerCompat] is connecting.
         */
        const val EXTRA_CURRENT_MEDIA_DESCRIPTION =
            "com.example.android.uamp.CURRENT_MEDIA_DESCRIPTION"
    }
}
