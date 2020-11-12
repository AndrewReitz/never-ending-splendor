package never.ending.splendor.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import never.ending.splendor.R
import never.ending.splendor.app.MusicService
import never.ending.splendor.databinding.FragmentPlaybackControlsBinding
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.x.di
import org.kodein.di.instance
import timber.log.Timber

/**
 * A class that shows the Media Queue to the user.
 */
class PlaybackControlsFragment : Fragment(), DIAware {

    override val di: DI by di()

    private var _binding: FragmentPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    val picasso: Picasso by instance()

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private val callback: MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                Timber.d("Received playback state change to state %s", state.state)
                this@PlaybackControlsFragment.onPlaybackStateChanged(state)
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                Timber.d(
                    "Received metadata state change to mediaId=%s song=%s",
                    metadata.description.mediaId,
                    metadata.description.title
                )
                this@PlaybackControlsFragment.onMetadataChanged(metadata)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("onCreateView")
        _binding = FragmentPlaybackControlsBinding.inflate(inflater, container, false)

        val rootView = binding.root
        binding.run {
            playPause.isEnabled = true
            playPause.setOnClickListener(buttonListener)

            rootView.setOnClickListener {
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
        }
        return rootView
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart")
        val controller = (activity as BaseActivity?)!!.supportMediaController
        if (controller != null) {
            onConnected()
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop")
        val controller = (activity as BaseActivity?)?.supportMediaController
        controller?.unregisterCallback(callback)
    }

    fun onConnected() {
        val controller = (activity as BaseActivity?)?.supportMediaController
        Timber.d("onConnected, mediaController==null? %s", controller == null)
        if (controller != null) {
            onMetadataChanged(controller.metadata)
            onPlaybackStateChanged(controller.playbackState)
            controller.registerCallback(callback)
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
        binding.title.text = metadata.description.title
        binding.artist.text = metadata.description.subtitle
        var artUrl: String? = null
        if (metadata.description.iconUri != null) {
            artUrl = metadata.description.iconUri.toString()
        }

        picasso.load(artUrl)
            .fit()
            .centerInside()
            .into(binding.albumArt)
    }

    fun setExtraInfo(extraInfo: String?) {
        if (extraInfo == null) {
            binding.extraInfo.visibility = View.GONE
        } else {
            binding.extraInfo.text = extraInfo
            binding.extraInfo.visibility = View.VISIBLE
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
            binding.playPause.setImageDrawable(
                ContextCompat.getDrawable(activity!!, R.drawable.ic_play_arrow_black_36dp)
            )
        } else {
            binding.playPause.setImageDrawable(
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

    private val buttonListener = View.OnClickListener { v ->
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
