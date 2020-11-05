package never.ending.splendor.app.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import never.ending.splendor.R
import never.ending.splendor.app.utils.MediaIdHelper
import never.ending.splendor.app.utils.MediaIdHelper.extractShowFromMediaID
import never.ending.splendor.app.utils.MediaIdHelper.getHierarchy
import never.ending.splendor.app.utils.MediaIdHelper.isShow
import timber.log.Timber

/**
 * A Fragment that lists all the various browsable queues available
 * from a [android.service.media.MediaBrowserService].
 *
 *
 * It uses a [MediaBrowserCompat] to connect to the [MusicService].
 * Once connected, the fragment subscribes to get all the children.
 * All [MediaBrowserCompat.MediaItem]'s that can be browsed are shown in a ListView.
 */
class MediaBrowserFragment : Fragment() {

    private val browserAdapter: MediaBrowserAdapter by lazy {
        // todo make this better
        MediaBrowserAdapter(
            requireActivity(),
            MediaControllerCompat.getMediaController(requireActivity())
        ) {
            Timber.d("clicked!")
            // todo
        }
    }

    private var mMediaId: String? = null
    private var mediaFragmentListener: MediaFragmentListener? = null
    private var errorView: View? = null
    private var errorMessage: TextView? = null
    private var progressBar: ProgressBar? = null

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private val mediaControllerCallback: MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                super.onMetadataChanged(metadata)
                Timber.d(
                    "Received metadata change to media %s",
                    metadata.description.mediaId
                )
                browserAdapter.notifyDataSetChanged()
                progressBar!!.visibility =
                    View.INVISIBLE // hide progress bar when we receive metadata
            }

            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                super.onPlaybackStateChanged(state)
                Timber.d("Received state change: %s", state)
                checkForUserVisibleErrors(false)
                browserAdapter.notifyDataSetChanged()
            }
        }

    private val subscriptionCallback: MediaBrowserCompat.SubscriptionCallback =
        object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: List<MediaBrowserCompat.MediaItem>
            ) {
                try {
                    Timber.d(
                        "fragment onChildrenLoaded, parentId=%s, count=%s",
                        parentId, children.size
                    )
                    checkForUserVisibleErrors(children.isEmpty())
                    progressBar!!.visibility = View.INVISIBLE
                    browserAdapter.media = children
                    browserAdapter.notifyDataSetChanged()
                } catch (t: Throwable) {
                    Timber.e(t, "Error on childrenloaded")
                }
            }

            override fun onError(id: String) {
                Timber.e("browse fragment subscription onError, id=%s", id)
                Toast.makeText(activity, R.string.error_loading_media, Toast.LENGTH_LONG).show()
                checkForUserVisibleErrors(true)
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // If used on an activity that doesn't implement MediaFragmentListener, it
        // will throw an exception as expected:
        mediaFragmentListener = activity as MediaFragmentListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("fragment.onCreateView")

        val rootView: View
        val mediaId = mediaId
        val listView: RecyclerView

        if (mediaId != null && isShow(mediaId)) {
            Timber.d("display show info: mediaId = %s and media is a show" , mediaId)

            setHasOptionsMenu(true) // show option to download
            rootView = inflater.inflate(R.layout.fragment_list_show, container, false)
            val viewPager: ViewPager = rootView.findViewById(R.id.viewpager)
            viewPager.adapter = ShowPagerAdapter(rootView)
            viewPager.offscreenPageLimit = 3
            val tabLayout: TabLayout = rootView.findViewById(R.id.sliding_tabs)
            tabLayout.setupWithViewPager(viewPager)
            val setlist = rootView.findViewById<WebView>(R.id.setlist_webview)
            setlist.settings.javaScriptEnabled = true

            // TODO Load setlist

            val reviews = rootView.findViewById<WebView>(R.id.reviews_webview)
            reviews.settings.javaScriptEnabled = true

            // todo load reviews


            val tapernotesWebview = rootView.findViewById<WebView>(R.id.tapernotes_webview)
            tapernotesWebview.settings.javaScriptEnabled = true
            val showId = extractShowFromMediaID(mediaId)

            // todo load tapper notes
        } else {
            rootView = inflater.inflate(R.layout.fragment_list, container, false)
        }

        errorView = rootView.findViewById(R.id.playback_error)
        errorMessage = errorView!!.findViewById(R.id.error_message)
        progressBar = rootView.findViewById(R.id.progress_bar)
        progressBar!!.visibility = View.VISIBLE
        listView = rootView.findViewById(R.id.list_view)
        listView.adapter = browserAdapter
        // todo
//        listView.onItemClickListener =
//            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
//                checkForUserVisibleErrors(false)
//                val item = browserAdapter.media.getItem(position)!!
//                mediaFragmentListener!!.onMediaItemSelected(item)
//            }
        return rootView
    }

    override fun onStart() {
        super.onStart()

        // fetch browsing information to fill the listview:
        val mediaBrowser = mediaFragmentListener!!.mediaBrowser
        Timber.d(
            "fragment.onStart, mediaId=%s onConnected=%s", mMediaId,
            mediaBrowser.isConnected
        )
        if (mediaBrowser.isConnected) {
            onConnected()
        }
    }

    override fun onStop() {
        super.onStop()
        val mediaBrowser = mediaFragmentListener!!.mediaBrowser
        if (mediaBrowser.isConnected && mMediaId != null) {
            mediaBrowser.unsubscribe(mMediaId!!)
        }
        val controller = (activity as BaseActivity?)?.supportMediaController!!
        controller.unregisterCallback(mediaControllerCallback)
    }

    override fun onDetach() {
        super.onDetach()
        mediaFragmentListener = null
    }

    val mediaId: String?
        get() {
            val args = arguments
            return args?.getString(ARG_MEDIA_ID)
        }
    val title: String?
        get() {
            val args = arguments
            return args?.getString(ARG_TITLE)
        }
    val subTitle: String?
        get() {
            val args = arguments
            return args?.getString(ARG_SUBTITLE)
        }

    fun setMediaId(title: String?, subtitle: String?, mediaId: String?) {
        val args = Bundle(3)
        args.putString(ARG_MEDIA_ID, mediaId)
        args.putString(ARG_TITLE, title)
        args.putString(ARG_SUBTITLE, subtitle)
        arguments = args
    }

    // Called when the MediaBrowser is connected. This method is either called by the
    // fragment.onStart() or explicitly by the activity in the case where the connection
    // completes after the onStart()
    fun onConnected() {
        if (isDetached) {
            return
        }
        mMediaId = mediaId
        if (mMediaId == null) {
            mMediaId = mediaFragmentListener!!.mediaBrowser.root
        }
        updateTitle()

        // Unsubscribing before subscribing is required if this mediaId already has a subscriber
        // on this MediaBrowser instance. Subscribing to an already subscribed mediaId will replace
        // the callback, but won't trigger the initial callback.onChildrenLoaded.
        //
        // This is temporary: A bug is being fixed that will make subscribe
        // consistently call onChildrenLoaded initially, no matter if it is replacing an existing
        // subscriber or not. Currently this only happens if the mediaID has no previous
        // subscriber or if the media content changes on the service side, so we need to
        // unsubscribe first.
        mediaFragmentListener!!.mediaBrowser.unsubscribe(mMediaId!!)
        mediaFragmentListener!!.mediaBrowser.subscribe(mMediaId!!, subscriptionCallback)

        // Add MediaController callback so we can redraw the list when metadata changes:
        val controller = (activity as BaseActivity?)?.supportMediaController
        controller?.registerCallback(mediaControllerCallback)
    }

    private fun checkForUserVisibleErrors(forceError: Boolean) {
        var showError = forceError
        // otherwise, if state is ERROR and metadata!=null, use playback state error message:
        val controller = (activity as BaseActivity?)?.supportMediaController
        if (controller != null && controller.metadata != null && controller.playbackState != null && controller.playbackState.state == PlaybackStateCompat.STATE_ERROR && controller.playbackState.errorMessage != null) {
            errorMessage!!.text = controller.playbackState.errorMessage
            showError = true
        } else if (forceError) {
            // Finally, if the caller requested to show error, show a generic message:
            errorMessage!!.setText(R.string.error_loading_media)
            showError = true
        }
        errorView!!.visibility = if (showError) View.VISIBLE else View.GONE
        if (showError) progressBar!!.visibility = View.INVISIBLE
        Timber.d(
            "checkForUserVisibleErrors. forceError=%s  showError=%s", forceError,
            showError
        )
    }

    private fun updateTitle() {
        if (mMediaId!!.startsWith(MediaIdHelper.MEDIA_ID_SHOWS_BY_YEAR)) {
            val year = getHierarchy(mMediaId!!)[1]
            mediaFragmentListener!!.setToolbarTitle(year)
            mediaFragmentListener!!.setToolbarSubTitle("")
            return
        }
        if (mMediaId!!.startsWith(MediaIdHelper.MEDIA_ID_TRACKS_BY_SHOW)) {
            mediaFragmentListener!!.setToolbarTitle(title.orEmpty())
            mediaFragmentListener!!.setToolbarSubTitle(subTitle.orEmpty())
            return
        }
        if (MediaIdHelper.MEDIA_ID_ROOT == mMediaId) {
            mediaFragmentListener!!.setToolbarTitle("")
            return
        }
        val mediaBrowser = mediaFragmentListener!!.mediaBrowser
        mediaBrowser.getItem(
            mMediaId!!,
            object : MediaBrowserCompat.ItemCallback() {
                override fun onItemLoaded(item: MediaBrowserCompat.MediaItem) {
                    mediaFragmentListener!!.setToolbarTitle(
                        item.description.title ?: ""
                    )
                }
            }
        )
    }

    interface MediaFragmentListener : MediaBrowserProvider {
        fun onMediaItemSelected(item: MediaBrowserCompat.MediaItem)
        fun setToolbarTitle(title: CharSequence)
        fun setToolbarSubTitle(subtitle: CharSequence)
    }

    companion object {
        private const val ARG_MEDIA_ID = "media_id"
        private const val ARG_TITLE = "title"
        private const val ARG_SUBTITLE = "subtitle"
    }
}
