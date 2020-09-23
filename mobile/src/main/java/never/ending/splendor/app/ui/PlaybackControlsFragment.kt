package never.ending.splendor.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import never.ending.splendor.R
import never.ending.splendor.app.MusicService
import never.ending.splendor.app.inject
import timber.log.Timber
import javax.inject.Inject

/**
 * A class that shows the Media Queue to the user.
 */
class PlaybackControlsFragment : Fragment() {
    private var mPlayPause: ImageButton? = null
    private var mTitle: TextView? = null
    private var mSubtitle: TextView? = null
    private var mExtraInfo: TextView? = null
    private var mAlbumArt: ImageView? = null
    private var mArtUrl: String? = null

    @Inject
    var picasso: Picasso? = null

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private val mCallback: MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                Timber.d("Received playback state change to state %s", state.state)
                this@PlaybackControlsFragment.onPlaybackStateChanged(state)
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                if (metadata == null) {
                    return
                }
                Timber.d(
                    "Received metadata state change to mediaId=%s song=%s",
                    metadata.description.mediaId,
                    metadata.description.title
                )
                this@PlaybackControlsFragment.onMetadataChanged(metadata)
            }
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        this.inject()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false)
        mPlayPause = rootView.findViewById(R.id.play_pause)
        mPlayPause!!.setEnabled(true)
        mPlayPause!!.setOnClickListener(mButtonListener)
        mTitle = rootView.findViewById(R.id.title)
        mSubtitle = rootView.findViewById(R.id.artist)
        mExtraInfo = rootView.findViewById(R.id.extra_info)
        mAlbumArt = rootView.findViewById(R.id.album_art)
        rootView.setOnClickListener { v: View? ->
            val intent = Intent(activity, FullScreenPlayerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            val controller = (activity as BaseActivity?)?.supportMediaController
            val metadata = controller!!.metadata
            if (metadata != null) {
                intent.putExtra(
                    MusicPlayerActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION,
                    metadata.description
                )
            }
            startActivity(intent)
        }
        return rootView
    }

    override fun onStart() {
        super.onStart()
        Timber.d("fragment.onStart")
        val controller = (activity as BaseActivity?)!!.supportMediaController
        if (controller != null) {
            onConnected()
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.d("fragment.onStop")
        val controller = (activity as BaseActivity?)?.supportMediaController
        controller?.unregisterCallback(mCallback)
    }

    fun onConnected() {
        val controller = (activity as BaseActivity?)?.supportMediaController
        Timber.d("onConnected, mediaController==null? %s", controller == null)
        if (controller != null) {
            onMetadataChanged(controller.metadata)
            onPlaybackStateChanged(controller.playbackState)
            controller.registerCallback(mCallback)
        }
    }

    @SuppressLint("BinaryOperationInTimber")
    private fun onMetadataChanged(metadata: MediaMetadataCompat?) {
        Timber.d("onMetadataChanged %s", metadata)
        if (activity == null) {
            Timber.w(
                "onMetadataChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring."
            )
            return
        }
        if (metadata == null) {
            return
        }
        mTitle!!.text = metadata.description.title
        mSubtitle!!.text = metadata.description.subtitle
        var artUrl: String? = null
        if (metadata.description.iconUri != null) {
            artUrl = metadata.description.iconUri.toString()
        }
        if (!TextUtils.equals(artUrl, mArtUrl)) {
            mArtUrl = artUrl
            val art = metadata.description.iconBitmap
            picasso!!.load(mArtUrl)
                .fit()
                .centerInside()
                .into(mAlbumArt)
        }
    }

    fun setExtraInfo(extraInfo: String?) {
        if (extraInfo == null) {
            mExtraInfo!!.visibility = View.GONE
        } else {
            mExtraInfo!!.text = extraInfo
            mExtraInfo!!.visibility = View.VISIBLE
        }
    }

    @SuppressLint("BinaryOperationInTimber")
    private fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
        Timber.d("onPlaybackStateChanged %s", state)
        if (activity == null) {
            Timber.w(
                "onPlaybackStateChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring."
            )
            return
        }
        if (state == null) {
            return
        }
        var enablePlay = false
        when (state.state) {
            PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_STOPPED -> enablePlay = true
            PlaybackStateCompat.STATE_ERROR -> {
                Timber.e("error playbackstate: %s", state.errorMessage)
                Toast.makeText(activity, state.errorMessage, Toast.LENGTH_LONG).show()
            }
        }
        if (enablePlay) {
            mPlayPause!!.setImageDrawable(
                ContextCompat.getDrawable(activity!!, R.drawable.ic_play_arrow_black_36dp)
            )
        } else {
            mPlayPause!!.setImageDrawable(
                ContextCompat.getDrawable(activity!!, R.drawable.ic_pause_black_36dp)
            )
        }
        val controller = (activity as BaseActivity?)?.supportMediaController
        var extraInfo: String? = null
        if (controller != null && controller.extras != null) {
            val castName = controller.extras.getString(MusicService.EXTRA_CONNECTED_CAST)
            if (castName != null) {
                extraInfo = resources.getString(R.string.casting_to_device, castName)
            }
        }
        setExtraInfo(extraInfo)
    }

    private val mButtonListener = View.OnClickListener { v ->
        val controller = (activity as BaseActivity?)?.supportMediaController
        val stateObj = controller!!.playbackState
        val state = stateObj?.state ?: PlaybackStateCompat.STATE_NONE
        Timber.d("Button pressed, in state %s", state)
        when (v.id) {
            R.id.play_pause -> {
                Timber.d("Play button pressed, in state %s", state)
                if (state == PlaybackStateCompat.STATE_PAUSED || state == PlaybackStateCompat.STATE_STOPPED || state == PlaybackStateCompat.STATE_NONE) {
                    playMedia()
                } else if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_BUFFERING || state == PlaybackStateCompat.STATE_CONNECTING) {
                    pauseMedia()
                }
            }
        }
    }

    private fun playMedia() {
        val controller = (activity as BaseActivity?)?.supportMediaController
        controller?.transportControls?.play()
    }

    private fun pauseMedia() {
        val controller = (activity as BaseActivity?)?.supportMediaController
        controller?.transportControls?.pause()
    }
}
