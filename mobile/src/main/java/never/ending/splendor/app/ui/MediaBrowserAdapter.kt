package never.ending.splendor.app.ui

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import never.ending.splendor.app.utils.layoutInflator
import never.ending.splendor.app.utils.toImmutableList
import never.ending.splendor.databinding.MediaListItemBinding

class MediaBrowserAdapter(
    private val context: Context,
    private val mediaController: MediaControllerCompat?, // todo fix this
    private val itemClickListener: (MediaBrowserCompat.MediaItem) -> Unit
) : RecyclerView.Adapter<MediaItemViewHolder>() {

    var media: List<MediaBrowserCompat.MediaItem> = listOf()
        set(value) {
            field = value.toImmutableList()
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MediaItemViewHolder(
            context = context,
            binding = MediaListItemBinding.inflate(parent.layoutInflator, parent, false),
            mediaController = mediaController
        ).apply {
            itemView.setOnClickListener { itemClickListener(media[layoutPosition]) }
        }

    override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
        val item = media[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = media.size
}
