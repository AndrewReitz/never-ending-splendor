package never.ending.splendor.app.ui

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import nes.networking.phishin.PhishInRepository
import nes.networking.phishnet.PhishNetRepository
import never.ending.splendor.R
import never.ending.splendor.app.utils.MediaIdHelper
import never.ending.splendor.app.utils.MediaIdHelper.extractShowFromMediaID
import never.ending.splendor.app.utils.MediaIdHelper.getHierarchy
import never.ending.splendor.app.utils.MediaIdHelper.isShow
import never.ending.splendor.databinding.FragmentListBinding
import never.ending.splendor.databinding.FragmentListShowBinding
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.x.di
import org.kodein.di.instance
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
class MediaBrowserFragment : Fragment(), DIAware {

    private lateinit var foreground: CoroutineScope

    override val di: DI by di()

    private val phishNetRepository: PhishNetRepository by instance()
    private val phishInRepository: PhishInRepository by instance()

    private val browserAdapter: MediaBrowserAdapter by lazy {
        MediaBrowserAdapter(
            requireActivity(),
            MediaControllerCompat.getMediaController(requireActivity())
        ) { item ->
            checkForUserVisibleErrors(false)
            mediaFragmentListener.onMediaItemSelected(item)
        }
    }

    private var _fragmentListBinding: FragmentListBinding? = null
    private val fragmentListBinding get() = requireNotNull(_fragmentListBinding)

    private var _fragmentListShowBinding: FragmentListShowBinding? = null
    private val fragmentListShowBinding get() = requireNotNull(_fragmentListShowBinding)

    private var mMediaId: String? = null

    private val mediaFragmentListener get() = activity as MediaFragmentListener

    private val errorView: View
        get() = when {
            _fragmentListBinding != null -> fragmentListBinding.playbackError
            _fragmentListShowBinding != null -> fragmentListShowBinding.playbackError
            else -> error("somehow neither are null")
        }

    private val errorMessage: TextView
        get() = when {
            _fragmentListBinding != null -> fragmentListBinding.errorMessage
            _fragmentListShowBinding != null -> fragmentListShowBinding.errorMessage
            else -> error("somehow neither are null")
        }

    private val progressBar: ProgressBar
        get() = when {
            _fragmentListBinding != null -> fragmentListBinding.progressBar
            _fragmentListShowBinding != null -> fragmentListShowBinding.progressBar
            else -> error("somehow neither are null")
        }

    private val listView: RecyclerView
        get() = when {
            _fragmentListBinding != null -> fragmentListBinding.listView
            _fragmentListShowBinding != null -> fragmentListShowBinding.listView
            else -> error("somehow neither are null")
        }

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
                progressBar.visibility =
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
                    progressBar.visibility = View.INVISIBLE
                    browserAdapter.media = children
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("fragment.onCreateView")
        foreground = MainScope()

        val mediaId = mediaId

        val rootView = if (mediaId != null && isShow(mediaId)) {
            Timber.d("display show info: mediaId = %s and media is a show", mediaId)
            _fragmentListShowBinding = FragmentListShowBinding.inflate(inflater, container, false)

            with(fragmentListShowBinding) {
                viewpager.adapter = ShowPagerAdapter(root)
                viewpager.offscreenPageLimit = 3

                slidingTabs.setupWithViewPager(viewpager)
                foreground.launch {
                    // todo better way to get show date.
                    val setlistResult = phishNetRepository.setlist(
                        requireNotNull(subTitle).replace(
                            ".",
                            "-"
                        )
                    )

                    when (setlistResult) {
                        is Success -> {
                            val data = setlistResult.value[0]

                            // todo check if extra showid extractShowFromMediaID is the same
                            // if it is use that instead.
                            val showid = data.showid
                            val header = "<h1>" + data.venue + "</h1>" +
                                    "<h2>" + data.location + "</h2>"

                            val setlistData = data.setlistdata
                            val setlistnotes: String = data.setlistnotes

                            setlistWebview.loadData(
                                header + setlistData + setlistnotes,
                                "text/html",
                                null
                            )

                            when (val reviewsResult =
                                phishNetRepository.reviews(showid.toString())) {
                                is Success -> {
                                    val display = StringBuilder()
                                    reviewsResult.value.forEach {
                                        val reviewSubs = it.reviewtext.replace("\n", "<br/>")
                                        display.append("<h2>")
                                            .append(it.username)
                                            .append("</h2>")
                                            .append("<h4>")
                                            .append(it.posted_date)
                                            .append("</h4>")
                                            .append(reviewSubs)
                                            .append("<br/>")
                                    }
                                }
                                is Failure -> reviewsWebview.loadData(
                                    "<div>Error loading Reviews</div>",
                                    "text/html",
                                    null
                                )
                            }
                        }
                        is Failure -> {
                            setlistWebview.loadData(
                                "<div>Error loading Setlist</div>",
                                "text/html",
                                null
                            )
                            reviewsWebview.loadData(
                                "<div>Error loading Reviews</div>",
                                "text/html",
                                null
                            )
                        }
                    }
                }

                val showId = requireNotNull(extractShowFromMediaID(mediaId))
                foreground.launch {
                    when(val showResult = phishInRepository.show(showId)) {
                        is Success -> {
                            val tapernotes = showResult.value.taper_notes ?: "Not available"
                            val notesSubs = tapernotes.replace("\n".toRegex(), "<br/>")
                            tapernotesWebview.loadData(notesSubs, "text/html", null)
                        }
                        is Failure -> tapernotesWebview.loadData(
                            "<div>Error loading Taper Notes</div>",
                            "text/html",
                            null
                        )
                    }
                }
                // todo load tapper notes
            }

            fragmentListShowBinding.root
        } else {
            _fragmentListBinding = FragmentListBinding.inflate(inflater, container, false)
            fragmentListBinding.root
        }

        progressBar.visibility = View.VISIBLE

        val layoutManager = LinearLayoutManager(context)
        listView.layoutManager = LinearLayoutManager(context)
        listView.adapter = browserAdapter

        val dividerItemDecoration = DividerItemDecoration(
            listView.context,
            layoutManager.orientation
        )
        listView.addItemDecoration(dividerItemDecoration)

        return rootView
    }

    override fun onStart() {
        super.onStart()

        // fetch browsing information to fill the listview:
        val mediaBrowser = mediaFragmentListener.mediaBrowser
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

        val mediaBrowser = mediaFragmentListener.mediaBrowser
        if (mediaBrowser.isConnected && mMediaId != null) {
            mediaBrowser.unsubscribe(mMediaId!!)
        }
        val controller = (activity as BaseActivity?)?.supportMediaController!!
        controller.unregisterCallback(mediaControllerCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentListBinding = null
        _fragmentListShowBinding = null
        foreground.cancel()
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
            mMediaId = mediaFragmentListener.mediaBrowser.root
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
        mediaFragmentListener.mediaBrowser.unsubscribe(mMediaId!!)
        mediaFragmentListener.mediaBrowser.subscribe(mMediaId!!, subscriptionCallback)

        // Add MediaController callback so we can redraw the list when metadata changes:
        val controller = (activity as BaseActivity?)?.supportMediaController
        requireNotNull(controller).registerCallback(mediaControllerCallback)
    }

    private fun checkForUserVisibleErrors(forceError: Boolean) {
        var showError = forceError
        // otherwise, if state is ERROR and metadata!=null, use playback state error message:
        val controller = (activity as BaseActivity?)?.supportMediaController
        if (controller != null && controller.metadata != null && controller.playbackState != null && controller.playbackState.state == PlaybackStateCompat.STATE_ERROR && controller.playbackState.errorMessage != null) {
            errorMessage.text = controller.playbackState.errorMessage
            showError = true
        } else if (forceError) {
            // Finally, if the caller requested to show error, show a generic message:
            errorMessage.setText(R.string.error_loading_media)
            showError = true
        }
        errorView.visibility = if (showError) View.VISIBLE else View.GONE
        if (showError) progressBar.visibility = View.INVISIBLE
        Timber.d(
            "checkForUserVisibleErrors. forceError=%s  showError=%s", forceError,
            showError
        )
    }

    private fun updateTitle() {
        if (mMediaId!!.startsWith(MediaIdHelper.MEDIA_ID_SHOWS_BY_YEAR)) {
            val year = getHierarchy(mMediaId!!)[1]
            mediaFragmentListener.setToolbarTitle(year)
            mediaFragmentListener.setToolbarSubTitle("")
            return
        }
        if (mMediaId!!.startsWith(MediaIdHelper.MEDIA_ID_TRACKS_BY_SHOW)) {
            mediaFragmentListener.setToolbarTitle(title.orEmpty())
            mediaFragmentListener.setToolbarSubTitle(subTitle.orEmpty())
            return
        }
        if (MediaIdHelper.MEDIA_ID_ROOT == mMediaId) {
            mediaFragmentListener.setToolbarTitle("")
            return
        }
        val mediaBrowser = mediaFragmentListener.mediaBrowser
        mediaBrowser.getItem(
            mMediaId!!,
            object : MediaBrowserCompat.ItemCallback() {
                override fun onItemLoaded(item: MediaBrowserCompat.MediaItem) {
                    mediaFragmentListener.setToolbarTitle(
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
