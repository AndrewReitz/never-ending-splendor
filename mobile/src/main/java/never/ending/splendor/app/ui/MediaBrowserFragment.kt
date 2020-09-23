package never.ending.splendor.app.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestParams
import cz.msebera.android.httpclient.Header
import never.ending.splendor.R
import never.ending.splendor.app.utils.MediaIDHelper
import never.ending.splendor.app.utils.MediaIDHelper.extractMusicIDFromMediaID
import never.ending.splendor.app.utils.MediaIDHelper.extractShowFromMediaID
import never.ending.splendor.app.utils.MediaIDHelper.getHierarchy
import never.ending.splendor.app.utils.MediaIDHelper.isShow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date

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
    private var browserAdapter: BrowseAdapter? = null
    private var mMediaId: String? = null
    private var mediaFragmentListener: MediaFragmentListener? = null
    private var errorView: View? = null
    private var errorMessage: TextView? = null
    private var progressBar: ProgressBar? = null
    private var showData: JSONObject? = null

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
                browserAdapter!!.notifyDataSetChanged()
                progressBar!!.visibility =
                    View.INVISIBLE // hide progress bar when we receive metadata
            }

            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                super.onPlaybackStateChanged(state)
                Timber.d("Received state change: %s", state)
                checkForUserVisibleErrors(false)
                browserAdapter!!.notifyDataSetChanged()
            }
        }

    private val mSubscriptionCallback: MediaBrowserCompat.SubscriptionCallback =
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
                    browserAdapter!!.clear()
                    for (item in children) {
                        browserAdapter!!.add(item)
                    }
                    browserAdapter!!.notifyDataSetChanged()
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
        mediaFragmentListener = activity as MediaFragmentListener?
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("fragment.onCreateView")
        val rootView: View
        val mediaId = mediaId
        val listView: ListView
        if (mediaId != null && isShow(mediaId)) {
            setHasOptionsMenu(true) // show option to download
            rootView = inflater.inflate(R.layout.fragment_list_show, container, false)
            val viewPager: ViewPager = rootView.findViewById(R.id.viewpager)
            viewPager.adapter = ShowPagerAdapter(rootView)
            viewPager.offscreenPageLimit = 3
            val tabLayout: TabLayout = rootView.findViewById(R.id.sliding_tabs)
            tabLayout.setupWithViewPager(viewPager)
            val setlist = rootView.findViewById<WebView>(R.id.setlist_webview)
            setlist.settings.javaScriptEnabled = true
            val setlistClient = AsyncHttpClient()
            val setlistParams = RequestParams()
            setlistParams.put("api", "2.0")
            setlistParams.put("method", "pnet.shows.setlists.get")
            setlistParams.put("showdate", subTitle)
            setlistParams.put("apikey", "C01AEE2002E80723E9E7")
            setlistParams.put("format", "json")
            setlistClient[
                "http://api.phish.net/api.js", setlistParams, object :
                    JsonHttpResponseHandler() {
                    override fun onSuccess(
                        statusCode: Int,
                        headers: Array<Header>,
                        response: JSONArray
                    ) {
                        super.onSuccess(statusCode, headers, response)
                        try {
                            val result = response.getJSONObject(0)
                            val city = result.getString("city")
                            val state = result.getString("state")
                            val country = result.getString("country")
                            val venue = result.getString("venue")
                            val header = "<h1>" + venue + "</h1>" + "<h2>" + city +
                                ", " + state + "<br/>" + country + "</h2>"
                            val setlistdata = result.getString("setlistdata")
                            val setlistnotes = result.getString("setlistnotes")
                            setlist.loadData(
                                header + setlistdata + setlistnotes,
                                "text/html",
                                null
                            )
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(
                        statusCode: Int,
                        headers: Array<Header>,
                        throwable: Throwable,
                        errorResponse: JSONObject
                    ) {
                        super.onFailure(statusCode, headers, throwable, errorResponse)
                    }
                }
            ]
            val reviews = rootView.findViewById<WebView>(R.id.reviews_webview)
            reviews.settings.javaScriptEnabled = true
            val reviewsClient = AsyncHttpClient()
            val reviewsParams = RequestParams()
            reviewsParams.put("api", "2.0")
            reviewsParams.put("method", "pnet.reviews.query")
            reviewsParams.put("showdate", subTitle)
            reviewsParams.put("apikey", "C01AEE2002E80723E9E7")
            reviewsParams.put("format", "json")
            reviewsClient[
                "http://api.phish.net/api.js", reviewsParams, object :
                    JsonHttpResponseHandler() {
                    override fun onSuccess(
                        statusCode: Int,
                        headers: Array<Header>,
                        response: JSONArray
                    ) {
                        super.onSuccess(statusCode, headers, response)
                        try {
                            val display = StringBuilder()
                            val len = response.length()
                            for (i in 0 until len) {
                                val entry = response.getJSONObject(i)
                                val author = entry.getString("author")
                                val review = entry.getString("review")
                                val tstamp = entry.getString("tstamp")
                                val reviewTime = Date(tstamp.toLong() * 1000)
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                val reviewDate = dateFormat.format(reviewTime)
                                val reviewSubs = review.replace("\n".toRegex(), "<br/>")
                                display.append("<h2>").append(author).append("</h2>")
                                    .append("<h4>")
                                    .append(reviewDate).append("</h4>")
                                display.append(reviewSubs).append("<br/>")
                            }
                            reviews.loadData(display.toString(), "text/html", null)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(
                        statusCode: Int,
                        headers: Array<Header>,
                        throwable: Throwable,
                        errorResponse: JSONObject
                    ) {
                        super.onFailure(statusCode, headers, throwable, errorResponse)
                    }
                }
            ]
            val tapernotesWebview = rootView.findViewById<WebView>(R.id.tapernotes_webview)
            tapernotesWebview.settings.javaScriptEnabled = true
            val showId = extractShowFromMediaID(mediaId)
            val tapernotesClient = AsyncHttpClient()
            tapernotesClient[
                "http://phish.in/api/v1/shows/$showId.json", null, object :
                    JsonHttpResponseHandler() {
                    override fun onSuccess(
                        statusCode: Int,
                        headers: Array<Header>,
                        response: JSONObject
                    ) {
                        super.onSuccess(statusCode, headers, response)
                        try {
                            showData = response
                            val data = response.getJSONObject("data")
                            var tapernotes = data.getString("taper_notes")
                            if (tapernotes == "null") tapernotes = "Not available"
                            val notesSubs = tapernotes.replace("\n".toRegex(), "<br/>")
                            tapernotesWebview.loadData(notesSubs, "text/html", null)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(
                        statusCode: Int,
                        headers: Array<Header>,
                        throwable: Throwable,
                        errorResponse: JSONObject
                    ) {
                        super.onFailure(statusCode, headers, throwable, errorResponse)
                    }
                }
            ]
        } else {
            rootView = inflater.inflate(R.layout.fragment_list, container, false)
        }
        errorView = rootView.findViewById(R.id.playback_error)
        errorMessage = errorView!!.findViewById(R.id.error_message)
        progressBar = rootView.findViewById(R.id.progress_bar)
        progressBar!!.visibility = View.VISIBLE
        browserAdapter = BrowseAdapter(activity!!)
        listView = rootView.findViewById(R.id.list_view)
        listView.adapter = browserAdapter
        listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                checkForUserVisibleErrors(false)
                val item = browserAdapter!!.getItem(position)!!
                mediaFragmentListener!!.onMediaItemSelected(item)
            }
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
        mediaFragmentListener!!.mediaBrowser.subscribe(mMediaId!!, mSubscriptionCallback)

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
        if (mMediaId!!.startsWith(MediaIDHelper.MEDIA_ID_SHOWS_BY_YEAR)) {
            val year = getHierarchy(mMediaId!!)[1]
            mediaFragmentListener!!.setToolbarTitle(year)
            mediaFragmentListener!!.setToolbarSubTitle("")
            return
        }
        if (mMediaId!!.startsWith(MediaIDHelper.MEDIA_ID_TRACKS_BY_SHOW)) {
            mediaFragmentListener!!.setToolbarTitle(title.orEmpty())
            mediaFragmentListener!!.setToolbarSubTitle(subTitle.orEmpty())
            return
        }
        if (MediaIDHelper.MEDIA_ID_ROOT == mMediaId) {
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

    // An adapter for showing the list of browsed MediaItem's
    private class BrowseAdapter(context: Activity) : ArrayAdapter<MediaBrowserCompat.MediaItem?>(
        context, R.layout.media_list_item, listOf<MediaBrowserCompat.MediaItem>()
    ) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            var itemState = MediaItemViewHolder.STATE_NONE
            if (item!!.isPlayable) {
                itemState = MediaItemViewHolder.STATE_PLAYABLE
                val controller = (context as BaseActivity).supportMediaController
                if (controller != null && controller.metadata != null) {
                    val currentPlaying = controller.metadata.description.mediaId
                    val musicId = extractMusicIDFromMediaID(item.description.mediaId!!)
                    if (currentPlaying != null && currentPlaying == musicId) {
                        val pbState = controller.playbackState
                        itemState = if (pbState == null ||
                            pbState.state == PlaybackStateCompat.STATE_ERROR
                        ) {
                            MediaItemViewHolder.STATE_NONE
                        } else if (pbState.state == PlaybackStateCompat.STATE_PLAYING) {
                            MediaItemViewHolder.STATE_PLAYING
                        } else {
                            MediaItemViewHolder.STATE_PAUSED
                        }
                    }
                }
            }
            return MediaItemViewHolder.setupView(
                activity = context as Activity,
                convertView = convertView,
                parent = parent,
                description = item.description,
                state = itemState
            )
        }
    }

    interface MediaFragmentListener : MediaBrowserProvider {
        fun onMediaItemSelected(item: MediaBrowserCompat.MediaItem)
        fun setToolbarTitle(title: CharSequence)
        fun setToolbarSubTitle(title: CharSequence)
    }

    companion object {
        private const val ARG_MEDIA_ID = "media_id"
        private const val ARG_TITLE = "title"
        private const val ARG_SUBTITLE = "subtitle"
    }
}
