package never.ending.splendor.app.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import never.ending.splendor.R
import never.ending.splendor.app.utils.MediaIdHelper.musicId
import never.ending.splendor.databinding.MediaListItemBinding

class MediaItemViewHolder(
    context: Context,
    binding: MediaListItemBinding,
    private val mediaController: MediaControllerCompat?
) : RecyclerView.ViewHolder(binding.root) {

    private val view: View = binding.root
    private val title: TextView = binding.title
    private val description: TextView = binding.description
    private val playImage: ImageView = binding.playImage

    private val colorStateNotPlaying = ColorStateList.valueOf(
        ContextCompat.getColor(view.context, R.color.media_item_icon_not_playing)
    )

    private val colorStatePlaying = ColorStateList.valueOf(
        ContextCompat.getColor(view.context, R.color.media_item_icon_playing)
    )

    private val pauseDrawable: Drawable =
        ContextCompat.getDrawable(context, R.drawable.ic_play_arrow_black_36dp)
            .let { requireNotNull(it) }
            .also { DrawableCompat.setTintList(it, colorStateNotPlaying) }

    private val playingAnimation =
        ContextCompat.getDrawable(context, R.drawable.ic_equalizer_white_36dp)
            .let { it as AnimationDrawable }
            .also { DrawableCompat.setTintList(it, colorStatePlaying) }

    private val playDrawable =
        ContextCompat.getDrawable(context, R.drawable.ic_equalizer1_white_36dp)
            .let { requireNotNull(it) }
            .also { DrawableCompat.setTintList(it, colorStatePlaying) }

    fun bind(item: MediaBrowserCompat.MediaItem) {
        title.text = item.description.title
        description.text = item.description.subtitle

        val currentState = if (item.isPlayable) MediaState.PLAYABLE else MediaState.NONE
        val cachedState = view.tag as? MediaState

        if (item.isPlayable) {
            if (mediaController?.metadata != null) {
                val currentlyPlaying = mediaController.metadata.description.mediaId
                val musicId = item.musicId
                if (currentlyPlaying == musicId) {
                    val playBackState = mediaController.playbackState
                    when (playBackState.state) {
                        STATE_ERROR -> MediaState.NONE
                        STATE_PLAYING -> MediaState.PLAYING
                        else -> MediaState.PAUSED
                    }
                }
            }
        }

        // If the state of convertView is different, we need to adapt the view to the
        // new state.
        if (cachedState != currentState) {
            when (currentState) {
                MediaState.PLAYABLE -> {
                    playImage.setImageDrawable(pauseDrawable)
                    playImage.visibility = View.VISIBLE
                }
                MediaState.PLAYING -> {
                    playImage.setImageDrawable(playingAnimation)
                    playImage.visibility = View.VISIBLE
                    playingAnimation.start()
                }
                MediaState.PAUSED -> {
                    playImage.setImageDrawable(playDrawable)
                }
                else -> playImage.visibility = View.GONE
            }
            view.tag = currentState
        }
    }

    private enum class MediaState {
        PLAYABLE,
        PLAYING,
        PAUSED,
        NONE
    }
}
