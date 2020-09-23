package never.ending.splendor.app.ui

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.AnimationDrawable
import android.support.v4.media.MediaDescriptionCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import never.ending.splendor.R

class MediaItemViewHolder {
    var mImageView: ImageView? = null
    var mTitleView: TextView? = null
    var mDescriptionView: TextView? = null

    companion object {
        const val STATE_INVALID = -1
        const val STATE_NONE = 0
        const val STATE_PLAYABLE = 1
        const val STATE_PAUSED = 2
        const val STATE_PLAYING = 3
        private var sColorStatePlaying: ColorStateList? = null
        private var sColorStateNotPlaying: ColorStateList? = null

        fun setupView(
            activity: Activity,
            convertView: View?,
            parent: ViewGroup?,
            description: MediaDescriptionCompat,
            state: Int
        ): View {
            var convertView = convertView
            if (sColorStateNotPlaying == null || sColorStatePlaying == null) {
                initializeColorStateLists(activity)
            }
            val holder: MediaItemViewHolder
            var cachedState = STATE_INVALID
            if (convertView == null) {
                convertView = LayoutInflater.from(activity)
                    .inflate(R.layout.media_list_item, parent, false)
                holder = MediaItemViewHolder()
                holder.mImageView = convertView.findViewById(R.id.play_eq)
                holder.mTitleView = convertView.findViewById(R.id.title)
                holder.mDescriptionView = convertView.findViewById(R.id.description)
                convertView.tag = holder
            } else {
                holder = convertView.tag as MediaItemViewHolder
                cachedState = convertView.getTag(R.id.tag_mediaitem_state_cache) as Int
            }
            holder.mTitleView!!.text = description.title
            holder.mDescriptionView!!.text = description.subtitle

            // If the state of convertView is different, we need to adapt the view to the
            // new state.
            if (cachedState == null || cachedState != state) {
                when (state) {
                    STATE_PLAYABLE -> {
                        val pauseDrawable = ContextCompat.getDrawable(
                            activity,
                            R.drawable.ic_play_arrow_black_36dp
                        )
                        DrawableCompat.setTintList(pauseDrawable!!, sColorStateNotPlaying)
                        holder.mImageView!!.setImageDrawable(pauseDrawable)
                        holder.mImageView!!.visibility = View.VISIBLE
                    }
                    STATE_PLAYING -> {
                        val animation = ContextCompat.getDrawable(
                            activity,
                            R.drawable.ic_equalizer_white_36dp
                        ) as AnimationDrawable?
                        DrawableCompat.setTintList(animation!!, sColorStatePlaying)
                        holder.mImageView!!.setImageDrawable(animation)
                        holder.mImageView!!.visibility = View.VISIBLE
                        animation.start()
                    }
                    STATE_PAUSED -> {
                        val playDrawable = ContextCompat.getDrawable(
                            activity,
                            R.drawable.ic_equalizer1_white_36dp
                        )
                        DrawableCompat.setTintList(playDrawable!!, sColorStatePlaying)
                        holder.mImageView!!.setImageDrawable(playDrawable)
                        holder.mImageView!!.visibility = View.VISIBLE
                    }
                    else -> holder.mImageView!!.visibility = View.GONE
                }
                convertView!!.setTag(R.id.tag_mediaitem_state_cache, state)
            }
            return convertView!!
        }

        private fun initializeColorStateLists(ctx: Context) {
            sColorStateNotPlaying = ColorStateList.valueOf(
                ctx.resources.getColor(
                    R.color.media_item_icon_not_playing
                )
            )
            sColorStatePlaying = ColorStateList.valueOf(
                ctx.resources.getColor(
                    R.color.media_item_icon_playing
                )
            )
        }
    }
}
