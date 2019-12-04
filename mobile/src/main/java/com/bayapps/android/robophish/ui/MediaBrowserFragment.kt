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
package com.bayapps.android.robophish.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.*
import android.webkit.WebView
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.bayapps.android.robophish.R
import com.bayapps.android.robophish.utils.MediaIDHelper
import com.bayapps.android.robophish.utils.NetworkHelper
import com.bayapps.android.robophish.utils.toStandardDateTimeString
import com.google.android.material.tabs.TabLayout
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestParams
import cz.msebera.android.httpclient.Header
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.android.synthetic.main.fragment_list.error_message
import kotlinx.android.synthetic.main.fragment_list.progress_bar
import kotlinx.android.synthetic.main.fragment_list.view.*
import kotlinx.android.synthetic.main.fragment_list_show.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

private const val ARG_MEDIA_ID = "media_id"
private const val ARG_TITLE = "title"
private const val ARG_SUBTITLE = "subtitle"

/**
 * A Fragment that lists all the various browsable queues available
 * from a [android.service.media.MediaBrowserService].
 *
 *
 * It uses a [MediaBrowserCompat] to connect to the [com.bayapps.android.robophish.MusicService].
 * Once connected, the fragment subscribes to get all the children.
 * All [MediaBrowserCompat.MediaItem]'s that can be browsed are shown in a ListView.
 */
class MediaBrowserFragment : Fragment(), KodeinAware, CoroutineScope by MainScope() {

    override val kodein: Kodein by kodein()

    private val mediaController: MediaControllerCompat by instance()

    // todo use kodein
    private val browserAdapter: BrowseAdapter by lazy {
        BrowseAdapter(requireActivity(), mediaController)
    }

    private val mediaFragmentListener: MediaFragmentListener by lazy {
        requireActivity() as MediaFragmentListener
    }

    private var mShowData: JSONObject? = null

    private val connectivityChangeReceiver = object : BroadcastReceiver() {

        private var oldOnline = false

        override fun onReceive(context: Context, intent: Intent) {
            // We don't care about network changes while this fragment is not associated
            // with a media ID (for example, while it is being initialized)
            if (mediaId == null) return

            val isOnline = NetworkHelper.isOnline(context)
            if (isOnline != oldOnline) {
                oldOnline = isOnline
                checkForUserVisibleErrors(false)
                if (isOnline) {
                    browserAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private val mediaControllerCallback: MediaControllerCompat.Callback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            Timber.d("Received metadata change to media %s", metadata.description.mediaId)
            browserAdapter.notifyDataSetChanged()
            //hide progress bar when we receive metadata
            progress_bar.isInvisible = true
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            Timber.d("Received state change: %s", state)
            checkForUserVisibleErrors(false)
            browserAdapter.notifyDataSetChanged()
        }
    }

    private val subscriptionCallback: MediaBrowserCompat.SubscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) {
            try {
                Timber.d("fragment onChildrenLoaded, parentId=%s, count=%s", parentId, children.size)
                checkForUserVisibleErrors(children.isEmpty())
                progress_bar.isVisible = true
                browserAdapter.clear()
                browserAdapter.addAll(children)
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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        Timber.d("onCreateView()")

        val mediaId = mediaId
        val rootView = if (mediaId != null && MediaIDHelper.isShow(mediaId)) {
            inflater.inflate(R.layout.fragment_list_show, container, false)
        } else inflater.inflate(R.layout.fragment_list, container, false)

        if (mediaId != null && MediaIDHelper.isShow(mediaId)) {
            setHasOptionsMenu(true) //show option to download

            viewpager.adapter = ShowPagerAdapter(inflater, rootView)
            viewpager.offscreenPageLimit = 3

            sliding_tabs.setupWithViewPager(viewpager)

            setlist_webview.settings.javaScriptEnabled = true
            val setlistClient = AsyncHttpClient()
            val setlistParams = RequestParams()

            setlistParams.put("api", "2.0")
            setlistParams.put("method", "pnet.shows.setlists.get")
            setlistParams.put("showdate", subTitle)
            setlistParams.put("apikey", "C01AEE2002E80723E9E7")
            setlistParams.put("format", "json")
            setlistClient["http://api.phish.net/api.js", setlistParams, object : JsonHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONArray) {
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
                        setlist_webview.loadData(header + setlistdata + setlistnotes, "text/html", null)
                    } catch (e: JSONException) {
                        Timber.e(e, "Error loading setlist")
                    }
                }
            }]


            reviews_webview.settings.javaScriptEnabled = true
            val reviewsClient = AsyncHttpClient()
            val reviewsParams = RequestParams()
            reviewsParams.put("api", "2.0")
            reviewsParams.put("method", "pnet.reviews.query")
            reviewsParams.put("showdate", subTitle)
            reviewsParams.put("apikey", "C01AEE2002E80723E9E7")
            reviewsParams.put("format", "json")
            reviewsClient["http://api.phish.net/api.js", reviewsParams, object : JsonHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONArray) {
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
                            val reviewDate = reviewTime.toStandardDateTimeString()
                            val reviewSubs = review.replace("\n".toRegex(), "<br/>")
                            display.append("<h2>").append(author).append("</h2>").append("<h4>").append(reviewDate).append("</h4>")
                            display.append(reviewSubs).append("<br/>")
                        }
                        reviews_webview.loadData(display.toString(), "text/html", null)
                    } catch (e: JSONException) {
                        Timber.e(e, "Error loading reviews")
                    }
                }
            }]


            tapernotes_webview.settings.javaScriptEnabled = true
            val showId = MediaIDHelper.extractShowFromMediaID(mediaId)
            val tapernotesClient = AsyncHttpClient()
            tapernotesClient["http://phish.in/api/v1/shows/$showId.json", null, object : JsonHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                    super.onSuccess(statusCode, headers, response)
                    try {
                        mShowData = response
                        val data = response.getJSONObject("data")
                        var tapernotes = data.getString("taper_notes")
                        if (tapernotes == "null") tapernotes = "Not available"
                        val notesSubs = tapernotes.replace("\n".toRegex(), "<br/>")
                        tapernotes_webview.loadData(notesSubs, "text/html", null)
                    } catch (e: JSONException) {
                        Timber.e(e, "error loading taper notes")
                    }
                }
            }]
        }

        rootView.progress_bar.isVisible = true

        list_view.adapter = browserAdapter
        list_view.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            checkForUserVisibleErrors(false)
            val item = browserAdapter.getItem(position)
            mediaFragmentListener.onMediaItemSelected(item)
        }

        return rootView
    }

    override fun onStart() {
        super.onStart()
        // fetch browsing information to fill the listview:
        val mediaBrowser = mediaFragmentListener.mediaBrowser
        Timber.d("fragment.onStart, mediaId=%s onConnected=%s", mediaId,
                mediaBrowser.isConnected)
        if (mediaBrowser.isConnected) {
            onConnected()
        }
        // Registers BroadcastReceiver to track network connection changes.
        requireActivity().registerReceiver(connectivityChangeReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onStop() {
        super.onStop()
        val mediaBrowser = mediaFragmentListener.mediaBrowser
        if (mediaBrowser != null && mediaBrowser.isConnected && mediaId != null) {
            mediaBrowser.unsubscribe(mediaId!!)
        }

        mediaController.unregisterCallback(mediaControllerCallback)
        requireActivity().unregisterReceiver(connectivityChangeReceiver)
    }

    override fun onDetach() {
        super.onDetach()
        cancel()
    }

    var mediaId: String? = arguments?.getString(ARG_MEDIA_ID)

    val title: String? = arguments?.getString(ARG_TITLE)

    private val subTitle: String? = arguments?.getString(ARG_SUBTITLE)

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

        if (mediaId == null) {
            mediaId = mediaFragmentListener.mediaBrowser.root
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
        mediaFragmentListener.mediaBrowser.unsubscribe(mediaId!!)
        mediaFragmentListener.mediaBrowser.subscribe(mediaId!!, subscriptionCallback)
        // Add MediaController callback so we can redraw the list when metadata changes:
        mediaController.registerCallback(mediaControllerCallback)
    }

    private fun checkForUserVisibleErrors(forceError: Boolean) {
        var showError = forceError
        // If offline, message is about the lack of connectivity:
        if (!NetworkHelper.isOnline(activity)) {
            error_message.setText(R.string.error_no_connection)
            showError = true
        } else {
            // otherwise, if state is ERROR and metadata!=null, use playback state error message:
            if (mediaController.metadata != null &&
                    mediaController.playbackState != null &&
                    mediaController.playbackState.state == PlaybackStateCompat.STATE_ERROR &&
                    mediaController.playbackState.errorMessage != null) {
                error_message.text = mediaController.playbackState.errorMessage
                showError = true
            } else if (forceError) {
                // Finally, if the caller requested to show error, show a generic message:
                error_message.setText(R.string.error_loading_media)
                showError = true
            }
        }
        error_message.visibility = if (showError) View.VISIBLE else View.GONE
        if (showError) progress_bar.isInvisible = true
        Timber.d("checkForUserVisibleErrors. forceError=%s  showError=%s  isOnline=%s", forceError, showError, NetworkHelper.isOnline(activity))
    }

    private fun updateTitle() {
        mediaFragmentListener.updateDrawerToggle()
        if (mediaId!!.startsWith(MediaIDHelper.MEDIA_ID_SHOWS_BY_YEAR)) {
            val year = MediaIDHelper.getHierarchy(mediaId!!)[1]
            mediaFragmentListener.setToolbarTitle(year)
            mediaFragmentListener.setToolbarSubTitle("")
            return
        }
        if (mediaId!!.startsWith(MediaIDHelper.MEDIA_ID_TRACKS_BY_SHOW)) {
            mediaFragmentListener.setToolbarTitle(title)
            mediaFragmentListener.setToolbarSubTitle(subTitle)
            return
        }
        if (MediaIDHelper.MEDIA_ID_ROOT == mediaId) {
            mediaFragmentListener.setToolbarTitle(null)
            return
        }
        val mediaBrowser = mediaFragmentListener.mediaBrowser
        mediaBrowser.getItem(mediaId!!, object : MediaBrowserCompat.ItemCallback() {
            override fun onItemLoaded(item: MediaBrowserCompat.MediaItem) {
                mediaFragmentListener.setToolbarTitle(
                        item.description.title)
            }
        })
    }
}

interface MediaFragmentListener : MediaBrowserProvider {
    fun onMediaItemSelected(item: MediaBrowserCompat.MediaItem?)
    fun setToolbarTitle(title: CharSequence?)
    fun setToolbarSubTitle(title: CharSequence?)
    fun updateDrawerToggle()
}

class BrowseAdapter(
        activity: Activity,
        private val mediaController: MediaControllerCompat
) : ArrayAdapter<MediaBrowserCompat.MediaItem>(activity, R.layout.media_list_item, mutableListOf()) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)

        var itemState = MediaItemViewHolder.STATE_NONE

        if (item!!.isPlayable) {
            itemState = MediaItemViewHolder.STATE_PLAYABLE

            if (mediaController.metadata != null) {
                val currentPlaying = mediaController.metadata.description.mediaId
                val musicId = MediaIDHelper.extractMusicIDFromMediaID(item.description.mediaId!!)
                if (currentPlaying != null && currentPlaying == musicId) {
                    val pbState = mediaController.playbackState
                    itemState = if (pbState == null ||
                            pbState.state == PlaybackStateCompat.STATE_ERROR) {
                        MediaItemViewHolder.STATE_NONE
                    } else if (pbState.state == PlaybackStateCompat.STATE_PLAYING) {
                        MediaItemViewHolder.STATE_PLAYING
                    } else {
                        MediaItemViewHolder.STATE_PAUSED
                    }
                }
            }
        }

        return MediaItemViewHolder.setupView(context as Activity, convertView, parent, item.description, itemState)
    }
}
